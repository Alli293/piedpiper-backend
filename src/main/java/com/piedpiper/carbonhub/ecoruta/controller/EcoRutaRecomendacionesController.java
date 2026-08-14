package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionesResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.service.RecomendacionAmbientalService;

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

/**
 * Recomendaciones ambientales para mejorar el EcoScore de un itinerario (PP-93).
 * Depende de PP-91 (EcoScore) y PP-92 (alternativas/sustitución): "aplicar" una recomendación
 * reutiliza el mismo endpoint de sustitución que PP-92, así que este controlador no expone
 * un DTO propio de aplicación.
 */
@RestController
@RequestMapping("/api/ecoruta/itinerarios")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaRecomendacionesController {

    private final RecomendacionAmbientalService service;

    public EcoRutaRecomendacionesController(RecomendacionAmbientalService service) {
        this.service = service;
    }

    @GetMapping("/{id}/recomendaciones")
    public ResponseEntity<RecomendacionesResponseDTO> obtenerRecomendaciones(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        RecomendacionesResponseDTO response = service.obtenerRecomendaciones(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/recomendaciones/{actividadId}/aplicar")
    public ResponseEntity<ItinerarioResponseDTO> aplicarRecomendacion(
            @PathVariable UUID id,
            @PathVariable UUID actividadId,
            @RequestBody @Valid SustitucionRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ItinerarioResponseDTO response = service.aplicarRecomendacion(id, actividadId, request, usuarioId);
        return ResponseEntity.ok(response);
    }
}
