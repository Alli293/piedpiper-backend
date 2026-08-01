package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Resumen de certificaciones para el bloque "Estado de certificaciones" del
 * dashboard (PP-74). Los tres conteos son mutuamente excluyentes y se derivan
 * exclusivamente de {@code fechaVencimiento} contra la fecha de hoy — no de
 * si el proceso nocturno de alertas (PP-70) ya generó una fila en
 * {@code alertas} para esa certificacion:
 *
 * <ul>
 *   <li>{@code vencidas}: {@code fechaVencimiento} no posterior a hoy.</li>
 *   <li>{@code proximasAVencer}: vigentes, a 90 dias o menos de vencer.</li>
 *   <li>{@code activas}: vigentes, a mas de 90 dias de vencer.</li>
 * </ul>
 *
 * <p><b>Por que no se usa {@code alertas}:</b> ese umbral (90/30/7 dias) solo
 * se materializa como fila cuando corre el batch de las 2 AM. Clasificar por
 * esa tabla ataria este resumen de lectura al horario del batch: si una
 * certificacion cruza el umbral a las 9 AM, quedaria mostrada como "activa"
 * hasta la corrida siguiente, y si el batch no corrio (ambiente nuevo,
 * caida, cron roto), el resumen mostraria "activa" para certificaciones que
 * en realidad estan a punto de vencer — exactamente el caso en el que este
 * bloque existe para avisar. Calcular contra {@code fechaVencimiento}
 * directamente da el mismo resultado que hoy cuando el batch esta al dia, y
 * el correcto cuando no lo esta.</p>
 *
 * <p>Nota aparte: {@code EstadoCertificacion} solo tiene el valor
 * {@code ACTIVA} hoy (no existe un estado "vencida" persistido), asi que
 * "vencida" se deriva de la fecha, igual que el concepto de "vigente" ya
 * usado en {@code CertificacionRepository} para el perfil publico.</p>
 */
@Service
public class DashboardCertificacionesService {

    private static final int UMBRAL_PROXIMA_A_VENCER_DIAS = 90;

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;

    public DashboardCertificacionesService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
    }

    @Transactional(readOnly = true)
    public ResumenCertificacionesDashboardResponseDTO obtenerResumen(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);

        List<Certificacion> certificaciones = certificacionRepository
                .findByEmpresaIdOrderByFechaEmisionDesc(empresaId);

        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);
        int activas = 0;
        int proximasAVencer = 0;
        int vencidas = 0;

        for (Certificacion certificacion : certificaciones) {
            long diasRestantes = ChronoUnit.DAYS.between(hoy, certificacion.getFechaVencimiento());
            if (diasRestantes <= 0) {
                vencidas++;
            } else if (diasRestantes <= UMBRAL_PROXIMA_A_VENCER_DIAS) {
                proximasAVencer++;
            } else {
                activas++;
            }
        }

        return new ResumenCertificacionesDashboardResponseDTO(activas, proximasAVencer, vencidas);
    }
}
