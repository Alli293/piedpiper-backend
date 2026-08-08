package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.exceptions.SlugCambiadoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoErrorDTO;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler de excepciones con scope limitado al {@link PerfilPublicoController}.
 * Retorna siempre {@link PerfilPublicoErrorDTO} con un solo campo "mensaje",
 * sin exponer datos internos (trazas, tablas, IPs).
 *
 * El @Order(1) garantiza que este handler tiene prioridad sobre el
 * GlobalExceptionHandler para las excepciones del perfil público.
 */
@RestControllerAdvice(assignableTypes = PerfilPublicoController.class)
@Order(1)
public class PerfilPublicoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PerfilPublicoExceptionHandler.class);

    @ExceptionHandler(SlugCambiadoException.class)
    public ResponseEntity<Void> handleSlugCambiado(SlugCambiadoException ex,
                                                   HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Reemplazar el slug antiguo en el path con el slug vigente
        String nuevaUrl = uri.replaceFirst(
                "(/api/perfil-publico/)[^/]+", "$1" + ex.getSlugVigente());
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, nuevaUrl)
                .build();
    }

    @ExceptionHandler(PerfilNoEncontradoException.class)
    public ResponseEntity<PerfilPublicoErrorDTO> handleNoEncontrado(PerfilNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PerfilPublicoErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<PerfilPublicoErrorDTO> handleDataAccess(DataAccessException ex) {
        log.error("Error de acceso a datos al consultar perfil público", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PerfilPublicoErrorDTO(
                        "No fue posible cargar el perfil en este momento. Intenta nuevamente más tarde."));
    }
}
