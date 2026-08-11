package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

/**
 * {@code hasRole('AUDITOR_CERTIFICADO')} solo valida el rol embebido en el token: un
 * auditor en {@code PENDIENTE_VALIDACION} conserva un token valido a proposito, porque
 * lo necesita para completar su configuracion inicial y consultar su solicitud. Todo
 * endpoint de auditor que no dependa de una asignacion previa (que ya exige estado
 * ACTIVO al crearse) para acotar el acceso debe validar el estado explicitamente con
 * este helper.
 */
public final class ValidacionesAuditor {

    private ValidacionesAuditor() {
    }

    public static boolean esAuditorCertificadoActivo(Usuario usuario) {
        return usuario.getRol() == Rol.AUDITOR_CERTIFICADO && usuario.getEstado() == EstadoUsuario.ACTIVO;
    }
}
