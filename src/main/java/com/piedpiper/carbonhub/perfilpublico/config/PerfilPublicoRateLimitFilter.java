package com.piedpiper.carbonhub.perfilpublico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoErrorDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Filtro de rate limiting para el endpoint publico de perfil
 * ({@code /api/perfil-publico/**}).
 *
 * <p>Limita la cantidad de peticiones por direccion IP usando una ventana
 * deslizante (sliding window) de 1 minuto. Si se supera el umbral configurado,
 * retorna HTTP 429 con un {@link PerfilPublicoErrorDTO}.
 *
 * <p>Implementacion liviana con {@link ConcurrentHashMap} para evitar dependencias
 * externas. La limpieza de entradas expiradas se realiza de forma lazy en cada
 * acceso y periodicamente mediante un hilo daemon de bajo impacto.
 */
@Component
public class PerfilPublicoRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PerfilPublicoRateLimitFilter.class);
    private static final String PERFIL_PUBLICO_PATH_PREFIX = "/api/perfil-publico/";
    private static final String RATE_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente en unos minutos.";
    private static final int MAXIMO_IPS = 10_000;
    private static final String CUBETA_EXCESO = "__overflow__";

    private final Map<String, Deque<Long>> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * Maximo de peticiones permitidas por IP dentro de la ventana de tiempo.
     */
    @Value("${perfilpublico.rate-limit.max-requests:60}")
    private int maxRequests;

    /**
     * Ventana de tiempo en milisegundos (por defecto 60 segundos = 1 minuto).
     */
    @Value("${perfilpublico.rate-limit.window-ms:60000}")
    private long windowMs;

    public PerfilPublicoRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        startCleanupDaemon();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolvePath(request);
        return !path.startsWith(PERFIL_PUBLICO_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String remoteIp = resolveClientIp(request);
        String clientIp = requestCounts.containsKey(remoteIp) || requestCounts.size() < MAXIMO_IPS
                ? remoteIp : CUBETA_EXCESO;
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestCounts.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        boolean permitido = reservar(timestamps, now);
        if (!permitido) {
            log.warn("Rate limit excedido para IP: {} en ruta: {}", clientIp, resolvePath(request));
            writeRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean reservar(Deque<Long> timestamps, long now) {
        synchronized (timestamps) {
            long windowStart = now - windowMs;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Nunca confiar directamente en X-Forwarded-For: es controlado por el cliente.
        // Si existe un proxy confiable, debe normalizar la direccion antes de llegar a la app.
        return request.getRemoteAddr();
    }

    /**
     * Resuelve la ruta de la peticion de forma robusta (igual que ContentSecurityPolicyFilter).
     */
    private String resolvePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            String contextPath = request.getContextPath();
            String requestUri = request.getRequestURI();
            if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
                path = requestUri.substring(contextPath.length());
            } else {
                path = requestUri;
            }
        }
        return path;
    }

    /**
     * Escribe la respuesta HTTP 429 con el DTO de error en formato JSON.
     */
    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        PerfilPublicoErrorDTO errorDto = new PerfilPublicoErrorDTO(RATE_LIMIT_MESSAGE);
        response.getWriter().write(objectMapper.writeValueAsString(errorDto));
    }

    /**
     * Inicia un hilo daemon que limpia entradas expiradas del mapa cada 5 minutos
     * para evitar acumulacion de memoria en escenarios con muchas IPs distintas.
     */
    private void startCleanupDaemon() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300_000); // 5 minutos
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "rate-limit-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * Elimina del mapa las IPs cuyas colas de timestamps estan completamente expiradas.
     */
    private void cleanupExpiredEntries() {
        long cutoff = System.currentTimeMillis() - windowMs;
        Iterator<Map.Entry<String, Deque<Long>>> it = requestCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> timestamps = entry.getValue();
            // Limpiar timestamps expirados
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            // Si la cola queda vacia, eliminar la entrada del mapa
            if (timestamps.isEmpty()) {
                it.remove();
            }
        }
    }
}
