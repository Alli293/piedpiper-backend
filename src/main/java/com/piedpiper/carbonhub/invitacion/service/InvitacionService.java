package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionPublicaResponseDTO;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionRequestDTO;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionResponseDTO;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.models.enums.EstadoInvitacion;
import com.piedpiper.carbonhub.invitacion.repository.InvitacionRepository;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InvitacionService {

    private static final long DIAS_EXPIRACION = 7;

    private final InvitacionRepository invitacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnvioCorreoInvitacion envioCorreoInvitacion;

    public InvitacionService(InvitacionRepository invitacionRepository,
                             UsuarioRepository usuarioRepository,
                             EnvioCorreoInvitacion envioCorreoInvitacion) {
        this.invitacionRepository = invitacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.envioCorreoInvitacion = envioCorreoInvitacion;
    }

    @Transactional
    public InvitacionResponseDTO emitir(UUID usuarioId, InvitacionRequestDTO request) {
        Usuario administrador = validarAdministrador(usuarioId);
        UUID empresaId = administrador.getEmpresa().getId();
        String email = request.getEmail().trim();
        Instant ahora = Instant.now();

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            if (usuario.getEmpresa() != null && empresaId.equals(usuario.getEmpresa().getId())) {
                throw ApiException.invitacionCorreoYaEnEmpresa();
            }
        });

        if (invitacionRepository.existsByEmpresaIdAndEmailIgnoreCaseAndEstadoAndFechaExpiracionAfter(
                empresaId, email, EstadoInvitacion.ENVIADA, ahora)) {
            throw ApiException.invitacionPendiente();
        }

        String token = TokenVerificacionGenerator.generar();

        Invitacion invitacion = Invitacion.builder()
                .email(email)
                .empresa(administrador.getEmpresa())
                .tokenHash(TokenVerificacionGenerator.hash(token))
                .estado(EstadoInvitacion.ENVIADA)
                .fechaEmision(ahora)
                .fechaExpiracion(ahora.plus(DIAS_EXPIRACION, ChronoUnit.DAYS))
                .build();

        invitacion = invitacionRepository.save(invitacion);

        envioCorreoInvitacion.enviar(email, administrador.getEmpresa().getNombreEmpresa(), token);

        return aDto(invitacion, ahora);
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponseDTO> listar(UUID usuarioId) {
        Usuario administrador = validarAdministrador(usuarioId);
        Instant ahora = Instant.now();
        return invitacionRepository
                .findAllByEmpresaIdOrderByFechaEmisionDesc(administrador.getEmpresa().getId())
                .stream()
                .map(invitacion -> aDto(invitacion, ahora))
                .toList();
    }

    @Transactional
    public InvitacionResponseDTO revocar(UUID usuarioId, UUID invitacionId) {
        Usuario administrador = validarAdministrador(usuarioId);
        Instant ahora = Instant.now();

        Invitacion invitacion = invitacionRepository
                .findByIdAndEmpresaId(invitacionId, administrador.getEmpresa().getId())
                .orElseThrow(ApiException::invitacionNoEncontrada);

        if (invitacion.estadoEfectivo(ahora) != EstadoInvitacion.ENVIADA) {
            throw ApiException.invitacionNoRevocable();
        }

        invitacion.setEstado(EstadoInvitacion.REVOCADA);
        invitacion = invitacionRepository.save(invitacion);

        return aDto(invitacion, ahora);
    }

    @Transactional(readOnly = true)
    public InvitacionPublicaResponseDTO resolver(String token) {
        Invitacion invitacion = invitacionRepository
                .findByTokenHash(TokenVerificacionGenerator.hash(token))
                .orElseThrow(ApiException::invitacionInvalida);

        if (invitacion.getEstado() == EstadoInvitacion.ACEPTADA) {
            throw ApiException.invitacionYaUtilizada();
        }
        if (invitacion.getEstado() != EstadoInvitacion.ENVIADA) {
            throw ApiException.invitacionInvalida();
        }
        if (invitacion.expirada(Instant.now())) {
            throw ApiException.invitacionExpirada();
        }

        return new InvitacionPublicaResponseDTO(
                invitacion.getEmail(), invitacion.getEmpresa().getNombreEmpresa());
    }

    private Usuario validarAdministrador(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "Solo el administrador de la empresa puede gestionar invitaciones."));
        if (usuario.getRol() != Rol.ADMINISTRADOR_EMPRESA) {
            throw ApiException.accesoDenegado(
                    "Solo el administrador de la empresa puede gestionar invitaciones.");
        }
        if (usuario.getEmpresa() == null) {
            throw ApiException.invitacionSinEmpresa();
        }
        return usuario;
    }

    private InvitacionResponseDTO aDto(Invitacion invitacion, Instant ahora) {
        return new InvitacionResponseDTO(
                invitacion.getId(),
                invitacion.getEmail(),
                invitacion.estadoEfectivo(ahora).name(),
                invitacion.getFechaEmision(),
                invitacion.getFechaExpiracion());
    }
}
