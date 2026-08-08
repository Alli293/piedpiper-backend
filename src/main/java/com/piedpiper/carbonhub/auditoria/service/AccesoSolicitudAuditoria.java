package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Unica regla de quien puede consultar una solicitud de auditoria y todo lo que cuelga de ella.
 *
 * <p>Vive en un colaborador y no repetida en cada servicio porque el detalle y los documentos son
 * el mismo recurso visto de dos formas: si las reglas se escriben dos veces, tarde o temprano una
 * de las dos se amplia sola y un documento queda accesible para alguien que no puede ver la
 * solicitud que lo contiene.</p>
 */
@Component
public class AccesoSolicitudAuditoria {

    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;

    public AccesoSolicitudAuditoria(TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository) {
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
    }

    /**
     * El auditor que ya no figura asignado se resuelve por el historial: tras un rechazo o un
     * vencimiento el campo {@code auditor} queda nulo, y sin mirar el historial perderia el acceso
     * a la auditoria en la que participo justo despues de responderla.
     */
    public boolean puedeConsultar(SolicitudAuditoria solicitud, Usuario usuario) {
        if (usuario.getRol() == Rol.ADMINISTRADOR_PLATAFORMA) {
            return true;
        }

        UUID empresaSolicitud = solicitud.getEmpresa() == null ? null : solicitud.getEmpresa().getId();
        UUID empresaUsuario = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
        if (empresaSolicitud != null && empresaSolicitud.equals(empresaUsuario)) {
            return true;
        }

        if (solicitud.getAuditor() != null && solicitud.getAuditor().getId().equals(usuario.getId())) {
            return true;
        }

        return transicionEstadoAuditoriaRepository
                .idsAuditoresConHistorial(solicitud.getId())
                .contains(usuario.getId());
    }
}
