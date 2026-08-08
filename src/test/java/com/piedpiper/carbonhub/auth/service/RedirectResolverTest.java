package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El destino post-login lo decide el backend y el frontend solo obedece, asi que estas son las
 * pruebas de que cada rol aterriza donde corresponde.
 */
class RedirectResolverTest {

    /**
     * El auditor validado entra directo a sus solicitudes asignadas: es lo unico que tiene que
     * hacer al iniciar sesion. Antes caia en {@code /auditor/panel}, una pantalla vacia sin ninguna
     * entrada al listado, asi que la pantalla existia pero no se llegaba a ella.
     */
    @Test
    void unAuditorValidadoAterrizaEnSusSolicitudesAsignadas() {
        assertThat(RedirectResolver.paraUsuario(auditor(EstadoUsuario.ACTIVO)))
                .isEqualTo("/auditor/auditorias");
    }

    /** Mientras el admin no lo acepte no tiene solicitudes que ver, asi que espera aparte. */
    @Test
    void unAuditorPendienteDeValidacionSigueEnLaPantallaDeEspera() {
        assertThat(RedirectResolver.paraUsuario(auditor(EstadoUsuario.PENDIENTE_VALIDACION)))
                .isEqualTo("/auditor/validacion-pendiente");
    }

    /** La configuracion inicial manda sobre el destino por rol: sin ella no hay nada que mostrar. */
    @Test
    void unAuditorSinConfiguracionCompletaVaAConfigurarSuPerfil() {
        Usuario sinConfigurar = auditor(EstadoUsuario.ACTIVO);
        sinConfigurar.setConfiguracionCompleta(false);

        assertThat(RedirectResolver.paraUsuario(sinConfigurar))
                .isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void unAdministradorDeEmpresaAterrizaEnSuPanel() {
        assertThat(RedirectResolver.paraUsuario(administradorEmpresa()))
                .isEqualTo("/empresa/panel");
    }

    /**
     * Sin empresa asociada la configuracion que falta es la de la empresa, no la del perfil: el
     * administrador que se registra por su cuenta todavia no tiene a que empresa pertenecer.
     */
    @Test
    void unAdministradorDeEmpresaSinEmpresaVaAConfigurarLaEmpresa() {
        Usuario sinEmpresa = administradorEmpresa();
        sinEmpresa.setConfiguracionCompleta(false);
        sinEmpresa.setEmpresa(null);

        assertThat(RedirectResolver.paraUsuario(sinEmpresa))
                .isEqualTo("/empresa/configuracion-inicial");
    }

    private static Usuario auditor(EstadoUsuario estado) {
        return Usuario.builder()
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(estado)
                .configuracionCompleta(true)
                .build();
    }

    private static Usuario administradorEmpresa() {
        return Usuario.builder()
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .configuracionCompleta(true)
                .empresa(Empresa.builder().nombreEmpresa("Acme S.A.").build())
                .build();
    }
}
