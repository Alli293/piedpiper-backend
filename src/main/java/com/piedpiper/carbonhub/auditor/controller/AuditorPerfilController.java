package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.service.AuditorPerfilService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditores/{auditorId}/perfil")
@PreAuthorize("hasRole('AUDITOR_CERTIFICADO')")
public class AuditorPerfilController {

    private final AuditorPerfilService auditorPerfilService;

    public AuditorPerfilController(AuditorPerfilService auditorPerfilService) {
        this.auditorPerfilService = auditorPerfilService;
    }

    @PutMapping
    public ResponseEntity<PerfilAuditorResponseDTO> actualizar(
            @PathVariable UUID auditorId,
            @Valid @RequestBody ActualizarPerfilAuditorRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(auditorPerfilService.actualizar(usuarioId, auditorId, request));
    }
}
