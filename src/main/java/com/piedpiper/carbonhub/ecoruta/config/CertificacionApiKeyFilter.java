package com.piedpiper.carbonhub.ecoruta.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class CertificacionApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CertificacionApiKeyFilter.class);
    private static final String EVENTOS_CERTIFICACION_PATH = "/api/certificacion/eventos";

    private final String apiKeyHeader;
    private final String apiKey;

    public CertificacionApiKeyFilter(
            @Value("${certificacion.api-key-header:X-Certificacion-Api-Key}") String apiKeyHeader,
            @Value("${certificacion.api-key:}") String apiKey) {
        this.apiKeyHeader = apiKeyHeader;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !esEndpointEventosCertificacion(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!apiKeyConfigurada() || !claveValida(request.getHeader(apiKeyHeader))) {
            log.warn("Solicitud de evento de Certificacion rechazada por credenciales invalidas");
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "certificacion", null, List.of(new SimpleGrantedAuthority("ROLE_CERTIFICACION")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean apiKeyConfigurada() {
        return !apiKey.isBlank();
    }

    private boolean esEndpointEventosCertificacion(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (EVENTOS_CERTIFICACION_PATH.equals(servletPath)) {
            return true;
        }

        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return EVENTOS_CERTIFICACION_PATH.equals(requestUri);
    }

    private boolean claveValida(String claveRecibida) {
        if (claveRecibida == null || claveRecibida.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                claveRecibida.getBytes(StandardCharsets.UTF_8));
    }
}
