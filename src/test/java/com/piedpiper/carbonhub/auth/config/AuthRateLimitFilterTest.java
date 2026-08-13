package com.piedpiper.carbonhub.auth.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private static AuthRateLimitFilter filtro(int maxLogin, int maxRegistro, int maxReset) {
        return new AuthRateLimitFilter(new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()),
                maxLogin, 60_000, maxRegistro, 60_000, maxReset, 60_000);
    }

    @Test
    void bloqueaLaPeticionQueSuperaLaCuotaPorIp() throws Exception {
        AuthRateLimitFilter filter = filtro(2, 10, 10);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.10"),
                    new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.10"), bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentAsString()).contains("Demasiados intentos");
        verify(chain, times(2)).doFilter(any(), any());
    }

    /**
     * El filtro no debe leer nunca X-Forwarded-For: la escribe el cliente, asi que confiar en ella
     * seria regalar una cubeta nueva por cada valor inventado. Traducirla a la IP real es tarea de
     * Tomcat, que solo lo hace para proxies de confianza.
     */
    @Test
    void ignoraXForwardedForControladoPorElCliente() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 10, 10);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest primera = peticion("/api/auth/login", "192.0.2.20");
        primera.addHeader("X-Forwarded-For", "203.0.113.1");
        filter.doFilterInternal(primera, new MockHttpServletResponse(), chain);

        MockHttpServletRequest segunda = peticion("/api/auth/login", "192.0.2.20");
        segunda.addHeader("X-Forwarded-For", "203.0.113.2");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filter.doFilterInternal(segunda, respuesta, chain);

        assertThat(respuesta.getStatus()).isEqualTo(429);
    }

    @Test
    void ipsDistintasNoSePisanLaCuota() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 10, 10);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.30"),
                new MockHttpServletResponse(), chain);
        MockHttpServletResponse otra = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.31"), otra, chain);

        assertThat(otra.getStatus()).isEqualTo(200);
        verify(chain, times(2)).doFilter(any(), any());
    }

    /** Sin limite, la diferencia entre 409 y 201 del registro es un listado de cuentas. */
    @Test
    void elRegistroTambienTieneCuota() throws Exception {
        AuthRateLimitFilter filter = filtro(20, 2, 10);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            filter.doFilterInternal(peticion("/api/auth/registro/empresa/correo", "192.0.2.40"),
                    new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/registro/empresa/correo", "192.0.2.40"),
                bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentAsString()).contains("Demasiados intentos de registro");
    }

    /** Se reconoce por prefijo para que un endpoint de registro nuevo nazca limitado. */
    @Test
    void lasVariantesDeRegistroCompartenCuota() throws Exception {
        AuthRateLimitFilter filter = filtro(20, 2, 10);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(peticion("/api/auth/registro/empresa/correo", "192.0.2.50"),
                new MockHttpServletResponse(), chain);
        filter.doFilterInternal(peticion("/api/auth/registro/auditor", "192.0.2.50"),
                new MockHttpServletResponse(), chain);

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/registro/usuario/correo", "192.0.2.50"),
                bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
    }

    @Test
    void solicitarResetTambienTieneCuota() throws Exception {
        AuthRateLimitFilter filter = filtro(20, 10, 1);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(peticion("/api/auth/solicitar-reset-contrasena", "192.0.2.60"),
                new MockHttpServletResponse(), chain);
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/solicitar-reset-contrasena", "192.0.2.60"),
                bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
    }

    /** Gastar los intentos de registro no puede dejar a nadie sin poder iniciar sesion. */
    @Test
    void cadaGrupoLlevaSuPropiaCuenta() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 1, 1);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(peticion("/api/auth/registro/empresa/correo", "192.0.2.70"),
                new MockHttpServletResponse(), chain);
        MockHttpServletResponse registroBloqueado = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/registro/empresa/correo", "192.0.2.70"),
                registroBloqueado, chain);

        MockHttpServletResponse login = new MockHttpServletResponse();
        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.70"), login, chain);

        assertThat(registroBloqueado.getStatus()).isEqualTo(429);
        assertThat(login.getStatus()).isEqualTo(200);
    }

    @Test
    void elGetNoConsumeCuota() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 10, 10);
        MockHttpServletRequest get = peticion("/api/auth/login", "192.0.2.80");
        get.setMethod("GET");

        assertThat(filter.shouldNotFilter(get)).isTrue();
    }

    @Test
    void unEndpointDeAutenticacionSinCuotaPasaDeLargo() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 1, 1);

        assertThat(filter.shouldNotFilter(peticion("/api/auth/restablecer-contrasena", "192.0.2.90")))
                .isTrue();
    }

    /** El cuerpo tiene que hablar el mismo idioma que el resto de la API o el frontend no lo lee. */
    @Test
    void elCuerpoDel429UsaElFormatoDeErrorDeLaApi() throws Exception {
        AuthRateLimitFilter filter = filtro(0, 10, 10);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.100"), respuesta,
                mock(FilterChain.class));

        assertThat(respuesta.getStatus()).isEqualTo(429);
        assertThat(respuesta.getContentAsString()).contains("\"status\":429").contains("\"message\"");
        assertThat(respuesta.getContentAsString()).doesNotContain("\"mensaje\"");
        assertThat(respuesta.getHeader("Retry-After")).isEqualTo("60");
    }

    /**
     * Con context path {@code getServletPath()} viene vacio. Si el filtro solo mirara ahi, la ruta
     * no coincidiria y no limitaria nada.
     */
    @Test
    void reconoceLaRutaAunConContextPath() throws Exception {
        AuthRateLimitFilter filter = filtro(1, 10, 10);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest primera = conContextPath("/carbonhub", "/api/auth/login", "192.0.2.110");
        filter.doFilterInternal(primera, new MockHttpServletResponse(), chain);

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(conContextPath("/carbonhub", "/api/auth/login", "192.0.2.110"),
                bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void laPeticionBloqueadaNoLlegaAlControlador() throws Exception {
        AuthRateLimitFilter filter = filtro(0, 10, 10);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(peticion("/api/auth/login", "192.0.2.120"),
                new MockHttpServletResponse(), chain);

        verify(chain, never()).doFilter(any(), any());
    }

    private static MockHttpServletRequest peticion(String ruta, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        return request;
    }

    private static MockHttpServletRequest conContextPath(String contextPath, String ruta, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", contextPath + ruta);
        request.setContextPath(contextPath);
        request.setServletPath("");
        request.setRequestURI(contextPath + ruta);
        request.setRemoteAddr(ip);
        return request;
    }
}
