package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class VerificarCorreoService {

    private static final Logger log = LoggerFactory.getLogger(VerificarCorreoService.class);

    private static final int MAX_REENVIOS_POR_HORA = 3;
    private static final String MENSAJE_REENVIO_UNIFORME =
            "Si tu cuenta requiere verificación, te enviamos un nuevo enlace.";

    private final UsuarioRepository usuarioRepository;
    private final SolicitudValidacionRepository solicitudValidacionRepository;
    private final EnvioCorreoVerificacionService envioCorreoVerificacionService;

    public VerificarCorreoService(UsuarioRepository usuarioRepository,
                                  SolicitudValidacionRepository solicitudValidacionRepository,
                                  EnvioCorreoVerificacionService envioCorreoVerificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudValidacionRepository = solicitudValidacionRepository;
        this.envioCorreoVerificacionService = envioCorreoVerificacionService;
    }

    @Transactional
    public MensajeResponseDTO verificar(String token) {
        if (!TokenVerificacionGenerator.formatoValido(token)) {
            throw ApiException.tokenVerificacionMalFormado();
        }

        String tokenHash = TokenVerificacionGenerator.hash(token);
        Usuario usuario = usuarioRepository.findByTokenVerificacionHashForUpdate(tokenHash)
                .orElseThrow(ApiException::tokenVerificacionInvalido);

        if (usuario.getEstado() == EstadoUsuario.ACTIVO || usuario.getEstado() == EstadoUsuario.PENDIENTE_VALIDACION) {
            throw ApiException.correoYaVerificado();
        }
        if (usuario.getEstado() != EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw ApiException.cuentaNoDisponible();
        }

        if (usuario.getTokenVerificacionExpiracion() == null
                || usuario.getTokenVerificacionExpiracion().isBefore(Instant.now())) {
            throw ApiException.tokenVerificacionInvalido();
        }

        try {
            boolean esAuditor = usuario.getRol() == Rol.AUDITOR_CERTIFICADO;
            usuario.setEstado(esAuditor ? EstadoUsuario.PENDIENTE_VALIDACION : EstadoUsuario.ACTIVO);
            usuario.setTokenVerificacionHash(null);
            usuario.setTokenVerificacionExpiracion(null);
            usuarioRepository.saveAndFlush(usuario);

            if (esAuditor) {
                solicitudValidacionRepository.save(SolicitudValidacion.builder()
                        .auditor(usuario)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .fechaSolicitud(Instant.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error inesperado al verificar el correo del usuario {}", usuario.getId(), e);
            throw ApiException.errorInterno("No se pudo verificar tu correo. Intenta nuevamente.");
        }

        return new MensajeResponseDTO(mensajeDeExito(usuario.getRol()));
    }

    @Transactional
    public MensajeResponseDTO reenviar(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCaseForUpdate(email.trim());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
                registrarIntentoDeReenvio(usuario);

                String token = TokenVerificacionGenerator.generar();
                usuario.setTokenVerificacionHash(TokenVerificacionGenerator.hash(token));
                usuario.setTokenVerificacionExpiracion(TokenVerificacionGenerator.calcularExpiracion());
                usuarioRepository.saveAndFlush(usuario);

                enviarTrasCommit(usuario.getNombre(), usuario.getEmail(), token);
            }
        }

        return new MensajeResponseDTO(MENSAJE_REENVIO_UNIFORME);
    }

    private void registrarIntentoDeReenvio(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant ventanaInicio = usuario.getReenvioVerificacionVentanaInicio();

        if (ventanaInicio == null || ventanaInicio.isBefore(ahora.minus(1, ChronoUnit.HOURS))) {
            usuario.setReenvioVerificacionVentanaInicio(ahora);
            usuario.setReenvioVerificacionContador(0);
        }

        if (usuario.getReenvioVerificacionContador() >= MAX_REENVIOS_POR_HORA) {
            throw ApiException.reenviosVerificacionExcedidos();
        }

        usuario.setReenvioVerificacionContador(usuario.getReenvioVerificacionContador() + 1);
    }

    private void enviarTrasCommit(String nombre, String email, String token) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envioCorreoVerificacionService.enviar(nombre, email, token);
                }
            });
        } else {
            envioCorreoVerificacionService.enviar(nombre, email, token);
        }
    }

    private String mensajeDeExito(Rol rol) {
        if (rol == Rol.AUDITOR_CERTIFICADO) {
            return "Tu correo fue verificado. Tu cuenta de auditor está en revisión; "
                    + "te avisaremos cuando el administrador la apruebe.";
        }
        return "Tu correo fue verificado. Ya puedes iniciar sesión.";
    }
}
