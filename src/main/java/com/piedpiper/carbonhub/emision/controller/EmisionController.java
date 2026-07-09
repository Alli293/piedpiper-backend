package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarElectricidadRequestDTO;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/emisiones")
public class EmisionController {

    private final EmisionElectricidadService emisionElectricidadService;

    public EmisionController(EmisionElectricidadService emisionElectricidadService) {
        this.emisionElectricidadService = emisionElectricidadService;
    }

    @PostMapping("/electricidad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
    public ResponseEntity<EmisionResponseDTO> registrarElectricidad(
            @Valid @RequestBody RegistrarElectricidadRequestDTO request) {
        UUID usuarioId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        EmisionResponseDTO response = emisionElectricidadService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
