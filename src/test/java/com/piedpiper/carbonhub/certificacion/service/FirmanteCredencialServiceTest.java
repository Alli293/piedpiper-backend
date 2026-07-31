package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.RSAKey;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FirmanteCredencialServiceTest {

    private static String pemDe(RSAKey clave) throws Exception {
        return "-----BEGIN PRIVATE KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(clave.toPrivateKey().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    @Test
    void jwksPublicoDeclaraAlgYUsePorqueUnVerificadorEstrictoLosExige() throws Exception {
        RSAKey claveRsa = new RSAKeyGenerator(2048).keyID("prueba-1").generate();
        FirmanteCredencialService servicio =
                new FirmanteCredencialService(pemDe(claveRsa), "prueba-1");

        Map<String, Object> jwks = servicio.jwksPublico();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claves = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(claves).hasSize(1);
        assertThat(claves.get(0).get("alg")).isEqualTo("RS256");
        assertThat(claves.get(0).get("use")).isEqualTo("sig");
        assertThat(claves.get(0).get("kid")).isEqualTo("prueba-1");
    }

    @Test
    void jwksPublicoEsUnConjuntoVacioSinClaveConfigurada() {
        FirmanteCredencialService servicio = new FirmanteCredencialService("", "prueba-1");

        Map<String, Object> jwks = servicio.jwksPublico();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claves = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(claves).isEmpty();
    }
}
