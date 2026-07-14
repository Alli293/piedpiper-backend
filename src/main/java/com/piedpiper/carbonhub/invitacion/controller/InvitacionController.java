package com.piedpiper.carbonhub.invitacion.controller;

import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionRequestDTO;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionResponseDTO;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas/invitaciones")
public class InvitacionController {

    private final InvitacionService invitacionService;

    public InvitacionController(InvitacionService invitacionService) {
        this.invitacionService = invitacionService;
    }

    @PostMapping
    public ResponseEntity<InvitacionResponseDTO> emitir(
            Authentication authentication,
            @Valid @RequestBody InvitacionRequestDTO request) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        InvitacionResponseDTO response = invitacionService.emitir(usuarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvitacionResponseDTO>> listar(Authentication authentication) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(invitacionService.listar(usuarioId));
    }

    @PostMapping("/{invitacionId}/revocar")
    public ResponseEntity<InvitacionResponseDTO> revocar(
            Authentication authentication,
            @PathVariable UUID invitacionId) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(invitacionService.revocar(usuarioId, invitacionId));
    }
}
