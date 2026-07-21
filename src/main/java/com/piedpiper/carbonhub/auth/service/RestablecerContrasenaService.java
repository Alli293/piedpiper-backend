package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ValidarTokenResetResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Service
public class RestablecerContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(RestablecerContrasenaService.class);

    private static final Pattern TOKEN_FORMATO = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final int MAX_SOLICITUDES_POR_HORA = 3;
    private static final String MENSAJE_SOLICITUD_UNIFORME =
            "Si existe una cuenta con ese correo, te enviamos un enlace para restablecer tu contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioCorreoResetContrasenaService envioCorreoResetContrasenaService;

    public RestablecerContrasenaService(UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        EnvioCorreoResetContrasenaService envioCorreoResetContrasenaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioCorreoResetContrasenaService = envioCorreoResetContrasenaService;
    }

    @Transactional
    public MensajeResponseDTO solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCaseForUpdate(email.trim()).ifPresent(usuario -> {
            if (!registrarIntentoDeSolicitud(usuario)) {
                return;
            }

            if (usuario.getMetodoAuth() != MetodoAuth.CORREO) {
                enviarTrasCommit(() ->
                        envioCorreoResetContrasenaService.enviarUsaGoogle(usuario.getNombre(), usuario.getEmail()));
                return;
            }

            String token = TokenVerificacionGenerator.generar();
            usuario.setTokenResetHash(TokenVerificacionGenerator.hash(token));
            usuario.setTokenResetExpiracion(TokenVerificacionGenerator.calcularExpiracion(1));
            usuarioRepository.saveAndFlush(usuario);

            enviarTrasCommit(() ->
                    envioCorreoResetContrasenaService.enviarReset(usuario.getNombre(), usuario.getEmail(), token));
        });

        return new MensajeResponseDTO(MENSAJE_SOLICITUD_UNIFORME);
    }

    @Transactional(readOnly = true)
    public ValidarTokenResetResponseDTO validarToken(String token) {
        Usuario usuario = buscarPorTokenValido(token);
        return new ValidarTokenResetResponseDTO(usuario.getEmail());
    }

    @Transactional
    public MensajeResponseDTO restablecer(String token, String nuevaContrasena) {
        Usuario usuario = buscarPorTokenValido(token);

        try {
            usuario.setPasswordHash(passwordEncoder.encode(nuevaContrasena));
            usuario.setTokenResetHash(null);
            usuario.setTokenResetExpiracion(null);
            usuarioRepository.saveAndFlush(usuario);
        } catch (Exception e) {
            log.error("Error inesperado al restablecer la contrasena del usuario {}", usuario.getId(), e);
            throw ApiException.errorInterno("No se pudo actualizar tu contraseña. Intenta nuevamente.");
        }

        return new MensajeResponseDTO("Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
    }

    private Usuario buscarPorTokenValido(String token) {
        if (token == null || !TOKEN_FORMATO.matcher(token).matches()) {
            throw ApiException.tokenResetMalFormado();
        }

        String tokenHash = TokenVerificacionGenerator.hash(token);
        Usuario usuario = usuarioRepository.findByTokenResetHashForUpdate(tokenHash)
                .orElseThrow(ApiException::tokenResetInvalido);

        if (usuario.getTokenResetExpiracion() == null
                || usuario.getTokenResetExpiracion().isBefore(Instant.now())) {
            throw ApiException.tokenResetInvalido();
        }

        return usuario;
    }

    /**
     * Ventana fija de 1 hora, máximo {@value #MAX_SOLICITUDES_POR_HORA} solicitudes.
     * Se aplica por igual a cuentas CORREO y GOOGLE (se llama antes de bifurcar por
     * {@code metodoAuth}), para que una cuenta vinculada a Google no pueda recibir avisos
     * "usa Google" sin límite. Al excederse, NO lanza excepción (a diferencia del reenvío
     * de verificación de PP-33): devolver un 429 aquí revelaría que la cuenta existe,
     * rompiendo la respuesta uniforme. El límite alcanzado se traduce simplemente en
     * "no enviar", en silencio.
     */
    private boolean registrarIntentoDeSolicitud(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant ventanaInicio = usuario.getResetContrasenaVentanaInicio();

        if (ventanaInicio == null || ventanaInicio.isBefore(ahora.minus(1, ChronoUnit.HOURS))) {
            usuario.setResetContrasenaVentanaInicio(ahora);
            usuario.setResetContrasenaContador(0);
        }

        if (usuario.getResetContrasenaContador() >= MAX_SOLICITUDES_POR_HORA) {
            return false;
        }

        usuario.setResetContrasenaContador(usuario.getResetContrasenaContador() + 1);
        return true;
    }

    private void enviarTrasCommit(Runnable envio) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }
}
