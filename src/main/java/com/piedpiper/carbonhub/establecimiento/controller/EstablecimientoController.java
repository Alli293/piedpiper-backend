package com.piedpiper.carbonhub.establecimiento.controller;

import com.piedpiper.carbonhub.establecimiento.models.dtos.EstablecimientoBannerResponseDTO;
import com.piedpiper.carbonhub.establecimiento.service.EstablecimientoBannerService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/establecimientos")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EstablecimientoController {

    private final EstablecimientoBannerService service;

    public EstablecimientoController(EstablecimientoBannerService service) {
        this.service = service;
    }

    @GetMapping("/{id}/banner")
    public ResponseEntity<EstablecimientoBannerResponseDTO> obtenerBanner(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenerBanner(id));
    }
}
