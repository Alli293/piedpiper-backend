package com.piedpiper.carbonhub.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAXIMO_IPS = 10_000;
    private static final String CUBETA_EXCESO = "__overflow__";

    private final Map<String, Deque<Long>> intentosPorIp = new ConcurrentHashMap<>();
    private final int maxIntentos;
    private final long ventanaMs;

    public LoginRateLimitFilter(
            @Value("${auth.login.rate-limit.max-requests:20}") int maxIntentos,
            @Value("${auth.login.rate-limit.window-ms:60000}") long ventanaMs) {
        this.maxIntentos = maxIntentos;
        this.ventanaMs = ventanaMs;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !LOGIN_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ipRemota = request.getRemoteAddr();
        String ip = intentosPorIp.containsKey(ipRemota) || intentosPorIp.size() < MAXIMO_IPS
                ? ipRemota : CUBETA_EXCESO;
        Deque<Long> intentos = intentosPorIp.computeIfAbsent(ip, ignored -> new ArrayDeque<>());
        long ahora = System.currentTimeMillis();

        synchronized (intentos) {
            long inicio = ahora - ventanaMs;
            while (!intentos.isEmpty() && intentos.peekFirst() < inicio) {
                intentos.removeFirst();
            }
            if (intentos.size() >= maxIntentos) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"mensaje\":\"Demasiados intentos de inicio de sesion.\"}");
                return;
            }
            intentos.addLast(ahora);
        }

        filterChain.doFilter(request, response);
    }
}
