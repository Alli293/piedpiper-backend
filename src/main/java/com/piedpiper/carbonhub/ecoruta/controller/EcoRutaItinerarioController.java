package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * Conversación continua de refinamiento del itinerario (PP-88). A diferencia de {@link #obtener},
     * no hace un chequeo de ownership previo en el controlador: {@code service.refinar(...)} ya
     * hace su propia consulta por {@code itinerarioId + usuarioId} y devuelve el 403 con el mensaje
     * correcto — agregar una verificación acá sería una segunda consulta idéntica para la misma
     * comprobación, y CONVENTIONS.md §3.7 pide no meter esa lógica en el controlador.
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

    /**
     * Misma validación que {@link #verificarPropiedadItinerario}, con el mensaje específico de
     * "modificar" en vez de "acceder". Ya no la usa {@link #refinar} (esa ownership vive ahora en
     * {@code EcoRutaItinerarioService.refinar}, una sola consulta en vez de dos) — queda acá porque
     * la rama de PP-89 (que arranca desde esta y ya está abierta como PR dependiente) la reutiliza
     * para el endpoint de eliminar itinerario.
     */
    private void verificarPropiedadItinerarioParaModificar(UUID itinerarioId, UUID usuarioId) {
        if (!service.perteneceAlUsuario(itinerarioId, usuarioId)) {
            log.warn("Intento de modificar itinerario {} por usuario {}: no es el propietario", itinerarioId, usuarioId);
            throw ApiException.accesoDenegado("No tienes permiso para modificar este itinerario.");
        }
    }
}
