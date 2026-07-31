package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resumen de certificaciones para el bloque "Estado de certificaciones" del
 * dashboard (PP-74). Los tres conteos son mutuamente excluyentes:
 *
 * <ul>
 *   <li>{@code vencidas}: {@code fechaVencimiento} no posterior a hoy,
 *       sin importar si tiene alertas generadas.</li>
 *   <li>{@code proximasAVencer}: vigentes (fechaVencimiento futura) que ya
 *       cruzaron algun umbral de alerta (90/30/7 dias) — es decir, tienen al
 *       menos una fila en {@code alertas}.</li>
 *   <li>{@code activas}: vigentes sin ninguna alerta generada todavia.</li>
 * </ul>
 *
 * <p>Nota: {@code EstadoCertificacion} solo tiene el valor {@code ACTIVA} hoy
 * (no existe un estado "vencida" persistido), asi que "vencida" se deriva de
 * la fecha, igual que el concepto de "vigente" ya usado en
 * {@code CertificacionRepository} para el perfil publico.</p>
 */
@Service
public class DashboardCertificacionesService {

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final AlertaRepository alertaRepository;

    public DashboardCertificacionesService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            AlertaRepository alertaRepository) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.alertaRepository = alertaRepository;
    }

    @Transactional(readOnly = true)
    public ResumenCertificacionesDashboardResponseDTO obtenerResumen(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);

        List<Certificacion> certificaciones = certificacionRepository
                .findByEmpresaIdOrderByFechaEmisionDesc(empresaId);
        Set<UUID> idsConAlerta = alertaRepository.findByEmpresaId(empresaId).stream()
                .map(alerta -> alerta.getCertificacion().getId())
                .collect(Collectors.toSet());

        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);
        int activas = 0;
        int proximasAVencer = 0;
        int vencidas = 0;

        for (Certificacion certificacion : certificaciones) {
            if (!certificacion.getFechaVencimiento().isAfter(hoy)) {
                vencidas++;
            } else if (idsConAlerta.contains(certificacion.getId())) {
                proximasAVencer++;
            } else {
                activas++;
            }
        }

        return new ResumenCertificacionesDashboardResponseDTO(activas, proximasAVencer, vencidas);
    }
}
