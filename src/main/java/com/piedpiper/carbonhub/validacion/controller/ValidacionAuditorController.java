package com.piedpiper.carbonhub.validacion.controller;

import com.piedpiper.carbonhub.validacion.models.dtos.DecisionSolicitudRequestDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.service.ValidacionAuditorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/solicitudes-auditor")
@PreAuthorize("hasRole('ADMINISTRADOR_PLATAFORMA')")
public class ValidacionAuditorController {

    private final ValidacionAuditorService validacionAuditorService;

    public ValidacionAuditorController(ValidacionAuditorService validacionAuditorService) {
        this.validacionAuditorService = validacionAuditorService;
    }

    @GetMapping
    public ResponseEntity<PaginaSolicitudesResponseDTO> listarPendientes(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(validacionAuditorService.listarPendientes(usuarioId, pagina));
    }

    @PostMapping("/{solicitudId}/decision")
    public ResponseEntity<SolicitudResueltaResponseDTO> resolver(
            Authentication authentication,
            @PathVariable UUID solicitudId,
            @Valid @RequestBody DecisionSolicitudRequestDTO request) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(validacionAuditorService.resolver(usuarioId, solicitudId, request));
    }
}
