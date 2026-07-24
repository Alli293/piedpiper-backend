package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.ecoruta.service.InsigniaEcoRutaService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificacion/eventos")
public class CertificacionInsigniaController {

    private final InsigniaEcoRutaService insigniaEcoRutaService;

    public CertificacionInsigniaController(InsigniaEcoRutaService insigniaEcoRutaService) {
        this.insigniaEcoRutaService = insigniaEcoRutaService;
    }

    @PostMapping
    public ResponseEntity<Void> recibir(@Valid @RequestBody EventoCertificacionRequestDTO request) {
        insigniaEcoRutaService.evaluarYOtorgar(request);
        return ResponseEntity.ok().build();
    }
}
