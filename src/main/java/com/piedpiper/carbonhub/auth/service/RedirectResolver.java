package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

public final class RedirectResolver {

    private RedirectResolver() {
    }

    public static String paraUsuario(Usuario usuario) {
        if (!usuario.isConfiguracionCompleta()) {
            return switch (usuario.getRol()) {
                case ADMINISTRADOR_EMPRESA -> usuario.getEmpresa() == null
                        ? "/empresa/configuracion-inicial"
                        : "/perfil/configuracion-inicial";
                case AUDITOR_CERTIFICADO -> "/auditor/configuracion-inicial";
                default -> "/perfil/configuracion-inicial";
            };
        }
        return switch (usuario.getRol()) {
            case ADMINISTRADOR_EMPRESA -> "/empresa/panel";
            // El auditor ya validado aterriza en sus solicitudes asignadas y no en un panel: es lo
            // unico que tiene que hacer al entrar, y hasta ahora caia en una pantalla vacia desde
            // la que no habia forma de llegar al listado.
            // Cualquier estado no-ACTIVO (PENDIENTE_VALIDACION o RECHAZADO) va a la pantalla de
            // espera: esa pantalla ya sabe distinguir "en revision" de "rechazado, con motivo".
            case AUDITOR_CERTIFICADO -> usuario.getEstado() != EstadoUsuario.ACTIVO
                    ? "/auditor/validacion-pendiente"
                    : "/auditor/auditorias";
            case ADMINISTRADOR_PLATAFORMA -> "/admin/panel";
            case USUARIO_INDIVIDUAL -> "/ecoruta/preferencias";
            default -> "/panel";
        };
    }
}
