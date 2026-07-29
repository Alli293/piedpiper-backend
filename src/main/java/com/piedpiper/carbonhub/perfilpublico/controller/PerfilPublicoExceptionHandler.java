package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoErrorDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler de excepciones con scope limitado al {@link PerfilPublicoController}.
 * Retorna siempre {@link PerfilPublicoErrorDTO} con un solo campo "mensaje",
 * sin exponer datos internos (trazas, tablas, IPs).
 */
@RestControllerAdvice(assignableTypes = PerfilPublicoController.class)
public class PerfilPublicoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PerfilPublicoExceptionHandler.class);

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
