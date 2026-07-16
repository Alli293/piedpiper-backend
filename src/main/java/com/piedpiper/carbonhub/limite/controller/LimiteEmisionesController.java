package com.piedpiper.carbonhub.limite.controller;

import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.service.EmpresaAutenticadaService;
import com.piedpiper.carbonhub.limite.service.LimiteEmisionesService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/limites")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class LimiteEmisionesController {
    private final LimiteEmisionesService service;
    private final EmpresaAutenticadaService empresaAutenticadaService;

    public LimiteEmisionesController(
            LimiteEmisionesService service,
            EmpresaAutenticadaService empresaAutenticadaService
    ) {
        this.service = service;
        this.empresaAutenticadaService = empresaAutenticadaService;
    }

    @GetMapping
    public ResponseEntity<List<LimiteEmisionesResponseDTO>> listarLimites(
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.listarLimites(empresaId(authentication)));
    }

    @GetMapping("/{anio}")
    public ResponseEntity<LimiteEmisionesResponseDTO> obtenerLimite(
            @PathVariable Integer anio,
            Authentication authentication
    ) {
        return service.obtenerLimite(empresaId(authentication), anio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LimiteEmisionesResponseDTO> crearOActualizarLimite(
            @Valid @RequestBody LimiteEmisionesRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.guardarLimite(empresaId(authentication), request));
    }

    @DeleteMapping("/{anio}")
    public ResponseEntity<Void> eliminarLimite(
            @PathVariable Integer anio,
            Authentication authentication
    ) {
        service.eliminarLimite(empresaId(authentication), anio);
        return ResponseEntity.noContent().build();
    }

    private UUID empresaId(Authentication authentication) {
        return empresaAutenticadaService.obtenerEmpresaId(authentication);
    }
}
