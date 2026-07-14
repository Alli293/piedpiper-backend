package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.service.PreferenciasUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios/me/preferencias")
public class UsuarioController {

    private final PreferenciasUsuarioService preferenciasUsuarioService;

    public UsuarioController(PreferenciasUsuarioService preferenciasUsuarioService) {
        this.preferenciasUsuarioService = preferenciasUsuarioService;
    }

    @GetMapping
    public ResponseEntity<PreferenciasUsuarioResponseDTO> obtenerPreferencias(
            Authentication authentication) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(preferenciasUsuarioService.obtenerPreferencias(usuarioId));
    }

    @PutMapping
    public ResponseEntity<PreferenciasUsuarioResponseDTO> actualizarPreferencias(
            Authentication authentication,
            @Valid @RequestBody PreferenciasUsuarioRequestDTO request) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(
                preferenciasUsuarioService.actualizarPreferencias(usuarioId, request));
    }
}
