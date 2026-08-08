package com.piedpiper.carbonhub.auth.config;

import com.piedpiper.carbonhub.ecoruta.config.CertificacionApiKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CertificacionApiKeyFilter certificacionApiKeyFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CertificacionApiKeyFilter certificacionApiKeyFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.certificacionApiKeyFilter = certificacionApiKeyFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource))
                // CSRF no aplica: la API es stateless (sin sesion ni cookies) y toda
                // autenticacion viaja en headers (Authorization: Bearer, X-Certificacion-Api-Key)
                // que un sitio malicioso no puede adjuntar automaticamente a una peticion cross-site.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verificar-correo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/reset-contrasena").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/invitaciones/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/**").permitAll()
                        // Un verificador externo de credenciales OpenBadges no tiene sesion:
                        // debe poder resolver el perfil del emisor, su clave publica y la
                        // definicion del logro para validar una certificacion.
                        .requestMatchers(HttpMethod.GET,
                                "/api/certificaciones/emisor",
                                "/api/certificaciones/emisor/jwks.json",
                                "/api/certificaciones/estado/lista").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/certificaciones/logros/**")
                        .permitAll()
                        // Verificacion publica de una certificacion puntual (PP-59): un
                        // tercero sin sesion (o LinkedIn) debe poder resolverla por id.
                        .requestMatchers(HttpMethod.GET, "/api/certificaciones/*/verificar")
                        .permitAll()
                        // VC-JWT crudo de una certificacion puntual: es el artefacto que
                        // realmente esperan los validadores de OpenBadges 3.0 (tres partes
                        // separadas por punto) -- /verificar solo devuelve el documento
                        // JSON-LD decodificado, sin firma.
                        .requestMatchers(HttpMethod.GET, "/api/certificaciones/*/verificacion.jwt")
                        .permitAll()
                        // Perfil publico de una empresa: pagina publica sin sesion.
                        // Rutas explicitas para que un endpoint nuevo no nazca publico sin revision.
                        // Verificacion publica de insignias empresariales (PP-61). El JSON-LD
                        // es legible para usuarios y el .jwt es el artefacto firmado que
                        // validadores OpenBadges 3.0 esperan recibir.
                        .requestMatchers(HttpMethod.GET,
                                "/api/insignias/*/verificacion",
                                "/api/insignias/*/verificacion.jwt",
                                "/api/insignias/logros/**").permitAll()
                        // Perfil publico de una empresa: pagina publica sin sesion, expone
                        // solo certificaciones activas por slug. Ruta explicita, no comodin
                        // "/**", para que un endpoint nuevo bajo este prefijo no nazca publico
                        // sin que alguien lo revise (mismo criterio que las rutas de
                        // certificaciones listadas arriba).
                        .requestMatchers(HttpMethod.GET,
                                "/api/perfil-publico/buscar",
                                "/api/perfil-publico/*",
                                "/api/perfil-publico/*/certificaciones",
                                "/api/perfil-publico/*/insignias",
                                "/api/perfil-publico/*/compartir",
                                "/api/perfil-publico/*/evolucion-huella").permitAll()
                        // Verificacion publica por codigo corto (PP-68): mismo criterio que
                        // la verificacion por id de arriba, ahora bajo su propio prefijo.
                        .requestMatchers(HttpMethod.GET, "/api/verificar/*").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(certificacionApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
