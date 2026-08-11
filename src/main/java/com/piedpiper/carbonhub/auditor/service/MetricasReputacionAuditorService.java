package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.DistribucionSectorAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MetricasReputacionAuditorService {

    private static final Logger log =
            LoggerFactory.getLogger(MetricasReputacionAuditorService.class);
    private static final EnumSet<EstadoSolicitudAuditoria> ESTADOS_COMPLETADOS =
            EnumSet.of(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final Clock clock;

    public MetricasReputacionAuditorService(PerfilAuditorRepository perfilAuditorRepository,
                                            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                            Clock clock) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.clock = clock;
    }

    @Transactional
    public void recalcular(UUID auditorId) {
        PerfilAuditor perfil = perfilAuditorRepository.findByAuditorIdConDistribucion(auditorId)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe perfil de auditor para recalcular metricas."));
        List<SolicitudAuditoria> completadas =
                solicitudAuditoriaRepository.listarCompletadasPorAuditor(auditorId, ESTADOS_COMPLETADOS);

        if (completadas.isEmpty()) {
            dejarSinDatos(perfil);
            return;
        }

        perfil.setAuditoriasCompletadas(completadas.size());
        perfil.setTiempoPromedioRespuestaDias(calcularTiempoPromedioRespuesta(completadas));
        reemplazarDistribucion(perfil, calcularDistribucionSectores(completadas));
        perfil.setActualizadoEn(Instant.now(clock));
    }

    private void dejarSinDatos(PerfilAuditor perfil) {
        perfil.setAuditoriasCompletadas(null);
        perfil.setTiempoPromedioRespuestaDias(null);
        perfil.getDistribucionSectores().clear();
        perfil.setActualizadoEn(Instant.now(clock));
    }

    private BigDecimal calcularTiempoPromedioRespuesta(List<SolicitudAuditoria> completadas) {
        List<SolicitudAuditoria> conFechas = completadas.stream()
                .filter(this::tieneFechasRespuesta)
                .toList();

        if (conFechas.isEmpty()) {
            return null;
        }

        BigDecimal totalDias = conFechas.stream()
                .map(this::diasRespuesta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalDias.divide(BigDecimal.valueOf(conFechas.size()), 1, RoundingMode.HALF_UP);
    }

    private boolean tieneFechasRespuesta(SolicitudAuditoria solicitud) {
        boolean completa = true;
        if (solicitud.getFechaAsignacion() == null) {
            log.warn("Auditoria {} excluida de tiempoPromedioRespuestaDias: falta fechaAsignacion.",
                    solicitud.getId());
            completa = false;
        }
        if (solicitud.getFechaAceptacion() == null) {
            log.warn("Auditoria {} excluida de tiempoPromedioRespuestaDias: falta fechaPrimeraRespuesta.",
                    solicitud.getId());
            completa = false;
        }
        if (completa && solicitud.getFechaAceptacion().isBefore(solicitud.getFechaAsignacion())) {
            log.warn("Auditoria {} excluida de tiempoPromedioRespuestaDias: fechaPrimeraRespuesta "
                    + "anterior a fechaAsignacion.", solicitud.getId());
            return false;
        }
        return completa;
    }

    private BigDecimal diasRespuesta(SolicitudAuditoria solicitud) {
        long segundos = Duration.between(
                solicitud.getFechaAsignacion(),
                solicitud.getFechaAceptacion()).toSeconds();
        return BigDecimal.valueOf(segundos)
                .divide(BigDecimal.valueOf(86_400), 10, RoundingMode.HALF_UP);
    }

    private List<DistribucionSectorAuditor> calcularDistribucionSectores(
            List<SolicitudAuditoria> completadas) {
        int totalCompletadas = completadas.size();
        Map<String, Long> conteos = completadas.stream()
                .map(this::sectorDe)
                .filter(sector -> sector != null && !sector.isBlank())
                .collect(Collectors.groupingBy(sector -> sector, Collectors.counting()));

        return conteos.entrySet().stream()
                .map(entry -> aDistribucion(entry.getKey(), entry.getValue(), totalCompletadas))
                .sorted((a, b) -> b.getPorcentaje().compareTo(a.getPorcentaje()))
                .toList();
    }

    private String sectorDe(SolicitudAuditoria solicitud) {
        Empresa empresa = solicitud.getEmpresa();
        if (empresa == null || empresa.getSectorIndustrial() == null) {
            log.warn("Auditoria {} excluida de distribucionSectores: falta sectorEmpresa.",
                    solicitud.getId());
            return null;
        }
        return empresa.getSectorIndustrial().name();
    }

    private DistribucionSectorAuditor aDistribucion(
            String sector,
            long cantidad,
            int totalCompletadas) {
        BigDecimal porcentaje = BigDecimal.valueOf(cantidad * 100)
                .divide(BigDecimal.valueOf(totalCompletadas), 1, RoundingMode.HALF_UP);
        return new DistribucionSectorAuditor(sector, Math.toIntExact(cantidad), porcentaje);
    }

    private void reemplazarDistribucion(
            PerfilAuditor perfil,
            List<DistribucionSectorAuditor> distribucion) {
        perfil.getDistribucionSectores().clear();
        perfil.getDistribucionSectores().addAll(distribucion);
    }
}
