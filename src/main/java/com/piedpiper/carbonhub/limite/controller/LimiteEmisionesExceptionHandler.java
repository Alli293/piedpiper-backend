package com.piedpiper.carbonhub.limite.controller;

import com.piedpiper.carbonhub.common.ApiErrorDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LimiteEmisionesExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(Exception exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrorDTO.of(
                HttpStatus.FORBIDDEN.value(),
                "No tiene permiso para modificar el limite de la empresa."
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorDTO.of(
                HttpStatus.CONFLICT.value(),
                "Conflicto al guardar el limite. Intente nuevamente."
        ));
    }
}
