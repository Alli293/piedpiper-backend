package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.FiltrarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.service.AuditorDirectorioService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditores")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL', 'AUDITOR_CERTIFICADO')")
public class AuditorController {

    private final AuditorDirectorioService auditorDirectorioService;

    public AuditorController(AuditorDirectorioService auditorDirectorioService) {
        this.auditorDirectorioService = auditorDirectorioService;
    }

    @GetMapping
    public ResponseEntity<PaginaAuditoresResponseDTO> listar(
            @ModelAttribute FiltrarAuditoresRequestDTO filtros) {
        return ResponseEntity.ok(auditorDirectorioService.listar(filtros));
    }
}
