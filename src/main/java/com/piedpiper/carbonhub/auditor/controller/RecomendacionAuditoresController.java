package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.RecomendacionAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador aparte de {@link AuditorController} a propósito: aquel abre el directorio a tres
 * roles, y la recomendación es solo del administrador de empresa, porque se apoya en el sector de
 * su empresa. Anidar un {@code @PreAuthorize} más estrecho dentro de la clase existente escondería
 * esa diferencia.
 */
@RestController
@RequestMapping("/api/auditores")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class RecomendacionAuditoresController {

    private final RecomendacionAuditoresService recomendacionAuditoresService;

    public RecomendacionAuditoresController(RecomendacionAuditoresService recomendacionAuditoresService) {
        this.recomendacionAuditoresService = recomendacionAuditoresService;
    }

    @PostMapping("/recomendaciones")
    public ResponseEntity<RecomendacionAuditoresResponseDTO> recomendar(
            @Valid @RequestBody RecomendarAuditoresRequestDTO filtros,
            Authentication authentication) {
        return ResponseEntity.ok(recomendacionAuditoresService.recomendar(
                filtros, Autenticaciones.usuarioId(authentication)));
    }
}
