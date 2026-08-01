package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CalendarioVencimientosResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertificacionVencimientoDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calendario de vencimientos del dashboard (PP-77): certificaciones de la
 * empresa autenticada agrupadas por día de vencimiento, para el mes
 * solicitado.
 */
@Service
public class CalendarioVencimientosService {

    private static final String URGENCIA_VENCIDA = "vencida";

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;

    public CalendarioVencimientosService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            CatalogoTiposCertificacion catalogoTiposCertificacion) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
    }

    @Transactional(readOnly = true)
    public CalendarioVencimientosResponseDTO obtenerCalendario(UUID usuarioId, String mesSolicitado) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        YearMonth mes = parsearMes(mesSolicitado);

        List<Certificacion> certificaciones = certificacionRepository
                .findByEmpresaIdAndFechaVencimientoBetween(empresaId, mes.atDay(1), mes.atEndOfMonth());

        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);

        Map<String, List<CertificacionVencimientoDTO>> vencimientosPorFecha = certificaciones.stream()
                .collect(Collectors.groupingBy(
                        certificacion -> certificacion.getFechaVencimiento().toString(),
                        TreeMap::new,
                        Collectors.mapping(certificacion -> aDto(certificacion, hoy), Collectors.toList())));

        return new CalendarioVencimientosResponseDTO(mes.toString(), vencimientosPorFecha);
    }

    private CertificacionVencimientoDTO aDto(Certificacion certificacion, LocalDate hoy) {
        long diasRestantes = ChronoUnit.DAYS.between(hoy, certificacion.getFechaVencimiento());

        return new CertificacionVencimientoDTO(
                certificacion.getId(),
                nombreLegible(certificacion),
                urgenciaPara(diasRestantes));
    }

    /**
     * El nombre legible vive en {@link CatalogoTiposCertificacion} — es el
     * mismo que ya usan la credencial OpenBadges y los correos de vencimiento
     * de PP-71. No se duplica acá para no arriesgar que las dos copias
     * diverjan (ya paso una vez en este mismo cambio).
     */
    private String nombreLegible(Certificacion certificacion) {
        return catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .map(DefinicionCertificacion::nombre)
                .orElseGet(certificacion.getTipo()::getCodigo);
    }

    /**
     * "vencida" es un valor propio del calendario, no de {@link TipoAlerta}:
     * una certificacion con {@code diasRestantes <= 0} ya paso su fecha de
     * vencimiento, lo cual es un estado distinto de "esta por vencer en los
     * proximos 7/30/90 dias". Separarlo evita que el front tenga que
     * re-derivar "ya vencio" comparando la fecha (llave del mapa) contra hoy.
     */
    private String urgenciaPara(long diasRestantes) {
        if (diasRestantes <= 0) {
            return URGENCIA_VENCIDA;
        }

        return Arrays.stream(TipoAlerta.values())
                .filter(tipo -> diasRestantes <= tipo.getDias())
                .min(Comparator.comparingInt(TipoAlerta::getDias))
                .map(TipoAlerta::getCodigo)
                .orElse(TipoAlerta.DIAS_90.getCodigo());
    }

    private YearMonth parsearMes(String mesSolicitado) {
        if (mesSolicitado != null) {
            try {
                return YearMonth.parse(mesSolicitado);
            } catch (DateTimeParseException e) {
                // Formato invalido: cae al mes actual, tal como pide el criterio de aceptacion.
            }
        }
        return YearMonth.now(ZonasHorarias.COSTA_RICA);
    }
}
