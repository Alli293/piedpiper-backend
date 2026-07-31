package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.service.EmisionConsultaService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta, listado y borrado de emisiones ya registradas. */
@RestController
@RequestMapping("/api/emisiones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class EmisionConsultaController {

    private final EmisionConsultaService emisionConsultaService;

    public EmisionConsultaController(EmisionConsultaService emisionConsultaService) {
        this.emisionConsultaService = emisionConsultaService;
    }

    @GetMapping
    public ResponseEntity<List<EmisionResponseDTO>> listar(
            Authentication authentication,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(emisionConsultaService.listar(
                Autenticaciones.usuarioId(authentication),
                normalizarCategoria(categoria),
                anio,
                mes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmisionResponseDTO> obtener(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(emisionConsultaService.obtener(id, Autenticaciones.usuarioId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID id) {
        emisionConsultaService.eliminar(id, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.noContent().build();
    }

    private CategoriaEmision normalizarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank() || "TODAS".equalsIgnoreCase(categoria)) {
            return null;
        }
        try {
            return CategoriaEmision.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.categoriaEmisionInvalida();
        }
    }
}
