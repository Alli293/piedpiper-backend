package com.piedpiper.carbonhub.notification.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnlacesCorreoConfigTest {

    @Test
    void enlacesHttpsDelHostPermitidoSonValidos() {
        EnlacesCorreoConfig config = config("app.carbonhub.cr", "https://app.carbonhub.cr");

        assertThatCode(config::validar).doesNotThrowAnyException();
    }

    @Test
    void localhostPermiteHttpParaDesarrollo() {
        EnlacesCorreoConfig config = config("localhost,127.0.0.1", "http://localhost:4200");

        assertThatCode(config::validar).doesNotThrowAnyException();
    }

    @Test
    void enlaceHttpFueraDeLocalEsRechazado() {
        EnlacesCorreoConfig config = config("app.carbonhub.cr", "http://app.carbonhub.cr");

        assertThatThrownBy(config::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void hostNoPermitidoEsRechazado() {
        EnlacesCorreoConfig config = config("app.carbonhub.cr", "https://phishing.example");

        assertThatThrownBy(config::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("host que no esta permitido");
    }

    @Test
    void enlaceConQueryOFragmentoPreexistenteEsRechazado() {
        EnlacesCorreoConfig config = config(
                "app.carbonhub.cr", "https://app.carbonhub.cr/login?redirect=https://phishing.example");

        assertThatThrownBy(config::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("query ni fragmento");
    }

    private EnlacesCorreoConfig config(String hosts, String base) {
        return new EnlacesCorreoConfig(
                hosts,
                base + "/verificar-correo",
                base + "/registro/invitacion",
                base + "/reset-contrasena",
                base + "/login",
                base + "/empresa/{slug}/reputacion/certificaciones",
                base);
    }
}
