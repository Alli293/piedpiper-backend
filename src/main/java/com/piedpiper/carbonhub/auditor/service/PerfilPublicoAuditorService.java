package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PerfilPublicoAuditorService {

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final CertificacionRepository certificacionRepository;
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final PerfilPublicoAuditorMapper mapper;
    private final Clock clock;

    public PerfilPublicoAuditorService(PerfilAuditorRepository perfilAuditorRepository,
                                       CertificacionRepository certificacionRepository,
                                       SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                       CatalogoTiposCertificacion catalogoTiposCertificacion,
                                       PerfilPublicoAuditorMapper mapper,
                                       Clock clock) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.certificacionRepository = certificacionRepository;
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoAuditorResponseDTO obtenerPerfilPublico(UUID auditorId) {
        // 1. Buscar perfil con estado ACTIVO (mismo 404 para inexistente y no activo)
        PerfilAuditor perfil = perfilAuditorRepository
                .findByAuditorIdAndAuditorEstado(auditorId, EstadoUsuario.ACTIVO)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "El perfil solicitado no está disponible."));

        // 2. Obtener certificaciones del auditor
        List<Certificacion> certificaciones = certificacionRepository.findByAuditorId(auditorId);

        // 3. Obtener auditorías completadas (estado CERTIFICACION_EMITIDA)
        List<SolicitudAuditoria> auditoriasCompletadas = solicitudAuditoriaRepository
                .findByAuditorIdAndEstado(auditorId, EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);

        // 4. Calcular métricas
        MetricasAuditor metricas = calcularMetricas(perfil, auditoriasCompletadas);

        // 5. Calcular certificaciones públicas con vigencia
        List<CertificacionPublicaDTO> certificacionesPublicas = calcularCertificacionesPublicas(certificaciones);

        // 6. Calcular distribución de sectores
        List<DistribucionSectorDTO> distribucionSectores = calcularDistribucionSectores(auditoriasCompletadas);

        // 7. Obtener reseñas verificadas
        // TODO: Implementar cuando PP-56 cree la entidad Calificacion
        List<ResenaVerificadaDTO> resenas = Collections.emptyList();

        // 8. Ensamblar y retornar el DTO
        return mapper.aPerfilPublicoDto(perfil, metricas, certificacionesPublicas,
                distribucionSectores, resenas);
    }

    MetricasAuditor calcularMetricas(PerfilAuditor perfil, List<SolicitudAuditoria> auditoriasCompletadas) {
        if (auditoriasCompletadas.isEmpty()
                && (perfil.getTotalResenas() == 0 || perfil.getCalificacionPromedio() == null)) {
            return null;
        }

        BigDecimal calificacionPromedio = perfil.getCalificacionPromedio();
        Integer totalResenas = perfil.getTotalResenas();

        if (auditoriasCompletadas.isEmpty()) {
            // Perfil tiene calificación previa pero sin auditorías completadas en el listado actual
            return new MetricasAuditor(calificacionPromedio, totalResenas, 0, null);
        }

        Integer numAuditoriasCompletadas = auditoriasCompletadas.size();
        BigDecimal tiempoPromedioRespuestaDias = calcularTiempoPromedioRespuesta(perfil, auditoriasCompletadas);

        return new MetricasAuditor(calificacionPromedio, totalResenas,
                numAuditoriasCompletadas, tiempoPromedioRespuestaDias);
    }

    private BigDecimal calcularTiempoPromedioRespuesta(PerfilAuditor perfil,
                                                        List<SolicitudAuditoria> auditoriasCompletadas) {
        // Intentar calcular desde fechaAsignacion y fechaAceptacion de cada solicitud
        List<SolicitudAuditoria> conFechasRespuesta = auditoriasCompletadas.stream()
                .filter(s -> s.getFechaAsignacion() != null && s.getFechaAceptacion() != null)
                .toList();

        if (!conFechasRespuesta.isEmpty()) {
            long totalHoras = conFechasRespuesta.stream()
                    .mapToLong(s -> Duration.between(s.getFechaAsignacion(), s.getFechaAceptacion()).toHours())
                    .sum();
            BigDecimal promedioDias = BigDecimal.valueOf(totalHoras)
                    .divide(BigDecimal.valueOf(conFechasRespuesta.size() * 24L), 1, RoundingMode.HALF_UP);
            return promedioDias;
        }

        // Fallback: usar tiempoRespuestaHoras del perfil
        if (perfil.getTiempoRespuestaHoras() != null) {
            return BigDecimal.valueOf(perfil.getTiempoRespuestaHoras())
                    .divide(BigDecimal.valueOf(24), 1, RoundingMode.HALF_UP);
        }

        return null;
    }

    List<CertificacionPublicaDTO> calcularCertificacionesPublicas(List<Certificacion> certificaciones) {
        LocalDate hoy = LocalDate.now(clock);
        return certificaciones.stream()
                .map(cert -> {
                    String nombre = catalogoTiposCertificacion.buscar(cert.getTipo())
                            .map(def -> def.nombre())
                            .orElse(cert.getTipo() != null ? cert.getTipo().name() : "Certificación");
                    String entidadCertificadora = "CarbonHub";
                    LocalDate fechaVigencia = cert.getFechaVencimiento();
                    boolean vencida = fechaVigencia == null || fechaVigencia.isBefore(hoy);
                    return new CertificacionPublicaDTO(nombre, entidadCertificadora, fechaVigencia, vencida);
                })
                .toList();
    }

    List<DistribucionSectorDTO> calcularDistribucionSectores(List<SolicitudAuditoria> auditoriasCompletadas) {
        if (auditoriasCompletadas.isEmpty()) {
            return Collections.emptyList();
        }

        int total = auditoriasCompletadas.size();
        Map<String, Long> conteosPorSector = auditoriasCompletadas.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTipoCertificacion() != null ? s.getTipoCertificacion().name() : "OTRO",
                        Collectors.counting()));

        return conteosPorSector.entrySet().stream()
                .map(entry -> {
                    BigDecimal porcentaje = BigDecimal.valueOf(entry.getValue() * 100)
                            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
                    return new DistribucionSectorDTO(entry.getKey(), porcentaje);
                })
                .sorted(Comparator.comparing(DistribucionSectorDTO::getPorcentaje).reversed()
                        .thenComparing(DistribucionSectorDTO::getSector))
                .toList();
    }
}
