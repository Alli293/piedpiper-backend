package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
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

    /**
     * Devuelve el perfil del auditor para que la pantalla lo muestre antes de editarlo.
     *
     * <p><b>404 cuando todavía no hay perfil.</b> El auditor recién validado que nunca guardó llega
     * acá sin fila, y eso no es un error: la pantalla lo interpreta como "primera vez" y abre el
     * formulario vacío. Devolver 200 con un cuerpo vacío obligaría al cliente a distinguir "sin
     * datos" de "sin perfil" mirando los campos.</p>
     *
     * <p>Las mismas comprobaciones que {@link #actualizar}: se lee el perfil propio y nada más. El
     * identificador de la ruta no puede apuntar a otro auditor aunque el rol sea el correcto.</p>
     */
    @Transactional(readOnly = true)
    public PerfilAuditorResponseDTO obtener(UUID usuarioId, UUID auditorId) {
        verificarAccesoAlPerfil(usuarioId, auditorId);

        return perfilAuditorRepository.findByAuditorId(auditorId)
                .map(perfilAuditorMapper::aResponseDto)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "Todavía no has configurado tu perfil de auditor."));
    }

    @Transactional
    public ResultadoPerfil actualizar(UUID usuarioId, UUID auditorId,
                                      ActualizarPerfilAuditorRequestDTO request) {
        Usuario auditor = verificarAccesoAlPerfil(usuarioId, auditorId);

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

    /**
     * Precondiciones compartidas por la lectura y la escritura del perfil: que sea el propio, que la
     * cuenta esté validada y que el rol sea el correcto. Vive en un solo lugar para que las dos
     * operaciones no puedan divergir, que es como se abren los huecos de autorización.
     */
    private Usuario verificarAccesoAlPerfil(UUID usuarioId, UUID auditorId) {
        if (!usuarioId.equals(auditorId)) {
            throw ApiException.perfilNoPropio();
        }

        Usuario auditor = usuarioRepository.findById(auditorId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("Auditor no encontrado."));

        if (auditor.getEstado() != EstadoUsuario.ACTIVO) {
            throw ApiException.cuentaNoValidada();
        }

        if (auditor.getRol() != Rol.AUDITOR_CERTIFICADO) {
            throw ApiException.accesoDenegado("Solo usuarios con rol AUDITOR_CERTIFICADO pueden gestionar su perfil.");
        }

        return auditor;
    }
}
