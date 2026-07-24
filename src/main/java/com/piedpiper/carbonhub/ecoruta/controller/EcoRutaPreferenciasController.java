package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.PreferenciasViajeService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ecoruta/preferencias")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaPreferenciasController {

    private final PreferenciasViajeService service;

    public EcoRutaPreferenciasController(PreferenciasViajeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PreferenciasViajeResponseDTO> obtenerPreferencias(Authentication authentication) {
        return service.obtener(Autenticaciones.usuarioId(authentication))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PreferenciasViajeResponseDTO> guardarPreferencias(
            @Valid @RequestBody PreferenciasViajeRequestDTO request,
            Authentication authentication) {
        PreferenciasViajeResponseDTO response =
                service.guardar(Autenticaciones.usuarioId(authentication), request);
        HttpStatus status = response.isRecienCreada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
