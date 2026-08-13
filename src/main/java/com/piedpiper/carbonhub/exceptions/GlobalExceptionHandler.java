package com.piedpiper.carbonhub.exceptions;

import com.piedpiper.carbonhub.common.ApiErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;
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
            MissingPathVariableException.class,
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

    /**
     * Una ruta que no existe es un error del cliente, no del servidor. Sin este handler cae en
     * {@link #handleGeneric} y responde 500 escribiendo la traza completa: quien escanea endpoints
     * genera una traza por intento, inunda el log y entierra los errores que si importan.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleRutaNoEncontrada(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorDTO.of(HttpStatus.NOT_FOUND.value(), "El recurso solicitado no existe."));
    }

    /** El 405 debe declarar los metodos validos: lo exige HTTP y sin eso el cliente no puede corregir. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        Set<HttpMethod> permitidos = ex.getSupportedHttpMethods();
        if (permitidos != null && !permitidos.isEmpty()) {
            respuesta.allow(permitidos.toArray(new HttpMethod[0]));
        }
        return respuesta.body(ApiErrorDTO.of(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "El método usado no está permitido para este recurso."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> handleTipoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorDTO.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                        "El formato enviado no es compatible con este recurso."));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorDTO> handleTipoNoAceptable(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiErrorDTO.of(HttpStatus.NOT_ACCEPTABLE.value(),
                        "No es posible responder en el formato solicitado."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex) {
        log.error("Error inesperado no manejado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorDTO.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Ocurrió un error inesperado. Por favor, intenta nuevamente."));
    }
}
