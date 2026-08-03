package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verificacion publica de una credencial por su codigo corto (PP-68). Publico
 * y sin autenticacion a proposito: quien verifica es un tercero sin sesion en
 * CarbonHub (la ruta permitida vive en {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/verificar")
public class VerificacionPublicaController {

    private final ConsultaCertificacionService consultaCertificacionService;

    public VerificacionPublicaController(ConsultaCertificacionService consultaCertificacionService) {
        this.consultaCertificacionService = consultaCertificacionService;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<VerificacionCredencialDTO> verificar(@PathVariable String codigo) {
        return ResponseEntity.ok(consultaCertificacionService.verificarPorCodigo(codigo));
    }
}
