package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conformidad con W3C Bitstring Status List. Estas pruebas fallan si la lista
 * publicada deja de tener la forma que un verificador espera, incluso si
 * ninguna certificacion ha sido revocada todavia.
 */
class ListaEstadoCredencialesServiceTest {

    private static final String URL_BASE = "https://carbonhub.example";

    private static RSAKey claveRsa;
    private static ListaEstadoCredencialesService servicio;

    @BeforeAll
    static void prepararServicio() throws Exception {
        claveRsa = new RSAKeyGenerator(2048).keyID("prueba-1").generate();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(claveRsa.toPrivateKey().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        FirmanteCredencialService firmante = new FirmanteCredencialService(pem, "prueba-1");
        GeneradorCredencialOpenBadges generador =
                new GeneradorCredencialOpenBadges(firmante, URL_BASE, "CarbonHub");
        servicio = new ListaEstadoCredencialesService(firmante, generador);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> vcDe(String jwt) throws Exception {
        return (Map<String, Object>) SignedJWT.parse(jwt).getJWTClaimsSet().getClaim("vc");
    }

    @Test
    void firmaLaListaConRs256YEsVerificableConLaClavePublica() throws Exception {
        SignedJWT firmado = SignedJWT.parse(servicio.generar());

        assertThat(firmado.verify(new RSASSAVerifier(claveRsa.toRSAPublicKey()))).isTrue();
    }

    @Test
    void emiteElTipoBitstringStatusListCredential() throws Exception {
        Map<String, Object> vc = vcDe(servicio.generar());

        assertThat((List<String>) vc.get("type"))
                .containsExactly("VerifiableCredential", "BitstringStatusListCredential");
        assertThat(vc.get("id")).isEqualTo(URL_BASE + "/api/certificaciones/estado/lista");
    }

    @Test
    void elSujetoEsUnaBitstringStatusListDePropositoDeRevocacion() throws Exception {
        Map<String, Object> vc = vcDe(servicio.generar());

        Map<String, Object> sujeto = (Map<String, Object>) vc.get("credentialSubject");
        assertThat(sujeto.get("type")).isEqualTo("BitstringStatusList");
        assertThat(sujeto.get("statusPurpose")).isEqualTo("revocation");
        assertThat((String) sujeto.get("encodedList")).isNotBlank();
    }

    @Test
    void elBitstringDecodificadoTieneElTamanoMinimoYNadaEstaRevocado() throws Exception {
        Map<String, Object> vc = vcDe(servicio.generar());
        Map<String, Object> sujeto = (Map<String, Object>) vc.get("credentialSubject");
        String encodedList = (String) sujeto.get("encodedList");

        // "u" es el prefijo multibase de base64url sin relleno.
        assertThat(encodedList).startsWith("u");
        byte[] comprimido = Base64.getUrlDecoder().decode(encodedList.substring(1));

        byte[] bitstring;
        try (GZIPInputStream gzip = new GZIPInputStream(
                new java.io.ByteArrayInputStream(comprimido))) {
            ByteArrayOutputStream descomprimido = new ByteArrayOutputStream();
            gzip.transferTo(descomprimido);
            bitstring = descomprimido.toByteArray();
        }

        assertThat(bitstring).hasSize(ListaEstadoCredencialesService.TAMANO_BITS / 8);
        // Todos los bits en cero: nada ha sido revocado todavia.
        assertThat(bitstring).containsOnly((byte) 0);
    }

    @Test
    void elIssuerEsIdenticoAlUsadoEnLasCertificacionesIndividuales() throws Exception {
        GeneradorCredencialOpenBadges generador =
                new GeneradorCredencialOpenBadges(
                        new FirmanteCredencialService(pemDeClavePrueba(), "prueba-1"),
                        URL_BASE, "CarbonHub");
        Map<String, Object> vc = vcDe(servicio.generar());

        assertThat(vc.get("issuer")).isEqualTo(generador.construirEmisor());
    }

    private static String pemDeClavePrueba() throws Exception {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(claveRsa.toPrivateKey().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }
}
