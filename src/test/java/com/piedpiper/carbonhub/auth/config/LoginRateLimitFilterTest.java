package com.piedpiper.carbonhub.auth.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LoginRateLimitFilterTest {

    @Test
    void bloqueaLaPeticionQueSuperaLaCuotaPorIp() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(2, 60_000);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = login("192.0.2.10");
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilterInternal(login("192.0.2.10"), bloqueada, chain);

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentAsString()).contains("Demasiados intentos");
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoraXForwardedForControladoPorElCliente() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter(1, 60_000);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest primera = login("192.0.2.20");
        primera.addHeader("X-Forwarded-For", "203.0.113.1");
        filter.doFilterInternal(primera, new MockHttpServletResponse(), chain);

        MockHttpServletRequest segunda = login("192.0.2.20");
        segunda.addHeader("X-Forwarded-For", "203.0.113.2");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filter.doFilterInternal(segunda, respuesta, chain);

        assertThat(respuesta.getStatus()).isEqualTo(429);
    }

    private MockHttpServletRequest login(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }
}
