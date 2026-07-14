package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarElectricidadRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarEnvioRequestDTO;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;
import com.piedpiper.carbonhub.emision.service.EmisionEnvioService;
import com.piedpiper.carbonhub.exceptions.ApiException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final EmisionEnvioService emisionEnvioService;

    public EmisionController(EmisionElectricidadService emisionElectricidadService,
                             EmisionEnvioService emisionEnvioService) {
        this.emisionElectricidadService = emisionElectricidadService;
        this.emisionEnvioService = emisionEnvioService;
    }

    @PostMapping("/electricidad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
    public ResponseEntity<EmisionResponseDTO> registrarElectricidad(
            @Valid @RequestBody RegistrarElectricidadRequestDTO request) {
        UUID usuarioId = usuarioIdAutenticado();
        EmisionResponseDTO response = emisionElectricidadService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/envio")
    @PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
    public ResponseEntity<EmisionEnvioResponseDTO> registrarEnvio(
            @Valid @RequestBody RegistrarEnvioRequestDTO request) {
        UUID usuarioId = usuarioIdAutenticado();
        EmisionEnvioResponseDTO response = emisionEnvioService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID usuarioIdAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw ApiException.errorInterno("No se pudo identificar al usuario autenticado.");
        }
    }
}
