package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ActualizarFavoritoItinerarioRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.FiltrarItinerariosRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioFavoritoResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PaginaItinerariosResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaItinerarioService;
import com.piedpiper.carbonhub.exceptions.ApiException;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ecoruta/itinerarios")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EcoRutaItinerarioController {

    private static final Logger log = LoggerFactory.getLogger(EcoRutaItinerarioController.class);

    private final EcoRutaItinerarioService service;

    public EcoRutaItinerarioController(EcoRutaItinerarioService service) {
        this.service = service;
    }

    @PostMapping("/generar")
    public ResponseEntity<ItinerarioResponseDTO> generar(Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ItinerarioResponseDTO response = service.generar(usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItinerarioResponseDTO> obtener(@PathVariable UUID id, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        verificarPropiedadItinerario(id, usuarioId);
        return ResponseEntity.ok(service.obtener(id, usuarioId));
    }

    /** Listado paginado de "Mis itinerarios" (PP-89). */
    @GetMapping
    public ResponseEntity<PaginaItinerariosResponseDTO> listar(
            @ModelAttribute FiltrarItinerariosRequestDTO filtros, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(service.listar(usuarioId, filtros));
    }

    @PutMapping("/{id}/favorito")
    public ResponseEntity<ItinerarioFavoritoResponseDTO> actualizarFavorito(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarFavoritoItinerarioRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(service.actualizarFavorito(id, usuarioId, request.getFavorito()));
    }

    /**
     * Eliminar itinerario (PP-89) — fuera del AC de la historia, pedido explícito del equipo. La
     * propiedad se resuelve dentro de {@link EcoRutaItinerarioService#eliminar}, con una sola
     * consulta — sin chequeo de ownership acá, para no repetir la misma comprobación dos veces
     * (detalle señalado en revisión, mismo criterio que {@link #refinar}).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        service.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Conversación continua de refinamiento del itinerario (PP-88). La propiedad se resuelve
     * dentro de {@link EcoRutaItinerarioService#refinar}, con una sola consulta — sin chequeo de
     * ownership acá, para no repetir la misma comprobación dos veces.
     */
    @PostMapping("/{id}/mensajes")
    public ResponseEntity<RefinamientoItinerarioResponseDTO> refinar(
            @PathVariable UUID id,
            @Valid @RequestBody RefinamientoItinerarioRequestDTO request,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(service.refinar(id, usuarioId, request));
    }

    /**
     * Verifica que el itinerario pertenezca al usuario autenticado antes de permitir la operación.
     * A diferencia del método {@link EcoRutaItinerarioService#obtener}, aquí se lanza 403 explícito
     * porque la validación de propiedad es un control de autorización previo a la ejecución de
     * la priorización (Req 4.3).
     */
    private void verificarPropiedadItinerario(UUID itinerarioId, UUID usuarioId) {
        if (!service.perteneceAlUsuario(itinerarioId, usuarioId)) {
            log.warn("Acceso denegado a itinerario {} por usuario {}: no es el propietario", itinerarioId, usuarioId);
            throw ApiException.accesoDenegado("No tienes permiso para acceder a este itinerario.");
        }
    }
}
