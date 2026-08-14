package com.piedpiper.carbonhub.perfilpublico.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que agrega la cabecera {@code Content-Security-Policy} a todas las
 * respuestas del endpoint publico de perfil ({@code /api/perfil-publico/**}).
 *
 * <p>La politica es restrictiva: {@code default-src 'none'; frame-ancestors 'none'}
 * porque el endpoint es una API REST que no sirve HTML ni carga recursos propios.
 * Esto previene que las respuestas se incrusten en iframes y bloquea cualquier
 * carga de recursos si el contenido se interpreta como HTML por error.
 */
@Component
public class ContentSecurityPolicyFilter extends OncePerRequestFilter {

    private static final String CSP_HEADER = "Content-Security-Policy";
    private static final String CSP_POLICY = "default-src 'none'; frame-ancestors 'none'";
    private static final String PERFIL_PUBLICO_PATH_PREFIX = "/api/perfil-publico/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            // Fallback: strip context path from requestURI
            String contextPath = request.getContextPath();
            String requestUri = request.getRequestURI();
            if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
                path = requestUri.substring(contextPath.length());
            } else {
                path = requestUri;
            }
        }
        return !path.startsWith(PERFIL_PUBLICO_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        response.setHeader(CSP_HEADER, CSP_POLICY);
    }
}
