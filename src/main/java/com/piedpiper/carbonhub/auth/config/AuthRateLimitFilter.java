package com.piedpiper.carbonhub.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.common.ApiErrorDTO;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite por IP sobre los POST publicos de autenticacion.
 *
 * <p>Cubre las tres puertas que estan abiertas sin sesion y que por eso son las primeras que
 * alguien golpea:</p>
 *
 * <ul>
 *   <li><b>login</b>: fuerza bruta de contrasenas.</li>
 *   <li><b>registro</b>: el registro responde 409 si el correo ya existe y 201 si es nuevo, que es
 *       justo lo que el usuario necesita leer. Sin limite esa diferencia se convierte en un
 *       listado completo de quien esta registrado, probando correos a toda velocidad.</li>
 *   <li><b>solicitar reset</b>: el control de verdad es por cuenta y vive en
 *       {@code RestablecerContrasenaService} (3 por hora), que ademas responde igual exista o no
 *       la cuenta para no delatarla. Este limite es solo para que no se martille el endpoint.</li>
 * </ul>
 *
 * <p>El registro se reconoce por prefijo y no por rutas exactas a proposito: asi un endpoint de
 * registro nuevo nace limitado en vez de nacer descubierto.</p>
 *
 * <p><b>Quien es "una IP".</b> Se usa {@code getRemoteAddr()} y nunca se lee {@code
 * X-Forwarded-For} desde aca, porque esa cabecera la escribe el cliente y confiar en ella seria
 * regalar cubetas infinitas. Detras de un proxy o un tunel quien traduce esa cabecera en la IP
 * real es Tomcat, que solo lo hace para proxies de confianza; eso se activa con
 * {@code server.forward-headers-strategy} y va atado a escuchar solo en loopback. Sin esa
 * configuracion todas las peticiones del tunel se ven iguales y comparten cubeta, que es una
 * negacion de servicio contra uno mismo.</p>
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTRO_PREFIJO = "/api/auth/registro";
    private static final String RESET_PATH = "/api/auth/solicitar-reset-contrasena";

    private static final String MENSAJE_LOGIN = "Demasiados intentos de inicio de sesión.";
    private static final String MENSAJE_REGISTRO = "Demasiados intentos de registro.";
    private static final String MENSAJE_RESET = "Demasiadas solicitudes. Intenta de nuevo en un momento.";

    /**
     * A partir de aqui se barren las entradas vencidas antes de crear una nueva. El barrido deja el
     * mapa proporcional a las IPs realmente activas en la ventana, que con ventanas de un minuto es
     * un numero chico.
     */
    private static final int UMBRAL_BARRIDO = 5_000;

    /**
     * Ultimo recurso si tras barrer sigue sin haber espacio. Compartir cubeta degrada el limite
     * para los que caen ahi, pero es preferible a dejar de limitar: fallar abierto convertiria el
     * llenado del mapa en la forma de saltarse el control.
     */
    private static final int MAXIMO_CLAVES = 10_000;
    private static final String CUBETA_EXCESO = "__overflow__";

    private final Map<String, Deque<Long>> intentosPorClave = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final List<Cuota> cuotas;

    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${auth.login.rate-limit.max-requests:20}") int maxLogin,
            @Value("${auth.login.rate-limit.window-ms:60000}") long ventanaLogin,
            @Value("${auth.registro.rate-limit.max-requests:10}") int maxRegistro,
            @Value("${auth.registro.rate-limit.window-ms:60000}") long ventanaRegistro,
            @Value("${auth.reset.rate-limit.max-requests:10}") int maxReset,
            @Value("${auth.reset.rate-limit.window-ms:60000}") long ventanaReset) {
        this.objectMapper = objectMapper;
        this.cuotas = List.of(
                new Cuota("login", maxLogin, ventanaLogin, MENSAJE_LOGIN),
                new Cuota("registro", maxRegistro, ventanaRegistro, MENSAJE_REGISTRO),
                new Cuota("reset", maxReset, ventanaReset, MENSAJE_RESET));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return cuotaDe(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Cuota cuota = cuotaDe(request);
        if (cuota == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long ahora = System.currentTimeMillis();
        // Cada grupo lleva su propia cuenta: gastar los intentos de registro no debe dejar a nadie
        // sin poder iniciar sesion.
        String clave = cuota.grupo() + "|" + claveDe(request, ahora, cuota.ventanaMs());
        Deque<Long> intentos = intentosPorClave.computeIfAbsent(clave, ignorada -> new ArrayDeque<>());

        synchronized (intentos) {
            long inicio = ahora - cuota.ventanaMs();
            while (!intentos.isEmpty() && intentos.peekFirst() < inicio) {
                intentos.removeFirst();
            }
            if (intentos.size() >= cuota.maxIntentos()) {
                responderExceso(response, cuota);
                return;
            }
            intentos.addLast(ahora);
        }

        filterChain.doFilter(request, response);
    }

    private Cuota cuotaDe(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        String ruta = rutaDe(request);
        if (LOGIN_PATH.equals(ruta)) {
            return cuotas.get(0);
        }
        if (ruta.startsWith(REGISTRO_PREFIJO)) {
            return cuotas.get(1);
        }
        if (RESET_PATH.equals(ruta)) {
            return cuotas.get(2);
        }
        return null;
    }

    /**
     * {@code getServletPath()} viene vacio cuando la app se sirve bajo un context path, y ahi las
     * rutas dejarian de coincidir y el filtro no limitaria nada. Por eso se cae al URI sin el
     * prefijo, igual que hace {@code CertificacionApiKeyFilter}.
     */
    private static String rutaDe(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private String claveDe(HttpServletRequest request, long ahora, long ventanaMs) {
        String ip = request.getRemoteAddr();
        if (intentosPorClave.containsKey(ip) || intentosPorClave.size() < MAXIMO_CLAVES) {
            return ip;
        }
        barrerVencidas(ahora - ventanaMs);
        return intentosPorClave.size() < MAXIMO_CLAVES ? ip : CUBETA_EXCESO;
    }

    private void barrerVencidas(long inicioVentana) {
        if (intentosPorClave.size() < UMBRAL_BARRIDO) {
            return;
        }
        Iterator<Map.Entry<String, Deque<Long>>> it = intentosPorClave.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> entrada = it.next();
            Deque<Long> intentos = entrada.getValue();
            synchronized (intentos) {
                while (!intentos.isEmpty() && intentos.peekFirst() < inicioVentana) {
                    intentos.removeFirst();
                }
                if (intentos.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    /**
     * El cuerpo usa {@link ApiErrorDTO} como el resto de la API. Antes era un JSON a mano con la
     * clave {@code mensaje}, que el frontend no lee: leia {@code message}, no lo encontraba y
     * mostraba el error generico en vez de decir que hubo demasiados intentos.
     */
    private void responderExceso(HttpServletResponse response, Cuota cuota) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(1, cuota.ventanaMs() / 1000)));
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiErrorDTO.of(HttpStatus.TOO_MANY_REQUESTS.value(), cuota.mensaje())));
    }

    private record Cuota(String grupo, int maxIntentos, long ventanaMs, String mensaje) {
    }
}
