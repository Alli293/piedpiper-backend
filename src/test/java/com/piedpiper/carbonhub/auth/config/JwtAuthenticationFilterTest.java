package com.piedpiper.carbonhub.auth.config;

import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "clave-secreta-de-pruebas-para-firmar-tokens-jwt-123456";
    private static final long EXPIRATION_MS = 1_800_000L;
    private static final long SESION_MAXIMA_MS = 43_200_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS, SESION_MAXIMA_MS);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioRechazadoConTokenValidoQuedaAutenticado() throws Exception {
        Usuario rechazado = usuario(EstadoUsuario.RECHAZADO);
        when(usuarioRepository.findById(rechazado.getId())).thenReturn(Optional.of(rechazado));

        filter.doFilterInternal(peticionConToken(rechazado), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(rechazado.getId().toString());
    }

    // No debe arrastrar el rol operativo: si un auditor RECHAZADO recibiera ROLE_AUDITOR_CERTIFICADO
    // podria seguir aceptando/emitiendo resultados de auditorias via DecisionAuditorController pese
    // a que la plataforma le retiro la credencial.
    @Test
    void usuarioRechazadoNoRecibeRolDeAuditorCertificado() throws Exception {
        Usuario rechazado = usuario(EstadoUsuario.RECHAZADO);
        when(usuarioRepository.findById(rechazado.getId())).thenReturn(Optional.of(rechazado));

        filter.doFilterInternal(peticionConToken(rechazado), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_AUDITOR_RECHAZADO");
    }

    @Test
    void usuarioPendienteDeValidacionConTokenValidoQuedaAutenticado() throws Exception {
        Usuario pendiente = usuario(EstadoUsuario.PENDIENTE_VALIDACION);
        when(usuarioRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));

        filter.doFilterInternal(peticionConToken(pendiente), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    // A diferencia de RECHAZADO, PENDIENTE_VALIDACION si necesita el rol operativo completo: lo usa
    // para completar su configuracion inicial en ConfiguracionInicialAuditorController.
    @Test
    void usuarioPendienteDeValidacionRecibeRolDeAuditorCertificado() throws Exception {
        Usuario pendiente = usuario(EstadoUsuario.PENDIENTE_VALIDACION);
        when(usuarioRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));

        filter.doFilterInternal(peticionConToken(pendiente), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_AUDITOR_CERTIFICADO");
    }

    @Test
    void usuarioDeshabilitadoConTokenValidoNoQuedaAutenticado() throws Exception {
        Usuario deshabilitado = usuario(EstadoUsuario.DESHABILITADO);
        when(usuarioRepository.findById(deshabilitado.getId())).thenReturn(Optional.of(deshabilitado));

        filter.doFilterInternal(peticionConToken(deshabilitado), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usuarioActivoConTokenValidoQuedaAutenticado() throws Exception {
        Usuario activo = usuario(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findById(activo.getId())).thenReturn(Optional.of(activo));

        filter.doFilterInternal(peticionConToken(activo), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void sinEncabezadoDeAutorizacionNoQuedaAutenticadoYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenMalFormadoLimpiaElContextoDeSeguridad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usuarioInexistenteNoQuedaAutenticado() throws Exception {
        Usuario fantasma = usuario(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findById(fantasma.getId())).thenReturn(Optional.empty());

        filter.doFilterInternal(peticionConToken(fantasma), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest peticionConToken(Usuario usuario) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generar(usuario));
        return request;
    }

    private Usuario usuario(EstadoUsuario estado) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("auditor@correo.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(estado)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }
}
