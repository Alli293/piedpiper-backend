package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ComparacionResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.service.ComparacionAlternativasService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ecoruta/itinerarios")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaAlternativasController {

    private final ComparacionAlternativasService service;

    public EcoRutaAlternativasController(ComparacionAlternativasService service) {
        this.service = service;
    }

    @GetMapping("/{id}/actividades/{actividadId}/alternativas")
    public ResponseEntity<ComparacionResponseDTO> obtenerAlternativas(
            @PathVariable UUID id,
            @PathVariable UUID actividadId,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ComparacionResponseDTO response = service.obtenerAlternativas(id, actividadId, usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/actividades/{actividadId}/sustituir")
    public ResponseEntity<ItinerarioResponseDTO> sustituirActividad(
            @PathVariable UUID id,
            @PathVariable UUID actividadId,
            @RequestBody @Valid SustitucionRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ItinerarioResponseDTO response = service.sustituirActividad(id, actividadId, request, usuarioId);
        return ResponseEntity.ok(response);
    }
}
