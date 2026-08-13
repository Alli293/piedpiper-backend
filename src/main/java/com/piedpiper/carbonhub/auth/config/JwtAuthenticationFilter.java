package com.piedpiper.carbonhub.auth.config;

import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parsear(token);
                Usuario usuario = usuarioRepository.findById(UUID.fromString(claims.getSubject()))
                        .orElse(null);
                if (usuario != null && habilitado(usuario)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            usuario.getId().toString(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    renovarSiLaSesionSigueVigente(response, claims, usuario);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Cada peticion autenticada devuelve un token nuevo, asi que quien usa la aplicacion no tiene
     * que volver a escribir su contrasena. El efecto no buscado es que una sesion no vence jamas:
     * con tocar cualquier endpoint una vez por hora, un token robado sirve para siempre. Pasado el
     * tope absoluto se deja de renovar y el token que el atacante tenga en la mano caduca solo.
     *
     * <p>El token en curso sigue valido hasta su propia expiracion: no se corta la peticion, solo
     * se deja de extender la sesion.</p>
     */
    private void renovarSiLaSesionSigueVigente(HttpServletResponse response, Claims claims,
                                               Usuario usuario) {
        if (!jwtService.puedeRenovarse(claims)) {
            return;
        }
        jwtService.inicioSesionDe(claims).ifPresent(inicio ->
                response.setHeader("X-Refresh-Token", jwtService.renovar(usuario, inicio)));
    }

    private boolean habilitado(Usuario usuario) {
        return usuario.getEstado() != EstadoUsuario.RECHAZADO
                && usuario.getEstado() != EstadoUsuario.DESHABILITADO;
    }
}
