package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.DistribucionSectorAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilPublicoAuditorService {

    private static final String ENTIDAD_CERTIFICADORA_CARBONHUB = "CarbonHub";

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final CertificacionRepository certificacionRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final PerfilPublicoAuditorMapper mapper;
    private final Clock clock;

    public PerfilPublicoAuditorService(PerfilAuditorRepository perfilAuditorRepository,
                                       CertificacionRepository certificacionRepository,
                                       CatalogoTiposCertificacion catalogoTiposCertificacion,
                                       PerfilPublicoAuditorMapper mapper,
                                       Clock clock) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.certificacionRepository = certificacionRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoAuditorResponseDTO obtenerPerfilPublico(UUID auditorId) {
        // 1. Buscar perfil con estado ACTIVO (mismo 404 para inexistente y no activo)
        PerfilAuditor perfil = perfilAuditorRepository
                .findByAuditorIdAndAuditorEstadoConDistribucion(auditorId, EstadoUsuario.ACTIVO)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "El perfil solicitado no está disponible."));

        // 2. Obtener certificaciones del auditor
        List<Certificacion> certificaciones = certificacionRepository.findByAuditorId(auditorId);

        // 3. Leer métricas persistidas por PP-55
        MetricasAuditor metricas = metricasPersistidas(perfil);

        // 4. Calcular certificaciones públicas con vigencia
        List<CertificacionPublicaDTO> certificacionesPublicas = calcularCertificacionesPublicas(certificaciones);

        // 5. Leer distribución persistida por PP-55
        List<DistribucionSectorDTO> distribucionSectores = distribucionPersistida(perfil);

        // 6. Obtener reseñas verificadas
        // TODO: Implementar cuando PP-56 cree la entidad Calificacion
        List<ResenaVerificadaDTO> resenas = Collections.emptyList();

        // 7. Ensamblar y retornar el DTO
        return mapper.aPerfilPublicoDto(perfil, metricas, certificacionesPublicas,
                distribucionSectores, resenas);
    }

    private MetricasAuditor metricasPersistidas(PerfilAuditor perfil) {
        return new MetricasAuditor(
                perfil.getCalificacionPromedio(),
                perfil.getTotalResenas(),
                perfil.getAuditoriasCompletadas(),
                perfil.getTiempoPromedioRespuestaDias());
    }

    List<CertificacionPublicaDTO> calcularCertificacionesPublicas(List<Certificacion> certificaciones) {
        LocalDate hoy = LocalDate.now(clock);
        return certificaciones.stream()
                .map(cert -> {
                    String nombre = catalogoTiposCertificacion.buscar(cert.getTipo())
                            .map(def -> def.nombre())
                            .orElse(cert.getTipo() != null ? cert.getTipo().name() : "Certificación");
                    LocalDate fechaVigencia = cert.getFechaVencimiento();
                    boolean vencida = fechaVigencia == null || fechaVigencia.isBefore(hoy);
                    return new CertificacionPublicaDTO(
                            nombre,
                            ENTIDAD_CERTIFICADORA_CARBONHUB,
                            fechaVigencia,
                            vencida);
                })
                .toList();
    }

    private List<DistribucionSectorDTO> distribucionPersistida(PerfilAuditor perfil) {
        return perfil.getDistribucionSectores().stream()
                .map(this::aDto)
                .toList();
    }

    private DistribucionSectorDTO aDto(DistribucionSectorAuditor distribucion) {
        return new DistribucionSectorDTO(
                distribucion.getSector(),
                distribucion.getCantidad(),
                distribucion.getPorcentaje());
    }
}
