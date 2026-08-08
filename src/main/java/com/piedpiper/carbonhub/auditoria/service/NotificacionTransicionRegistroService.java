package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.NotificacionTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.NotificacionTransicionAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Deja encolada una notificacion por cada destinatario de una transicion de estado.
 *
 * <p>Corre dentro de la transaccion de la transicion: si el cambio de estado se revierte, las
 * notificaciones se revierten con el, y nadie recibe un correo sobre algo que no ocurrio.</p>
 */
@Service
public class NotificacionTransicionRegistroService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransicionRegistroService.class);

    /**
     * Valida el correo antes de encolarlo para no gastar los tres intentos del barrido en una
     * direccion que nunca va a poder entregarse. Es el mismo criterio de PP-71.
     */
    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");

    private final NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository;

    public NotificacionTransicionRegistroService(
            NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository) {
        this.notificacionTransicionAuditoriaRepository = notificacionTransicionAuditoriaRepository;
    }

    @Transactional
    public void encolar(SolicitudAuditoria solicitud,
                        EstadoSolicitudAuditoria estadoAnterior,
                        EstadoSolicitudAuditoria estadoNuevo,
                        EventoTransicionAuditoria evento) {
        // Sin guard por empresa nula: la relacion es @ManyToOne(optional = false) con columna
        // not null, asi que una solicitud persistida siempre la tiene.
        Empresa empresa = solicitud.getEmpresa();
        String nombreEmpresa = empresa.getNombreEmpresa();

        encolarDestinatario(solicitud, empresa.getCorreoCorporativo(), nombreEmpresa,
                nombreEmpresa, estadoAnterior, estadoNuevo, evento);

        Usuario auditor = solicitud.getAuditor();
        if (auditor != null) {
            encolarDestinatario(solicitud, auditor.getEmail(), auditor.nombreCompleto(),
                    nombreEmpresa, estadoAnterior, estadoNuevo, evento);
        }
    }

    private void encolarDestinatario(SolicitudAuditoria solicitud,
                                     String email,
                                     String nombreDestinatario,
                                     String nombreEmpresa,
                                     EstadoSolicitudAuditoria estadoAnterior,
                                     EstadoSolicitudAuditoria estadoNuevo,
                                     EventoTransicionAuditoria evento) {
        if (email == null || !CORREO_VALIDO.matcher(email).matches()) {
            log.warn("No se encola la notificacion de la transicion {} de la solicitud {}: "
                            + "el correo del destinatario no tiene un formato valido",
                    evento, solicitud.getId());
            return;
        }

        notificacionTransicionAuditoriaRepository.save(NotificacionTransicionAuditoria.builder()
                .solicitud(solicitud)
                .destinatarioEmail(email)
                .destinatarioNombre(Usuario.recortarNombre(nombreDestinatario))
                .nombreEmpresa(nombreEmpresa)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .estado(EstadoNotificacionTransicion.PENDIENTE)
                .fechaCreacion(Instant.now())
                .build());
    }
}
