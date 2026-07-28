package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Firma credenciales OpenBadges 3.0 como VC-JWT.
 *
 * <p>Usa RS256 con Nimbus JOSE+JWT, que ya esta en el classpath a traves de
 * {@code spring-security-oauth2-jose}. Se eligio RSA y no EdDSA porque
 * {@code Ed25519Signer} de Nimbus delega en Google Tink, declarado
 * {@code optional} en el POM de Nimbus y por tanto ausente del classpath:
 * usar EdDSA exigiria una dependencia nueva. Migrar a EdDSA mas adelante es
 * un cambio de constante {@link JWSAlgorithm} mas esa dependencia.
 *
 * <p>No confundir con {@code auth.service.JwtService}: aquel emite tokens de
 * <em>sesion</em> con jjwt y un secreto simetrico HMAC, valido porque solo este
 * backend los verifica. Una credencial, en cambio, debe poder verificarla un
 * tercero sin secreto compartido, de ahi la firma asimetrica y el JWKS publico.
 * Son dos mecanismos distintos a proposito; no unificarlos.
 *
 * <p><strong>Desviacion documentada</strong> (CONVENTIONS §4): sin clave
 * configurada el bean arranca igualmente y es {@link #firmar} quien falla. Se
 * prefirio esto a fallar en el constructor para no impedir el arranque local
 * de quien trabaja en otras funcionalidades, siguiendo el mismo criterio que
 * {@code HttpCertificacionEventosClient} con su URL y su clave API. El fallo
 * queda en el log interno y la emision es reintentable, que es justamente lo
 * que pide PP-58.
 */
@Service
public class FirmanteCredencialService {

    private static final Logger log = LoggerFactory.getLogger(FirmanteCredencialService.class);

    private final RSAKey claveRsa;
    private final String claveId;

    public FirmanteCredencialService(
            @Value("${certificaciones.emision.clave-privada-pem:}") String clavePrivadaPem,
            @Value("${certificaciones.emision.clave-id:carbonhub-emision-1}") String claveId) {
        this.claveId = claveId;
        this.claveRsa = cargarClave(clavePrivadaPem, claveId);
        if (this.claveRsa == null) {
            log.warn("No hay clave privada configurada para firmar certificaciones: la emision "
                    + "fallara hasta definir CERTIFICACIONES_CLAVE_PRIVADA_PEM.");
        }
    }

    /**
     * Lee una clave privada RSA en PEM PKCS#8 usando solo JCA.
     *
     * <p>Se evita a proposito {@code RSAKey.parseFromPEMEncodedObjects}, que
     * delega en BouncyCastle: igual que Tink, es una dependencia opcional de
     * Nimbus y no esta en el classpath. La clave publica se reconstruye a partir
     * del modulo y el exponente publico de la privada, que toda clave RSA en
     * formato CRT expone.
     */
    private static RSAKey cargarClave(String clavePrivadaPem, String claveId) {
        if (clavePrivadaPem == null || clavePrivadaPem.isBlank()) {
            return null;
        }
        try {
            String base64 = clavePrivadaPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey privada = factory.generatePrivate(new PKCS8EncodedKeySpec(der));
            if (!(privada instanceof RSAPrivateCrtKey crt)) {
                log.error("La clave privada de certificaciones no esta en formato RSA CRT.");
                return null;
            }
            RSAPublicKey publica = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));

            // alg y use quedan en la clave (privada y publica comparten estos
            // metadatos): jwksPublico() los expone via toPublicJWK() sin duplicar
            // la configuracion, y un verificador estricto los exige.
            return new RSAKey.Builder(publica)
                    .privateKey(privada)
                    .keyID(claveId)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            log.error("La clave privada configurada para firmar certificaciones no es valida", e);
            return null;
        }
    }

    public boolean claveConfigurada() {
        return claveRsa != null;
    }

    public String firmar(JWTClaimsSet claims) {
        if (claveRsa == null) {
            log.error("Se intento emitir una certificacion sin clave de firma configurada");
            throw ApiException.errorInterno(
                    "No se pudo emitir la certificacion. El administrador de la plataforma "
                            + "puede reintentarlo.");
        }
        JWSHeader cabecera = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(claveId)
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT jwt = new SignedJWT(cabecera, claims);
        try {
            jwt.sign(new RSASSASigner(claveRsa));
        } catch (JOSEException e) {
            log.error("Error al firmar la credencial de la certificacion", e);
            throw ApiException.errorInterno(
                    "No se pudo emitir la certificacion. El administrador de la plataforma "
                            + "puede reintentarlo.");
        }
        return jwt.serialize();
    }

    /** Conjunto JWKS publico para que un verificador externo valide la firma. */
    public Map<String, Object> jwksPublico() {
        if (claveRsa == null) {
            return new JWKSet().toJSONObject();
        }
        return new JWKSet(claveRsa.toPublicJWK()).toJSONObject();
    }
}
