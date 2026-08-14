package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
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

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final VencimientoPresentacionService vencimientoPresentacionService;

    public CalendarioVencimientosService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            VencimientoPresentacionService vencimientoPresentacionService) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.vencimientoPresentacionService = vencimientoPresentacionService;
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
                vencimientoPresentacionService.nombreLegible(certificacion),
                vencimientoPresentacionService.urgenciaPara(diasRestantes));
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
