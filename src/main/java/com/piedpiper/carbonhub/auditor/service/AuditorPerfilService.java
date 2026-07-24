package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ZonaCobertura;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditorPerfilService {

    private static final Set<String> ESPECIALIDADES_VALIDAS = Arrays.stream(EspecialidadAuditor.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private static final Set<String> ZONAS_VALIDAS = Arrays.stream(ZonaCobertura.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilAuditorMapper perfilAuditorMapper;

    public AuditorPerfilService(PerfilAuditorRepository perfilAuditorRepository,
                                UsuarioRepository usuarioRepository,
                                PerfilAuditorMapper perfilAuditorMapper) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilAuditorMapper = perfilAuditorMapper;
    }

    @Transactional
    public PerfilAuditorResponseDTO actualizar(UUID usuarioId, UUID auditorId,
                                                ActualizarPerfilAuditorRequestDTO request) {
        // 1. Verificar propiedad: usuarioId == auditorId
        if (!usuarioId.equals(auditorId)) {
            throw ApiException.perfilNoPropio();
        }

        // 2. Buscar usuario, verificar estado ACTIVO
        Usuario auditor = usuarioRepository.findById(auditorId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("Auditor no encontrado."));

        if (auditor.getEstado() != EstadoUsuario.ACTIVO) {
            throw ApiException.cuentaNoValidada();
        }

        // 3. Validar membership en catálogos
        List<String> especialidadesInvalidas = request.getEspecialidades().stream()
                .filter(e -> !ESPECIALIDADES_VALIDAS.contains(e))
                .collect(Collectors.toList());

        if (!especialidadesInvalidas.isEmpty()) {
            throw ApiException.especialidadesInvalidas(especialidadesInvalidas);
        }

        List<String> zonasInvalidas = request.getZonasCobertura().stream()
                .filter(z -> !ZONAS_VALIDAS.contains(z))
                .collect(Collectors.toList());

        if (!zonasInvalidas.isEmpty()) {
            throw ApiException.zonasInvalidas(zonasInvalidas);
        }

        // 4. Upsert PerfilAuditor
        PerfilAuditor perfil = perfilAuditorRepository.findByAuditorId(auditorId)
                .orElseGet(() -> PerfilAuditor.builder()
                        .auditor(auditor)
                        .build());

        Set<EspecialidadAuditor> especialidades = request.getEspecialidades().stream()
                .map(EspecialidadAuditor::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
        perfil.setEspecialidades(especialidades);
        perfil.setZonasCobertura(perfilAuditorMapper.listToCsv(request.getZonasCobertura()));
        perfil.setDisponible(request.getDisponible());
        perfil.setDescripcionProfesional(request.getDescripcionProfesional());
        perfil.setActualizadoEn(Instant.now());

        perfil = perfilAuditorRepository.save(perfil);

        // 5. Retornar DTO mapeado
        return perfilAuditorMapper.aResponseDto(perfil);
    }
}
