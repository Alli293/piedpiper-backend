package com.piedpiper.carbonhub.limite.controller;

import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.service.LimiteEmisionesService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/limites")
@PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR_EMPRESA')")
public class LimiteEmisionesController {
    private final LimiteEmisionesService service;

    public LimiteEmisionesController(LimiteEmisionesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LimiteEmisionesResponseDTO>> listarLimites(
            @RequestHeader("X-Empresa-Id") Long empresaId
    ) {
        return ResponseEntity.ok(service.listarLimites(empresaId));
    }

    @GetMapping("/{anio}")
    public ResponseEntity<LimiteEmisionesResponseDTO> obtenerLimite(
            @RequestHeader("X-Empresa-Id") Long empresaId,
            @PathVariable Integer anio
    ) {
        return service.obtenerLimite(empresaId, anio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LimiteEmisionesResponseDTO> crearOActualizarLimite(
            @RequestHeader("X-Empresa-Id") Long empresaId,
            @Valid @RequestBody LimiteEmisionesRequestDTO request
    ) {
        return ResponseEntity.ok(service.guardarLimite(empresaId, request));
    }

    @DeleteMapping("/{anio}")
    public ResponseEntity<Void> eliminarLimite(
            @RequestHeader("X-Empresa-Id") Long empresaId,
            @PathVariable Integer anio
    ) {
        service.eliminarLimite(empresaId, anio);
        return ResponseEntity.noContent().build();
    }
}
