package com.piedpiper.carbonhub.perfilpublico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del filtro de rate limiting para el endpoint público
 * de perfil ({@code /api/perfil-publico/**}).
 *
 * Se instancia el filtro directamente sin contexto Spring para evitar overhead.
 */
class PerfilPublicoRateLimitFilterTest {

    private PerfilPublicoRateLimitFilter filter;
    private FilterChain filterChain;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new PerfilPublicoRateLimitFilter(objectMapper);
        // Configurar valores por defecto via reflection (simula @Value)
        ReflectionTestUtils.setField(filter, "maxRequests", 60);
        ReflectionTestUtils.setField(filter, "windowMs", 60000L);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void peticionesDentroDelLimiteRetornan200() throws ServletException, IOException {
        MockHttpServletRequest request = createRequest("/api/perfil-publico/empresa-verde", "192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Realizar una petición (dentro del límite de 60)
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        // El filtro no establece status; el filterChain lo haría → status por defecto 200
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void excederLimiteRetorna429ConCuerpoJsonCorrecto() throws ServletException, IOException {
        String clientIp = "10.0.0.1";

        // Realizar 60 peticiones (el límite)
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = createRequest("/api/perfil-publico/empresa-verde", clientIp);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
        }

        // La petición 61 debe ser rechazada con 429
        MockHttpServletRequest request = createRequest("/api/perfil-publico/empresa-verde", clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).startsWith("application/json");

        String body = response.getContentAsString();
        assertThat(body).contains("Demasiadas solicitudes. Intenta nuevamente en unos minutos.");

        // Verificar estructura JSON correcta
        var jsonNode = objectMapper.readTree(body);
        assertThat(jsonNode.has("mensaje")).isTrue();
        assertThat(jsonNode.get("mensaje").asText())
                .isEqualTo("Demasiadas solicitudes. Intenta nuevamente en unos minutos.");
    }

    @Test
    void diferentesIPsTienenLimitesIndependientes() throws ServletException, IOException {
        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.200";

        // Agotar el límite para ip1
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = createRequest("/api/perfil-publico/empresa-verde", ip1);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
        }

        // ip1 debe estar bloqueada (petición 61)
        MockHttpServletRequest blockedRequest = createRequest("/api/perfil-publico/empresa-verde", ip1);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(blockedRequest, blockedResponse, filterChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        // ip2 debe pasar normalmente (primera petición)
        MockHttpServletRequest allowedRequest = createRequest("/api/perfil-publico/empresa-verde", ip2);
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(allowedRequest, allowedResponse, filterChain);

        verify(filterChain, times(61)).doFilter(any(), any()); // 60 de ip1 + 1 de ip2
        assertThat(allowedResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void xForwardedForFalsificadoNoPermiteEvadirElLimite() throws ServletException, IOException {

        // Agotar el límite usando X-Forwarded-For
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = createRequest("/api/perfil-publico/empresa-verde", "127.0.0.1");
            req.addHeader("X-Forwarded-For", "203.0.113." + i);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(req, resp, filterChain);
        }

        // Petición 61 desde la misma IP real (via X-Forwarded-For) → 429
        MockHttpServletRequest blockedReq = createRequest("/api/perfil-publico/empresa-verde", "127.0.0.1");
        blockedReq.addHeader("X-Forwarded-For", "198.51.100.250");
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(blockedReq, blockedResp, filterChain);

        assertThat(blockedResp.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        // Una IP diferente (sin X-Forwarded-For) aún puede pasar
        MockHttpServletRequest otherReq = createRequest("/api/perfil-publico/empresa-verde", "192.168.0.1");
        MockHttpServletResponse otherResp = new MockHttpServletResponse();
        filter.doFilterInternal(otherReq, otherResp, filterChain);

        assertThat(otherResp.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Crea un MockHttpServletRequest para la ruta y IP dadas.
     */
    private MockHttpServletRequest createRequest(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
