package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResultadoPerfil;
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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
    public ResultadoPerfil actualizar(UUID usuarioId, UUID auditorId,
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

        // 4-5. Validar duplicados y membership en catálogos (dedup normaliza mayúsculas/espacios)
        Set<EspecialidadAuditor> especialidades = Catalogos.resolverConjunto(
                EspecialidadAuditor.class, request.getEspecialidades(),
                () -> ApiException.datosInvalidos("La lista de especialidades contiene duplicados."),
                ApiException::especialidadesInvalidas);

        Set<ProvinciaCR> zonas = Catalogos.resolverConjunto(
                ProvinciaCR.class, request.getZonasCobertura(),
                () -> ApiException.datosInvalidos("La lista de zonas de cobertura contiene duplicados."),
                ApiException::zonasInvalidas);

        // 6. Upsert PerfilAuditor — determinar si es creación o actualización
        var existente = perfilAuditorRepository.findByAuditorId(auditorId);
        boolean creado = existente.isEmpty();

        PerfilAuditor perfil = existente.orElseGet(() -> PerfilAuditor.builder()
                .auditor(auditor)
                .build());

        perfil.setEspecialidades(especialidades);
        perfil.setZonasCobertura(zonas);

        perfil.setDisponible(request.getDisponible());
        perfil.setDescripcionProfesional(request.getDescripcionProfesional());
        perfil.setActualizadoEn(Instant.now());

        try {
            perfil = perfilAuditorRepository.save(perfil);
        } catch (DataIntegrityViolationException e) {
            // Another request created the profile concurrently — retry as update
            PerfilAuditor existenteConcurrente = perfilAuditorRepository.findByAuditorId(auditorId)
                    .orElseThrow(() -> ApiException.errorInterno("Error al crear el perfil."));
            existenteConcurrente.setEspecialidades(especialidades);
            existenteConcurrente.setZonasCobertura(zonas);
            existenteConcurrente.setDisponible(request.getDisponible());
            existenteConcurrente.setDescripcionProfesional(request.getDescripcionProfesional());
            existenteConcurrente.setActualizadoEn(Instant.now());
            perfil = perfilAuditorRepository.save(existenteConcurrente);
            return new ResultadoPerfil(perfilAuditorMapper.aResponseDto(perfil), false);
        }

        // 7. Retornar resultado con flag de creación
        return new ResultadoPerfil(perfilAuditorMapper.aResponseDto(perfil), creado);
    }
}
