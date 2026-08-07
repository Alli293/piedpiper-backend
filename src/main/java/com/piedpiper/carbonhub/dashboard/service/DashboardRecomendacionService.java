package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionIaTexto;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionRenovacionResponseDTO;
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
 * "Recomendación de renovación" del dashboard de Certificaciones (PP-72):
 * identifica cuál certificación con alerta activa debe renovarse primero
 * y le pide a la IA que redacte la justificación.
 *
 * <p>No lee {@code DashboardAlertasService} ni la tabla {@code alertas}:
 * necesita datos de la {@link Certificacion} ({@code fechaEmision, tipo})
 * que {@code AlertaVencimientoDTO} no expone y que hacen falta para
 * calcular {@code impactoHuellaT}, así que recorre las certificaciones
 * activas de la empresa directamente. El umbral de 90 días que define
 * "alerta activa" es el mismo que usa {@code DashboardAlertasService} —
 * ver esa clase para el porqué de no depender tampoco acá de la tabla
 * {@code alertas} (PP-70/PP-71 son sobre el envío de correo, no sobre si
 * la certificación sigue vigente).</p>
 *
 * <p>{@code impactoHuellaT}: el ticket lo define como la suma de
 * emisiones del "período verificado" de la certificación, pero ese
 * período no existe como campo persistido (todavía no hay entidad
 * Auditoria — ver el comentario en el campo {@code idAuditoria} de
 * {@link Certificacion}). Se aproxima con la ventana de
 * {@code vigenciaMeses} del tipo de certificación, terminando en su fecha
 * de emisión: el mismo dato que ya usa
 * {@code EmisionCertificacionService.resolverVencimiento} para calcular
 * hacia adelante la fecha de vencimiento, aquí usado hacia atrás para
 * aproximar el período que la auditoría verificó.</p>
 */
@Service
public class DashboardRecomendacionService {

    private static final int UMBRAL_MAXIMO_DIAS = 90;

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final EmisionRepository emisionRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final VencimientoPresentacionService vencimientoPresentacionService;
    private final RecomendacionRenovacionSeleccionService seleccionService;
    private final RecomendacionRenovacionIaService iaService;

    public DashboardRecomendacionService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            EmisionRepository emisionRepository,
            CatalogoTiposCertificacion catalogoTiposCertificacion,
            VencimientoPresentacionService vencimientoPresentacionService,
            RecomendacionRenovacionSeleccionService seleccionService,
            RecomendacionRenovacionIaService iaService) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.emisionRepository = emisionRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.vencimientoPresentacionService = vencimientoPresentacionService;
        this.seleccionService = seleccionService;
        this.iaService = iaService;
    }

    @Transactional(readOnly = true)
    public Optional<RecomendacionRenovacionResponseDTO> obtenerRecomendacion(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);

        List<CertAlertaDTO> alertas = certificacionRepository
                .findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(empresaId, EstadoCertificacion.ACTIVA)
                .stream()
                .map(certificacion -> aCertAlertaDTO(certificacion, hoy))
                .filter(alerta -> alerta.getDiasRestantes() <= UMBRAL_MAXIMO_DIAS)
                .collect(Collectors.toList());

        if (alertas.isEmpty()) {
            return Optional.empty();
        }

        CertAlertaDTO prioritaria = seleccionService.seleccionarPrioritaria(alertas)
                .orElseThrow(() -> new IllegalStateException("alertas no está vacía"));

        Optional<RecomendacionIaTexto> texto = iaService.generar(prioritaria);

        return Optional.of(new RecomendacionRenovacionResponseDTO(
                prioritaria.getIdCertificacion(),
                prioritaria.getNombreCertificacion(),
                prioritaria.getFechaVencimiento(),
                prioritaria.getDiasRestantes(),
                prioritaria.getImpactoHuellaT(),
                texto.map(RecomendacionIaTexto::justificacion).orElse(null),
                texto.map(RecomendacionIaTexto::sugerenciaAccion).orElse(null)));
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
        int vigenciaMeses = catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .map(DefinicionCertificacion::vigenciaMeses)
                .orElse(0);
        LocalDate inicio = fin.minusMonths(vigenciaMeses);

        BigDecimal carbonKg = emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                certificacion.getEmpresa().getId(), inicio, fin);
        return HuellasCarbono.toneladasDesdeKg(Optional.ofNullable(carbonKg).orElse(BigDecimal.ZERO));
    }
}
