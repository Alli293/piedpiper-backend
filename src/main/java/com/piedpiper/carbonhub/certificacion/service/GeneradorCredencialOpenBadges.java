package com.piedpiper.carbonhub.certificacion.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construye la credencial OpenBadges 3.0 y la entrega firmada como VC-JWT.
 *
 * <p>La forma del documento la fija la especificacion y no debe "mejorarse":
 * los dos valores de {@code @context} van en ese orden exacto, los tipos son
 * {@code VerifiableCredential} y {@code OpenBadgeCredential}, y las fechas usan
 * {@code validFrom}/{@code validUntil} del Verifiable Credentials Data Model
 * 2.0 (nunca {@code issuanceDate}/{@code expirationDate}, que son de la v1.1).
 *
 * @see <a href="https://www.imsglobal.org/spec/ob/v3p0">OpenBadges 3.0</a>
 */
@Service
public class GeneradorCredencialOpenBadges {

    private static final Logger log = LoggerFactory.getLogger(GeneradorCredencialOpenBadges.class);

    public static final String CONTEXTO_VC = "https://www.w3.org/ns/credentials/v2";
    public static final String CONTEXTO_OPENBADGES =
            "https://purl.imsglobal.org/spec/ob/v3p0/context-3.0.3.json";

    /** Los dos contextos, en el orden exacto que exige la especificacion. */
    public static List<String> contexto() {
        return List.of(CONTEXTO_VC, CONTEXTO_OPENBADGES);
    }

    private final FirmanteCredencialService firmanteCredencialService;
    private final String urlBase;
    private final String emisorNombre;

    public GeneradorCredencialOpenBadges(
            FirmanteCredencialService firmanteCredencialService,
            @Value("${certificaciones.emision.url-base:http://localhost:8080}") String urlBase,
            @Value("${certificaciones.emision.emisor-nombre:CarbonHub}") String emisorNombre) {
        this.firmanteCredencialService = firmanteCredencialService;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.emisorNombre = emisorNombre;
    }

    public String generar(Certificacion certificacion, DefinicionCertificacion definicion) {
        String credencialId = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> credencial = construirCredencial(certificacion, definicion, credencialId);

        Date vencimiento = Date.from(certificacion.getFechaVencimiento()
                .atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(urlEmisor())
                .subject(urlEmpresa(certificacion))
                .jwtID(credencialId)
                .notBeforeTime(Date.from(certificacion.getFechaEmision()))
                .issueTime(Date.from(certificacion.getFechaEmision()))
                .expirationTime(vencimiento)
                .claim("vc", credencial)
                .build();

        return firmanteCredencialService.firmar(claims);
    }

    /**
     * Decodifica un VC-JWT ya firmado y devuelve el documento JSON-LD tal cual
     * fue firmado (el claim {@code vc}). Nunca reconstruir el documento
     * llamando de nuevo a {@link #construirCredencial}: ese metodo genera un
     * {@code id} aleatorio nuevo en cada llamada, asi que el resultado no
     * coincidiria con lo que realmente se firmo para esta certificacion.
     *
     * <p>Deliberadamente no reverifica la firma: solo lee datos propios ya
     * guardados en la base de datos (no entrada de un tercero), y acoplar la
     * lectura a {@link FirmanteCredencialService} dejaria indescargables las
     * certificaciones ya emitidas si la clave de firma se rota o se retira mas
     * adelante.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> decodificar(String credencialJwt) {
        try {
            Object vc = SignedJWT.parse(credencialJwt).getJWTClaimsSet().getClaim("vc");
            if (!(vc instanceof Map)) {
                throw ApiException.certificacionNoDisponible();
            }
            return (Map<String, Object>) vc;
        } catch (ParseException e) {
            log.error("No se pudo decodificar la credencial de una certificacion", e);
            throw ApiException.certificacionNoDisponible();
        }
    }

    private Map<String, Object> construirCredencial(Certificacion certificacion,
                                                    DefinicionCertificacion definicion,
                                                    String credencialId) {
        // LinkedHashMap para que el orden del documento sea estable y verificable.
        Map<String, Object> credencial = new LinkedHashMap<>();
        credencial.put("@context", contexto());
        credencial.put("id", credencialId);
        credencial.put("type", List.of("VerifiableCredential", "OpenBadgeCredential"));
        credencial.put("name", definicion.nombre());
        credencial.put("description", definicion.descripcion());
        credencial.put("issuer", construirEmisor());
        credencial.put("validFrom", certificacion.getFechaEmision().toString());
        credencial.put("validUntil", certificacion.getFechaVencimiento()
                .atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC).toString());
        credencial.put("credentialSubject", construirSujeto(certificacion, definicion));
        credencial.put("credentialStatus", construirEstadoCredencial(certificacion));
        credencial.put("evidence", List.of(construirEvidencia(certificacion)));
        return credencial;
    }

    /**
     * Referencia a la auditoria aprobada que respalda la certificacion. Sin
     * {@code id} a proposito: todavia no existe una entidad {@code Auditoria}
     * en el backend con una URL resoluble a la que apuntar (ver el comentario
     * en {@code Certificacion.idAuditoria}); el campo {@code id} de Evidence es
     * opcional en el Verifiable Credentials Data Model 2.0.
     */
    private Map<String, Object> construirEvidencia(Certificacion certificacion) {
        Map<String, Object> evidencia = new LinkedHashMap<>();
        evidencia.put("type", List.of("Evidence"));
        evidencia.put("name", "Auditoría aprobada");
        evidencia.put("narrative", "ID de auditoría: " + certificacion.getIdAuditoria());
        return evidencia;
    }

    /**
     * Entrada {@code BitstringStatusListEntry} (W3C Bitstring Status List) que
     * ubica esta certificacion dentro de {@link ListaEstadoCredencialesService}.
     * Nada revoca todavia por este medio -- no existe esa accion -- pero el
     * campo debe firmarse desde ya: agregarlo despues dejaria irrevocables para
     * siempre las certificaciones ya emitidas.
     */
    private Map<String, Object> construirEstadoCredencial(Certificacion certificacion) {
        String indice = String.valueOf(certificacion.getIndiceEstado());
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("id", urlListaEstado() + "#" + indice);
        estado.put("type", "BitstringStatusListEntry");
        estado.put("statusPurpose", "revocation");
        estado.put("statusListIndex", indice);
        estado.put("statusListCredential", urlListaEstado());
        return estado;
    }

    /**
     * Perfil {@code Profile} del emisor. Publico porque el mismo documento debe
     * servirse en la URL a la que apunta {@code issuer.id}: si divergieran, un
     * verificador que resuelva esa URL no podria confirmar al emisor.
     */
    public Map<String, Object> construirEmisor() {
        Map<String, Object> emisor = new LinkedHashMap<>();
        emisor.put("id", urlEmisor());
        emisor.put("type", List.of("Profile"));
        emisor.put("name", emisorNombre);
        emisor.put("url", urlBase);
        return emisor;
    }

    private Map<String, Object> construirSujeto(Certificacion certificacion,
                                                DefinicionCertificacion definicion) {
        Map<String, Object> sujeto = new LinkedHashMap<>();
        sujeto.put("id", urlEmpresa(certificacion));
        sujeto.put("type", List.of("AchievementSubject"));
        sujeto.put("achievement", construirLogro(definicion));
        return sujeto;
    }

    /**
     * Logro {@code Achievement}. Publico por la misma razon que el perfil del
     * emisor: se sirve en la URL a la que apunta {@code achievement.id}.
     */
    public Map<String, Object> construirLogro(DefinicionCertificacion definicion) {
        Map<String, Object> criterio = new LinkedHashMap<>();
        criterio.put("narrative", definicion.criterio());

        Map<String, Object> logro = new LinkedHashMap<>();
        logro.put("id", urlLogro(definicion));
        logro.put("type", List.of("Achievement"));
        logro.put("name", definicion.nombre());
        logro.put("description", definicion.descripcion());
        logro.put("achievementType", definicion.tipoLogro().getToken());
        logro.put("criteria", criterio);
        return logro;
    }

    /**
     * Publico porque {@link ListaEstadoCredencialesService} firma su VC-JWT con
     * esta misma URL como {@code iss}: el emisor debe ser identico en toda
     * credencial que este backend firme, sea una certificacion individual o la
     * lista de estado que las respalda.
     */
    /**
     * Nombre legible del emisor (CarbonHub): es la entidad certificadora que
     * respalda toda certificacion, ya que hoy no existe una entidad
     * "entidad certificadora" propia distinta de CarbonHub.
     */
    public String emisorNombre() {
        return emisorNombre;
    }

    public String urlEmisor() {
        return urlBase + "/api/certificaciones/emisor";
    }

    /**
     * Publico porque {@link ListaEstadoCredencialesService} usa esta misma URL
     * como {@code id} de la lista que publica: deben coincidir siempre.
     */
    public String urlListaEstado() {
        return urlBase + "/api/certificaciones/estado/lista";
    }

    /**
     * URL publica de verificacion de una certificacion puntual (sin sesion),
     * usada por el frontend para armar el enlace de "Compartir en LinkedIn" y
     * como destino de {@code CertificacionEmisorController#verificar}.
     */
    public String urlVerificacion(UUID certificacionId) {
        return urlBase + "/api/certificaciones/" + certificacionId + "/verificar";
    }

    private String urlLogro(DefinicionCertificacion definicion) {
        return urlBase + "/api/certificaciones/logros/" + definicion.tipo().getCodigo();
    }

    private String urlEmpresa(Certificacion certificacion) {
        return urlBase + "/api/empresas/" + certificacion.getEmpresa().getId();
    }
}
