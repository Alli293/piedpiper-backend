package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacionesAuditorTest {

    private static Usuario usuario(Rol rol, EstadoUsuario estado) {
        return Usuario.builder().id(UUID.randomUUID()).rol(rol).estado(estado).build();
    }

    @Test
    void auditorCertificadoYActivoEsValido() {
        assertThat(ValidacionesAuditor.esAuditorCertificadoActivo(
                usuario(Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO))).isTrue();
    }

    @Test
    void auditorPendienteDeValidacionNoEsValidoAunqueTengaElRol() {
        assertThat(ValidacionesAuditor.esAuditorCertificadoActivo(
                usuario(Rol.AUDITOR_CERTIFICADO, EstadoUsuario.PENDIENTE_VALIDACION))).isFalse();
    }

    @Test
    void auditorRechazadoNoEsValido() {
        assertThat(ValidacionesAuditor.esAuditorCertificadoActivo(
                usuario(Rol.AUDITOR_CERTIFICADO, EstadoUsuario.RECHAZADO))).isFalse();
    }

    @Test
    void auditorDeshabilitadoNoEsValido() {
        assertThat(ValidacionesAuditor.esAuditorCertificadoActivo(
                usuario(Rol.AUDITOR_CERTIFICADO, EstadoUsuario.DESHABILITADO))).isFalse();
    }

    @Test
    void usuarioActivoConOtroRolNoEsValido() {
        assertThat(ValidacionesAuditor.esAuditorCertificadoActivo(
                usuario(Rol.ADMINISTRADOR_EMPRESA, EstadoUsuario.ACTIVO))).isFalse();
    }
}
