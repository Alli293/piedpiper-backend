package com.piedpiper.carbonhub.exceptions;

import com.piedpiper.carbonhub.common.ApiErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorDTO.of(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiErrorDTO.of(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorDTO.of(HttpStatus.FORBIDDEN.value(), "No tiene permisos para realizar esta acción."));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorDTO> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiErrorDTO.of(HttpStatus.BAD_REQUEST.value(),
                "La solicitud contiene datos inválidos o incompletos."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorDTO> handleUploadSize(MaxUploadSizeExceededException ex) {
        ApiException error = ApiException.documentoRespaldoExcedeTamanio();
        return ResponseEntity.status(error.getStatus())
                .body(ApiErrorDTO.of(error.getStatus().value(), error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex) {
        log.error("Error inesperado no manejado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorDTO.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Ocurrió un error inesperado. Por favor, intenta nuevamente."));
    }
}
