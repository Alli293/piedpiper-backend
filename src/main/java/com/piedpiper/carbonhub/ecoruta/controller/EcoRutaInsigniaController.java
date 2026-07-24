package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.InsigniaUsuarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaInsigniaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ecoruta/insignias")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaInsigniaController {

    private final EcoRutaInsigniaService ecoRutaInsigniaService;

    public EcoRutaInsigniaController(EcoRutaInsigniaService ecoRutaInsigniaService) {
        this.ecoRutaInsigniaService = ecoRutaInsigniaService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<InsigniaUsuarioResponseDTO>> listarObtenidas(
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(ecoRutaInsigniaService.listarObtenidas(usuarioId));
    }
}
