package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.ecoruta.service.EcoRutaInsigniaService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificacion/eventos")
@PreAuthorize("hasRole('CERTIFICACION')")
public class EcoRutaCertificacionEventoController {

    private final EcoRutaInsigniaService ecoRutaInsigniaService;

    public EcoRutaCertificacionEventoController(EcoRutaInsigniaService ecoRutaInsigniaService) {
        this.ecoRutaInsigniaService = ecoRutaInsigniaService;
    }

    @PostMapping
    public ResponseEntity<Void> recibir(@Valid @RequestBody EventoCertificacionRequestDTO request) {
        ecoRutaInsigniaService.evaluarYOtorgar(request);
        return ResponseEntity.ok().build();
    }
}
