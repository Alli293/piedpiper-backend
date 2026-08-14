package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parte de base de datos de la recomendación de renovación (PP-72): reúne
 * las certificaciones con alerta activa de la empresa y selecciona cuál
 * debe renovarse primero.
 *
 * <p>Vive separada de {@link DashboardRecomendacionService} a propósito:
 * esa clase también llama a Gemini (un efecto externo), y
 * {@code docs/CONVENTIONS.md} §4.5 pide que esos efectos no corran dentro
 * de una transacción — una conexión del pool quedaría tomada durante todo
 * lo que tarde el LLM. Que sea un bean de Spring aparte (no solo un método
 * aparte en la misma clase) importa: por el proxy de {@code @Transactional},
 * un método transaccional llamado como {@code this.metodo(...)} desde
 * dentro de la propia clase NO abre transacción — y acá hace falta una
 * activa de principio a fin, porque {@link #impactoHuellaT} accede a
 * {@code certificacion.getEmpresa()}, una relación {@code LAZY}.</p>
 */
@Service
public class RecomendacionRenovacionConsultaService {

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final EmisionRepository emisionRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final VencimientoPresentacionService vencimientoPresentacionService;
    private final RecomendacionRenovacionSeleccionService seleccionService;

    public RecomendacionRenovacionConsultaService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            EmisionRepository emisionRepository,
            CatalogoTiposCertificacion catalogoTiposCertificacion,
            VencimientoPresentacionService vencimientoPresentacionService,
            RecomendacionRenovacionSeleccionService seleccionService) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.emisionRepository = emisionRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.vencimientoPresentacionService = vencimientoPresentacionService;
        this.seleccionService = seleccionService;
    }

    @Transactional(readOnly = true)
    public Optional<CertAlertaDTO> obtenerCertificacionPrioritaria(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);

        List<CertAlertaDTO> alertas = certificacionRepository
                .findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(empresaId, EstadoCertificacion.ACTIVA)
                .stream()
                .map(certificacion -> aCertAlertaDTO(certificacion, hoy))
                .filter(alerta -> vencimientoPresentacionService.dentroDelUmbralMaximo(alerta.getDiasRestantes()))
                .collect(Collectors.toList());

        // seleccionarPrioritaria() solo devuelve Optional.empty() si "alertas" esta
        // vacia, y ese caso ya lo representa un Optional.empty() legitimo aca -- no
        // hace falta distinguirlo con una excepcion.
        return seleccionService.seleccionarPrioritaria(alertas);
    }

    private CertAlertaDTO aCertAlertaDTO(Certificacion certificacion, LocalDate hoy) {
        long diasRestantes = ChronoUnit.DAYS.between(hoy, certificacion.getFechaVencimiento());
        return new CertAlertaDTO(
                certificacion.getId(),
                vencimientoPresentacionService.nombreLegible(certificacion),
                certificacion.getFechaVencimiento(),
                (int) diasRestantes,
                impactoHuellaT(certificacion));
    }

    private BigDecimal impactoHuellaT(Certificacion certificacion) {
        LocalDate fin = certificacion.getFechaEmision().atZone(ZonasHorarias.COSTA_RICA).toLocalDate();
        // Si el tipo no esta en el catalogo, vigenciaMeses cae a 0: inicio queda
        // igual a fin, la consulta de emisiones colapsa a un solo dia, y
        // impactoHuellaT sale practicamente 0 (nunca null/negativo). Ese impacto
        // artificialmente bajo despues se le pasa tal cual a la IA para redactar
        // la justificacion. Mismo patron de fallback silencioso que
        // VencimientoPresentacionService.nombreLegible ante un tipo desconocido;
        // en la practica no deberia pasar porque el catalogo cubre todos los
        // TipoCertificacion existentes, pero si el catalogo alguna vez queda
        // incompleto, esta es la consecuencia concreta a tener en cuenta.
        int vigenciaMeses = catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .map(DefinicionCertificacion::vigenciaMeses)
                .orElse(0);
        LocalDate inicio = fin.minusMonths(vigenciaMeses);

        BigDecimal carbonKg = emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                certificacion.getEmpresa().getId(), inicio, fin);
        return HuellasCarbono.toneladasDesdeKg(Optional.ofNullable(carbonKg).orElse(BigDecimal.ZERO));
    }
}
