package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.service.FirmanteCredencialService;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCredencialOpenBadges;
import com.piedpiper.carbonhub.certificacion.service.ListaEstadoCredencialesService;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints publicos que resuelven los identificadores de la credencial.
 *
 * <p>Deben ser accesibles sin autenticacion: quien verifica una credencial es un
 * tercero sin sesion en CarbonHub, y necesita resolver {@code issuer.id},
 * {@code achievement.id} y la clave publica para validar la firma. La lista de
 * rutas permitidas vive en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/certificaciones")
public class CertificacionEmisorController {

    private final GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    private final FirmanteCredencialService firmanteCredencialService;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final ListaEstadoCredencialesService listaEstadoCredencialesService;

    public CertificacionEmisorController(
            GeneradorCredencialOpenBadges generadorCredencialOpenBadges,
            FirmanteCredencialService firmanteCredencialService,
            CatalogoTiposCertificacion catalogoTiposCertificacion,
            ListaEstadoCredencialesService listaEstadoCredencialesService) {
        this.generadorCredencialOpenBadges = generadorCredencialOpenBadges;
        this.firmanteCredencialService = firmanteCredencialService;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.listaEstadoCredencialesService = listaEstadoCredencialesService;
    }

    @GetMapping("/emisor")
    public ResponseEntity<Map<String, Object>> emisor() {
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("@context", GeneradorCredencialOpenBadges.contexto());
        perfil.putAll(generadorCredencialOpenBadges.construirEmisor());
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/emisor/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(firmanteCredencialService.jwksPublico());
    }

    /**
     * Lista de estado de revocacion (W3C Bitstring Status List), como VC-JWT
     * compacto. Hoy siempre reporta todo vigente: no existe todavia una accion
     * para revocar una certificacion.
     */
    @GetMapping("/estado/lista")
    public ResponseEntity<String> listaEstado() {
        return ResponseEntity.ok(listaEstadoCredencialesService.generar());
    }

    @GetMapping("/logros/{codigo}")
    public ResponseEntity<Map<String, Object>> logro(@PathVariable String codigo) {
        return TipoCertificacion.desde(codigo)
                .flatMap(catalogoTiposCertificacion::buscar)
                .map(definicion -> {
                    Map<String, Object> logro = new LinkedHashMap<>();
                    logro.put("@context", GeneradorCredencialOpenBadges.contexto());
                    logro.putAll(generadorCredencialOpenBadges.construirLogro(definicion));
                    return ResponseEntity.ok(logro);
                })
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "El tipo de certificacion no existe."));
    }
}
