package com.piedpiper.carbonhub.ecoruta.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CertificacionApiKeyFilterTest {

    private static final String HEADER_API_KEY = "X-Certificacion-Api-Key";
    private static final String API_KEY_VALIDA = "clave-prueba";
    private static final String EVENTOS_CERTIFICACION_PATH = "/api/certificacion/eventos";

    private final CertificacionApiKeyFilter filter =
            new CertificacionApiKeyFilter(HEADER_API_KEY, API_KEY_VALIDA, EVENTOS_CERTIFICACION_PATH);

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiKeyValidaAutenticaSolicitudDeCertificacion() throws Exception {
        MockHttpServletRequest request = requestCertificacion();
        request.addHeader(HEADER_API_KEY, API_KEY_VALIDA);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filtroSiguienteInvocado = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) ->
                filtroSiguienteInvocado.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(filtroSiguienteInvocado).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .contains("ROLE_CERTIFICACION");
    }

    @Test
    void sinApiKeyDevuelve401YNoContinuaCadena() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filtroSiguienteInvocado = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) ->
                filtroSiguienteInvocado.set(true);

        filter.doFilter(requestCertificacion(), response, filterChain);

        assertThat(filtroSiguienteInvocado).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void apiKeyInvalidaDevuelve401YNoContinuaCadena() throws Exception {
        MockHttpServletRequest request = requestCertificacion();
        request.addHeader(HEADER_API_KEY, "clave-invalida");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filtroSiguienteInvocado = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) ->
                filtroSiguienteInvocado.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(filtroSiguienteInvocado).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void otraRutaNoEsFiltrada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/health");
        request.setServletPath("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filtroSiguienteInvocado = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) ->
                filtroSiguienteInvocado.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(filtroSiguienteInvocado).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void apiKeyValidaAutenticaAunqueServletPathVengaVacio() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                HttpMethod.POST.name(), "/api/certificacion/eventos");
        request.setServletPath("");
        request.addHeader(HEADER_API_KEY, API_KEY_VALIDA);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filtroSiguienteInvocado = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) ->
                filtroSiguienteInvocado.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(filtroSiguienteInvocado).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    private MockHttpServletRequest requestCertificacion() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                HttpMethod.POST.name(), "/api/certificacion/eventos");
        request.setServletPath("/api/certificacion/eventos");
        return request;
    }
}
