package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialResponseDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.service.PerfilInicialService;
import com.piedpiper.carbonhub.user.service.PreferenciasUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios/me")
@PreAuthorize("isAuthenticated()")
public class UsuarioController {

    private final PreferenciasUsuarioService preferenciasUsuarioService;
    private final PerfilInicialService perfilInicialService;

    public UsuarioController(PreferenciasUsuarioService preferenciasUsuarioService,
                             PerfilInicialService perfilInicialService) {
        this.preferenciasUsuarioService = preferenciasUsuarioService;
        this.perfilInicialService = perfilInicialService;
    }

    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciasUsuarioResponseDTO> obtenerPreferencias(
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(preferenciasUsuarioService.obtenerPreferencias(usuarioId));
    }

    @PutMapping("/preferencias")
    public ResponseEntity<PreferenciasUsuarioResponseDTO> actualizarPreferencias(
            Authentication authentication,
            @Valid @RequestBody PreferenciasUsuarioRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(
                preferenciasUsuarioService.actualizarPreferencias(usuarioId, request));
    }

    @GetMapping("/perfil-inicial")
    public ResponseEntity<PerfilInicialResponseDTO> obtenerPerfilInicial(
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(perfilInicialService.obtener(usuarioId));
    }

    @PutMapping("/perfil-inicial")
    public ResponseEntity<PerfilInicialResponseDTO> completarPerfilInicial(
            Authentication authentication,
            @Valid @RequestBody PerfilInicialRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(perfilInicialService.completar(usuarioId, request));
    }
}
