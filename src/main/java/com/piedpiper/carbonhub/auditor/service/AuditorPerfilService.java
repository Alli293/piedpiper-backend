package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditorPerfilService {

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

        // 3. Verificar rol AUDITOR_CERTIFICADO
        if (auditor.getRol() != Rol.AUDITOR_CERTIFICADO) {
            throw ApiException.accesoDenegado("Solo usuarios con rol AUDITOR_CERTIFICADO pueden gestionar su perfil.");
        }

        // 4. Validar membership en catálogos usando Catalogos.desde()
        List<String> especialidadesInvalidas = request.getEspecialidades().stream()
                .filter(e -> Catalogos.desde(EspecialidadAuditor.class, e).isEmpty())
                .collect(Collectors.toList());

        if (!especialidadesInvalidas.isEmpty()) {
            throw ApiException.especialidadesInvalidas(especialidadesInvalidas);
        }

        List<String> zonasInvalidas = request.getZonasCobertura().stream()
                .filter(z -> Catalogos.desde(ProvinciaCR.class, z).isEmpty())
                .collect(Collectors.toList());

        if (!zonasInvalidas.isEmpty()) {
            throw ApiException.zonasInvalidas(zonasInvalidas);
        }

        // 5. Upsert PerfilAuditor
        PerfilAuditor perfil = perfilAuditorRepository.findByAuditorId(auditorId)
                .orElseGet(() -> PerfilAuditor.builder()
                        .auditor(auditor)
                        .build());

        Set<EspecialidadAuditor> especialidades = request.getEspecialidades().stream()
                .map(e -> Catalogos.desde(EspecialidadAuditor.class, e).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
        perfil.setEspecialidades(especialidades);

        Set<ProvinciaCR> zonas = request.getZonasCobertura().stream()
                .map(z -> Catalogos.desde(ProvinciaCR.class, z).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
        perfil.setZonasCobertura(zonas);

        perfil.setDisponible(request.getDisponible());
        perfil.setDescripcionProfesional(request.getDescripcionProfesional());
        perfil.setActualizadoEn(Instant.now());

        perfil = perfilAuditorRepository.save(perfil);

        // 6. Retornar DTO mapeado
        return perfilAuditorMapper.aResponseDto(perfil);
    }
}
