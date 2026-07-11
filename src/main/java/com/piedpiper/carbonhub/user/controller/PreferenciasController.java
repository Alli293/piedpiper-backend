package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.user.models.dtos.PreferenciasRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasResponseDTO;
import com.piedpiper.carbonhub.user.service.PreferenciasService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints de preferencias de interfaz del usuario autenticado (PP-31).
 * El principal del contexto de seguridad es el UUID del usuario
 * (ver {@code JwtAuthenticationFilter}).
 */
@RestController
@RequestMapping("/api/usuarios/me/preferencias")
public class PreferenciasController {

    private final PreferenciasService preferenciasService;

    public PreferenciasController(PreferenciasService preferenciasService) {
        this.preferenciasService = preferenciasService;
    }

    @GetMapping
    public ResponseEntity<PreferenciasResponseDTO> obtener(Authentication authentication) {
        return ResponseEntity.ok(preferenciasService.obtener(usuarioId(authentication)));
    }

    @PutMapping
    public ResponseEntity<PreferenciasResponseDTO> actualizar(
            Authentication authentication,
            @Valid @RequestBody PreferenciasRequestDTO request) {
        return ResponseEntity.ok(preferenciasService.actualizar(usuarioId(authentication), request));
    }

    private UUID usuarioId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
