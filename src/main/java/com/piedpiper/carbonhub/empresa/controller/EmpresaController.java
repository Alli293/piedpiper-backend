package com.piedpiper.carbonhub.empresa.controller;

import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaRequestDTO;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.service.ConfiguracionInicialEmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final ConfiguracionInicialEmpresaService configuracionInicialEmpresaService;

    public EmpresaController(ConfiguracionInicialEmpresaService configuracionInicialEmpresaService) {
        this.configuracionInicialEmpresaService = configuracionInicialEmpresaService;
    }

    @PostMapping("/configuracion-inicial")
    public ResponseEntity<ConfiguracionInicialEmpresaResponseDTO> completarConfiguracionInicial(
            @Valid @RequestBody ConfiguracionInicialEmpresaRequestDTO request) {
        UUID usuarioId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        ConfiguracionInicialEmpresaResponseDTO response =
                configuracionInicialEmpresaService.completarConfiguracionEmpresa(usuarioId, request);
        HttpStatus status = response.isRecienCreada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
