package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.FiltrarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.service.AuditorDirectorioService;
import com.piedpiper.carbonhub.auditor.service.PerfilPublicoAuditorService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditores")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL', 'AUDITOR_CERTIFICADO')")
public class AuditorController {

    private final AuditorDirectorioService auditorDirectorioService;
    private final PerfilPublicoAuditorService perfilPublicoAuditorService;

    public AuditorController(AuditorDirectorioService auditorDirectorioService,
                             PerfilPublicoAuditorService perfilPublicoAuditorService) {
        this.auditorDirectorioService = auditorDirectorioService;
        this.perfilPublicoAuditorService = perfilPublicoAuditorService;
    }

    @GetMapping
    public ResponseEntity<PaginaAuditoresResponseDTO> listar(
            @ModelAttribute FiltrarAuditoresRequestDTO filtros) {
        return ResponseEntity.ok(auditorDirectorioService.listar(filtros));
    }

    @GetMapping("/{auditorId}")
    public ResponseEntity<PerfilPublicoAuditorResponseDTO> obtenerPerfilPublico(
            @PathVariable UUID auditorId) {
        return ResponseEntity.ok(perfilPublicoAuditorService.obtenerPerfilPublico(auditorId));
    }
}
