package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.AlertaVencimientoNotificacionDTO;
import com.piedpiper.carbonhub.notification.service.EmailAlertaVencimientoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Envia el correo de una alerta de vencimiento y registra el resultado (PP-71).
 *
 * <p>A proposito no es transaccional: lee los datos en la transaccion de
 * {@link AlertaVencimientoDatosService}, envia el correo afuera y escribe el resultado en la
 * transaccion de {@link AlertaEstadoEnvioService}. Asi el envio, que es un efecto externo y puede
 * tardar, no mantiene una transaccion abierta ni arrastra a la base si falla.</p>
 */
@Service
public class AlertaVencimientoNotificacionService {

    /**
     * Validacion de formato del destinatario. El repo usa {@code @Email} de Bean Validation, pero eso
     * aplica a DTOs de entrada y aca el correo viene de la base, asi que se valida a mano.
     */
    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");

    private static final Logger log = LoggerFactory.getLogger(AlertaVencimientoNotificacionService.class);

    private final AlertaVencimientoDatosService alertaVencimientoDatosService;
    private final AlertaEstadoEnvioService alertaEstadoEnvioService;
    private final EmailAlertaVencimientoService emailAlertaVencimientoService;

    public AlertaVencimientoNotificacionService(
            AlertaVencimientoDatosService alertaVencimientoDatosService,
            AlertaEstadoEnvioService alertaEstadoEnvioService,
            EmailAlertaVencimientoService emailAlertaVencimientoService) {
        this.alertaVencimientoDatosService = alertaVencimientoDatosService;
        this.alertaEstadoEnvioService = alertaEstadoEnvioService;
        this.emailAlertaVencimientoService = emailAlertaVencimientoService;
    }

    public void notificar(UUID alertaId) {
        Optional<AlertaVencimientoNotificacionDTO> datos = alertaVencimientoDatosService.datosDe(alertaId);
        if (datos.isEmpty()) {
            log.warn("No se encontro la alerta {} al intentar notificarla", alertaId);
            return;
        }

        AlertaVencimientoNotificacionDTO alerta = datos.get();
        if (!correoValido(alerta.getCorreoDestinatario())) {
            log.error("Alerta {}: el correo destinatario de la empresa {} no tiene un formato valido, "
                            + "se omite el envio", alertaId, alerta.getNombreEmpresa());
            alertaEstadoEnvioService.marcarFallidaSinReintento(alertaId);
            return;
        }

        // Reclamar es lo que autoriza a enviar: sube el contador solo si la alerta sigue pendiente y
        // le quedan intentos, en una sola sentencia. Si otro proceso la tomo primero, esto devuelve
        // false y aca se corta, que es lo que evita el correo duplicado.
        if (!alertaEstadoEnvioService.reclamar(alertaId)) {
            log.debug("La alerta {} ya no estaba disponible para enviar, se omite", alertaId);
            return;
        }

        try {
            emailAlertaVencimientoService.enviarAlertaVencimiento(
                    alerta.getCorreoDestinatario(),
                    alerta.getNombreEmpresa(),
                    alerta.getNombreCertificacion(),
                    alerta.getFechaVencimiento(),
                    alerta.getDiasRestantes(),
                    alerta.getUrlCertificacion());
        } catch (Exception e) {
            log.error("Fallo el envio del correo de la alerta {} ({})", alertaId, alerta.getTipoAlerta(), e);
            alertaEstadoEnvioService.registrarFallo(alertaId);
            return;
        }

        alertaEstadoEnvioService.marcarEnviada(alertaId);
        log.info("Correo de alerta {} enviado a {}", alerta.getTipoAlerta(), alerta.getCorreoDestinatario());
    }

    private static boolean correoValido(String correo) {
        return correo != null && FORMATO_CORREO.matcher(correo).matches();
    }
}
