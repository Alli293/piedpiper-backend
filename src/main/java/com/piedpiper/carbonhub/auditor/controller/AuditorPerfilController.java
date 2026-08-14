package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResultadoPerfil;
import com.piedpiper.carbonhub.auditor.service.AuditorPerfilService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * Lo que la pantalla de perfil necesita para mostrar lo ya guardado antes de dejar editar.
     *
     * <p>Sin este endpoint el cliente pedía un {@code GET} sobre una ruta mapeada solo para
     * {@code PUT}, así que recibía 405 y mostraba un error de carga: el formulario abría en blanco
     * aunque el auditor tuviera especialidades y zonas guardadas, y al guardar las pisaba.</p>
     */
    @GetMapping
    public ResponseEntity<PerfilAuditorResponseDTO> obtener(
            @PathVariable UUID auditorId, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(auditorPerfilService.obtener(usuarioId, auditorId));
    }

    @PutMapping
    public ResponseEntity<PerfilAuditorResponseDTO> actualizar(
            @PathVariable UUID auditorId,
            @Valid @RequestBody ActualizarPerfilAuditorRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ResultadoPerfil resultado = auditorPerfilService.actualizar(usuarioId, auditorId, request);
        if (resultado.creado()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado.dto());
        }
        return ResponseEntity.ok(resultado.dto());
    }
}
