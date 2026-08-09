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
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilPublicoAuditorService {

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final CertificacionRepository certificacionRepository;
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final PerfilPublicoAuditorMapper mapper;
    private final Clock clock;

    public PerfilPublicoAuditorService(PerfilAuditorRepository perfilAuditorRepository,
                                       CertificacionRepository certificacionRepository,
                                       SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                       PerfilPublicoAuditorMapper mapper,
                                       Clock clock) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.certificacionRepository = certificacionRepository;
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
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

        // 4. Calcular métricas y vigencia de certificaciones (placeholder para Task 3.2)
        MetricasAuditor metricas = null;
        List<CertificacionPublicaDTO> certificacionesPublicas = Collections.emptyList();
        List<DistribucionSectorDTO> distribucionSectores = Collections.emptyList();
        List<ResenaVerificadaDTO> resenas = Collections.emptyList();

        // 5. Ensamblar y retornar el DTO
        return mapper.aPerfilPublicoDto(perfil, metricas, certificacionesPublicas,
                distribucionSectores, resenas);
    }
}
