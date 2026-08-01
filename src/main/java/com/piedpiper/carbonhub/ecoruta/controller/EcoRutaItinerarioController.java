package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaItinerarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ecoruta/itinerarios")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaItinerarioController {

    private final EcoRutaItinerarioService service;

    public EcoRutaItinerarioController(EcoRutaItinerarioService service) {
        this.service = service;
    }

    @PostMapping("/generar")
    public ResponseEntity<ItinerarioResponseDTO> generar(Authentication authentication) {
        ItinerarioResponseDTO response = service.generar(Autenticaciones.usuarioId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItinerarioResponseDTO> obtener(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.obtener(id, Autenticaciones.usuarioId(authentication)));
    }
}
