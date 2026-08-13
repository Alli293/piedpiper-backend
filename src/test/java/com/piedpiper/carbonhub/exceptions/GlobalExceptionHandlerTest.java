package com.piedpiper.carbonhub.exceptions;

import com.piedpiper.carbonhub.common.ApiErrorDTO;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los cuatro casos de aqui son errores del cliente que antes caian en el handler generico: el
 * servidor respondia 500 y escribia una traza completa. Con eso, quien escanea endpoints genera una
 * traza por intento, inunda el log y entierra los errores que si importan.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unaRutaInexistenteEs404YNo500() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleRutaNoEncontrada(
                new NoResourceFoundException(HttpMethod.GET, "api/no-existe"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getStatus()).isEqualTo(404);
    }

    /** El mensaje va al usuario final: nada de nombres de clase ni rutas internas. */
    @Test
    void elCuerpoDel404NoFiltraDetalleInterno() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleRutaNoEncontrada(
                new NoResourceFoundException(HttpMethod.GET, "api/auditorias/secreto"));

        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getMessage())
                .isEqualTo("El recurso solicitado no existe.")
                .doesNotContain("auditorias")
                .doesNotContain("NoResourceFound");
    }

    @Test
    void unMetodoNoSoportadoEs405YDeclaraLosPermitidos() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleMetodoNoSoportado(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST")));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getHeaders().getAllow())
                .containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    }

    /**
     * Spring deja {@code getSupportedHttpMethods()} en nulo cuando no sabe cuales son. Construir el
     * header igual reventaria dentro del propio manejador de errores, que es el peor lugar posible.
     */
    @Test
    void unMetodoNoSoportadoSinListaDePermitidosNoRevienta() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleMetodoNoSoportado(
                new HttpRequestMethodNotSupportedException("TRACE"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getHeaders().get(HttpHeaders.ALLOW)).isNull();
    }

    @Test
    void unContentTypeNoSoportadoEs415() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleTipoNoSoportado(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN,
                        List.of(MediaType.APPLICATION_JSON)));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void unAcceptImposibleEs406() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleTipoNoAceptable(
                new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON)));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * El 500 sigue existiendo para lo que si es culpa del servidor. Si este dejara de responder 500
     * los errores de verdad pasarian desapercibidos, que es el problema contrario.
     */
    @Test
    void unErrorRealDelServidorSigueSiendo500() {
        ResponseEntity<ApiErrorDTO> respuesta = handler.handleGeneric(
                new IllegalStateException("la conexion a la base se cayo"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getMessage()).doesNotContain("conexion a la base");
    }
}
