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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionInicialEmpresaService.completarPaso2(usuarioId, request));
    }

    // PENDIENTE: conexion de subida de documentos (Paso 3 del wizard,
    // cedula juridica + personeria juridica en PDF). Almacenamiento local
    // por ahora (decision: no usar S3 todavia). Implementar cuando se
    // resuelva el mecanismo de storage.
    @PostMapping("/configuracion-inicial/documentos")
    public ResponseEntity<Void> subirDocumentos() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
