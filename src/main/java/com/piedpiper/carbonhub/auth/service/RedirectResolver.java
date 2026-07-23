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
                default -> "/perfil/configuracion-inicial";
            };
        }
        return switch (usuario.getRol()) {
            case ADMINISTRADOR_EMPRESA -> "/empresa/panel";
            case AUDITOR_CERTIFICADO -> usuario.getEstado() == EstadoUsuario.PENDIENTE_VALIDACION
                    ? "/auditor/validacion-pendiente"
                    : "/auditor/panel";
            case ADMINISTRADOR_PLATAFORMA -> "/admin/panel";
            case USUARIO_INDIVIDUAL -> "/ecoruta";
            default -> "/panel";
        };
    }
}
