package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Publica la Bitstring Status List (W3C) que respalda {@code credentialStatus}
 * en cada certificacion emitida.
 *
 * <p>Hoy no existe ninguna accion para revocar una certificacion, asi que esta
 * lista siempre reporta todo como vigente (todos los bits en cero). Publicarla
 * desde ya es lo que permite que, cuando exista esa accion, revocar sea solo
 * "cambiar un bit y volver a firmar esta lista" -- nunca reemitir credenciales
 * ya entregadas.
 *
 * <p><strong>Limite conocido:</strong> el tamano fijo (131 072 bits, el minimo
 * que recomienda el estandar por privacidad) alcanza para esa misma cantidad de
 * certificaciones. Si CarbonHub llegara a emitir mas, hace falta repartir el
 * estado en varias listas ("shards"); no se construye eso ahora porque no hay
 * volumen que lo justifique.
 *
 * <p>El JWT se cachea por {@link #DURACION_CACHE}: es un endpoint publico sin
 * autenticacion, firmar es una operacion RSA en cada llamada, y sin revocar
 * todavia el documento es identico entre refrescos. Sin cache, cada request
 * regenera un JWT distinto (solo cambian {@code validFrom}/{@code iat}) y le
 * impide a cualquier capa rio abajo cachear la respuesta.
 *
 * @see <a href="https://www.w3.org/TR/vc-bitstring-status-list/">Bitstring Status List v1.0</a>
 */
@Service
public class ListaEstadoCredencialesService {

    private static final Logger log = LoggerFactory.getLogger(ListaEstadoCredencialesService.class);

    /** Minimo recomendado por el estandar para el tamano del conjunto de anonimato. */
    static final int TAMANO_BITS = 131_072;

    /**
     * Cuanto tiempo se reutiliza el JWT firmado antes de regenerarlo. Acota
     * cuanto tarda un futuro revocar en propagarse a un verificador que
     * respete el cache; hoy, sin esa accion, solo evita firmar de mas.
     */
    private static final Duration DURACION_CACHE = Duration.ofMinutes(15);

    private final FirmanteCredencialService firmanteCredencialService;
    private final GeneradorCredencialOpenBadges generadorCredencialOpenBadges;

    private String jwtCacheado;
    private Instant expiracionCache = Instant.MIN;

    public ListaEstadoCredencialesService(FirmanteCredencialService firmanteCredencialService,
                                          GeneradorCredencialOpenBadges generadorCredencialOpenBadges) {
        this.firmanteCredencialService = firmanteCredencialService;
        this.generadorCredencialOpenBadges = generadorCredencialOpenBadges;
    }

    public synchronized String generar() {
        Instant ahora = Instant.now();
        if (jwtCacheado == null || ahora.isAfter(expiracionCache)) {
            jwtCacheado = construir(ahora);
            expiracionCache = ahora.plus(DURACION_CACHE);
        }
        return jwtCacheado;
    }

    private String construir(Instant ahora) {
        String encodedList = codificar(new byte[TAMANO_BITS / 8]);

        Map<String, Object> sujeto = new LinkedHashMap<>();
        sujeto.put("id", generadorCredencialOpenBadges.urlListaEstado() + "#list");
        sujeto.put("type", "BitstringStatusList");
        sujeto.put("statusPurpose", "revocation");
        sujeto.put("encodedList", encodedList);

        Map<String, Object> credencial = new LinkedHashMap<>();
        credencial.put("@context", List.of(GeneradorCredencialOpenBadges.CONTEXTO_VC));
        credencial.put("id", generadorCredencialOpenBadges.urlListaEstado());
        credencial.put("type", List.of("VerifiableCredential", "BitstringStatusListCredential"));
        credencial.put("issuer", generadorCredencialOpenBadges.construirEmisor());
        credencial.put("validFrom", ahora.toString());
        credencial.put("credentialSubject", sujeto);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(generadorCredencialOpenBadges.urlEmisor())
                .issueTime(Date.from(ahora))
                .claim("vc", credencial)
                .build();

        return firmanteCredencialService.firmar(claims);
    }

    /** Comprime en GZIP y codifica en multibase base64url sin relleno ("u" + base64url). */
    private String codificar(byte[] bitstring) {
        ByteArrayOutputStream comprimido = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(comprimido)) {
            gzip.write(bitstring);
        } catch (IOException e) {
            log.error("Error al comprimir la lista de estado de revocacion", e);
            throw ApiException.errorInterno(
                    "No se pudo generar la lista de estado de las certificaciones.");
        }
        return "u" + Base64.getUrlEncoder().withoutPadding().encodeToString(comprimido.toByteArray());
    }
}
