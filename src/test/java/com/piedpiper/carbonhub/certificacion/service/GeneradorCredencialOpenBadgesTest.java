package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Suite de conformidad con OpenBadges 3.0. Verifica la forma exacta del
 * documento emitido: si estas pruebas fallan, la credencial deja de ser
 * conforme aunque el resto del sistema funcione.
 */
class GeneradorCredencialOpenBadgesTest {

    private static final String URL_BASE = "https://carbonhub.example";
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final long INDICE_ESTADO = 42L;

    private static RSAKey claveRsa;
    private static GeneradorCredencialOpenBadges generador;
    private static CatalogoTiposCertificacion catalogo;

    @BeforeAll
    static void prepararFirmante() throws Exception {
        claveRsa = new RSAKeyGenerator(2048).keyID("prueba-1").generate();
        FirmanteCredencialService firmante =
                new FirmanteCredencialService(pemDe(claveRsa), "prueba-1");
        generador = new GeneradorCredencialOpenBadges(firmante, URL_BASE, "CarbonHub");
        catalogo = new CatalogoTiposCertificacion();
    }

    private static String pemDe(RSAKey clave) throws Exception {
        return "-----BEGIN PRIVATE KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(clave.toPrivateKey().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private Certificacion certificacion(TipoCertificacion tipo) {
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .idAuditoria(UUID.randomUUID())
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .auditor(Usuario.builder().id(UUID.randomUUID()).build())
                .tipo(tipo)
                .fechaEmision(Instant.parse("2026-07-25T12:00:00Z"))
                .fechaVencimiento(LocalDate.of(2027, 7, 25))
                .estado(EstadoCertificacion.ACTIVA)
                .indiceEstado(INDICE_ESTADO)
                .build();
    }

    private DefinicionCertificacion definicion(TipoCertificacion tipo) {
        return catalogo.buscar(tipo).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> credencialDe(TipoCertificacion tipo) throws Exception {
        String jwt = generador.generar(certificacion(tipo), definicion(tipo));
        return (Map<String, Object>) SignedJWT.parse(jwt).getJWTClaimsSet().getClaim("vc");
    }

    @Test
    void firmaLaCredencialConRs256YElKidDelEmisor() throws Exception {
        String jwt = generador.generar(certificacion(TipoCertificacion.CARBONO_NEUTRAL),
                definicion(TipoCertificacion.CARBONO_NEUTRAL));

        SignedJWT firmado = SignedJWT.parse(jwt);

        assertThat(firmado.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(firmado.getHeader().getKeyID()).isEqualTo("prueba-1");
        assertThat(firmado.verify(new RSASSAVerifier(claveRsa.toRSAPublicKey()))).isTrue();
    }

    @Test
    void emiteElContextoDeOpenBadges30EnElOrdenExigido() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        assertThat(credencial.get("@context")).asInstanceOf(
                        org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                .containsExactly(
                        "https://www.w3.org/ns/credentials/v2",
                        "https://purl.imsglobal.org/spec/ob/v3p0/context-3.0.3.json");
    }

    @Test
    void emiteLosTiposVerifiableCredentialYOpenBadgeCredential() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        assertThat((List<String>) credencial.get("type"))
                .containsExactly("VerifiableCredential", "OpenBadgeCredential");
    }

    @Test
    void usaValidFromYValidUntilYNoLaNomenclaturaDeLaVersion11() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        assertThat(credencial).containsKeys("validFrom", "validUntil");
        // Guarda de regresion: issuanceDate/expirationDate son del Verifiable
        // Credentials Data Model 1.1 y no deben reaparecer.
        assertThat(credencial).doesNotContainKeys("issuanceDate", "expirationDate");
        assertThat(credencial.get("validFrom")).isEqualTo("2026-07-25T12:00:00Z");
        assertThat(credencial.get("validUntil")).isEqualTo("2027-07-25T00:00:00Z");
    }

    @Test
    void emiteUnEmisorProfileConIdResoluble() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        Map<String, Object> emisor = (Map<String, Object>) credencial.get("issuer");
        assertThat(emisor.get("type")).isEqualTo("Profile");
        assertThat(emisor.get("name")).isEqualTo("CarbonHub");
        assertThat(emisor.get("id")).isEqualTo(URL_BASE + "/api/certificaciones/emisor");
    }

    @Test
    void emiteUnAchievementSubjectQueContieneElLogro() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        Map<String, Object> sujeto = (Map<String, Object>) credencial.get("credentialSubject");
        assertThat(sujeto.get("type")).isEqualTo("AchievementSubject");
        assertThat(sujeto.get("id")).isEqualTo(URL_BASE + "/api/empresas/" + EMPRESA_ID);

        Map<String, Object> logro = (Map<String, Object>) sujeto.get("achievement");
        assertThat(logro.get("type")).isEqualTo("Achievement");
        assertThat(logro.get("name")).isEqualTo("Carbono Neutral");
        assertThat(logro.get("id"))
                .isEqualTo(URL_BASE + "/api/certificaciones/logros/carbono_neutral");
        assertThat((Map<String, Object>) logro.get("criteria")).containsKey("narrative");
    }

    @Test
    void usaUrnUuidComoIdentificadorDeLaCredencial() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        assertThat((String) credencial.get("id")).startsWith("urn:uuid:");
    }

    @Test
    void mapeaCarbonoNeutralAlAchievementTypeCertification() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        Map<String, Object> sujeto = (Map<String, Object>) credencial.get("credentialSubject");
        Map<String, Object> logro = (Map<String, Object>) sujeto.get("achievement");
        assertThat(logro.get("achievementType")).isEqualTo("Certification");
    }

    @Test
    void mapeaInventarioGeiAlAchievementTypeQualityAssuranceCredential() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.INVENTARIO_GEI);

        Map<String, Object> sujeto = (Map<String, Object>) credencial.get("credentialSubject");
        Map<String, Object> logro = (Map<String, Object>) sujeto.get("achievement");
        assertThat(logro.get("achievementType")).isEqualTo("QualityAssuranceCredential");
    }

    @Test
    void elPerfilDelEmisorEsIdenticoAlEmbebidoEnLaCredencial() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        // Un verificador resuelve issuer.id y compara: si divergieran, no podria
        // confirmar al emisor.
        assertThat(generador.construirEmisor()).isEqualTo(credencial.get("issuer"));
    }

    @Test
    void emiteUnCredentialStatusQueApuntaALaListaConElIndiceCorrecto() throws Exception {
        Map<String, Object> credencial = credencialDe(TipoCertificacion.CARBONO_NEUTRAL);

        Map<String, Object> estado = (Map<String, Object>) credencial.get("credentialStatus");
        String urlLista = URL_BASE + "/api/certificaciones/estado/lista";
        assertThat(estado.get("type")).isEqualTo("BitstringStatusListEntry");
        assertThat(estado.get("statusPurpose")).isEqualTo("revocation");
        assertThat(estado.get("statusListCredential")).isEqualTo(urlLista);
        assertThat(estado.get("statusListIndex")).isEqualTo(String.valueOf(INDICE_ESTADO));
        assertThat(estado.get("id")).isEqualTo(urlLista + "#" + INDICE_ESTADO);
    }

    @Test
    void sinClaveConfiguradaLaEmisionFallaYNoDevuelveCredencialSinFirmar() {
        GeneradorCredencialOpenBadges sinClave = new GeneradorCredencialOpenBadges(
                new FirmanteCredencialService("", "sin-clave"), URL_BASE, "CarbonHub");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sinClave.generar(
                        certificacion(TipoCertificacion.CARBONO_NEUTRAL),
                        definicion(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(com.piedpiper.carbonhub.exceptions.ApiException.class);
    }
}
