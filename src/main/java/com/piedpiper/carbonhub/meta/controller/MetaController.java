package com.piedpiper.carbonhub.meta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.meta.models.dtos.CrearMetaRequestDTO;
import com.piedpiper.carbonhub.meta.models.dtos.MetaResponseDTO;
import com.piedpiper.carbonhub.meta.service.MetaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metas")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    @PostMapping
    public ResponseEntity<MetaResponseDTO> crear(
            Authentication authentication,
            @Valid @RequestBody CrearMetaRequestDTO request) {
        MetaResponseDTO creada = metaService.crear(Autenticaciones.usuarioId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<MetaResponseDTO>> listar(
            Authentication authentication,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(
                metaService.listar(Autenticaciones.usuarioId(authentication), periodo, anio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaResponseDTO> actualizar(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CrearMetaRequestDTO request) {
        MetaResponseDTO actualizada = metaService.actualizar(Autenticaciones.usuarioId(authentication), id, request);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID id) {
        metaService.eliminar(Autenticaciones.usuarioId(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
