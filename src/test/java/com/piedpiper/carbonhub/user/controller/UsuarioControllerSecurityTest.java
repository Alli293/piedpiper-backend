package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.user.service.PerfilInicialService;
import com.piedpiper.carbonhub.user.service.PreferenciasUsuarioService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class})
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferenciasUsuarioService preferenciasUsuarioService;
    @MockitoBean
    private PerfilInicialService perfilInicialService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me/preferencias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilInicialGetSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me/perfil-inicial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilInicialPutSinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/perfil-inicial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreVisible": "Ana G.",
                                  "preferencias": {
                                    "idioma": "ESPANOL",
                                    "moneda": "CRC",
                                    "unidades": "METRICO"
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenInvalidoDevuelve401() throws Exception {
        when(jwtService.parsear(anyString()))
                .thenThrow(new io.jsonwebtoken.JwtException("token inválido"));

        mockMvc.perform(get("/api/usuarios/me/preferencias")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("X-Refresh-Token"));
    }

    @Test
    void conTokenValidoDevuelve200() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(USUARIO_ID.toString());
        when(jwtService.parsear(anyString())).thenReturn(claims);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(USUARIO_ID)
                .email("usuario@correo.com")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build()));
        when(preferenciasUsuarioService.obtenerPreferencias(any(UUID.class)))
                .thenReturn(new PreferenciasUsuarioResponseDTO("ESPANOL", "CRC", "METRICO"));
        when(jwtService.generar(any(Usuario.class))).thenReturn("token-renovado");

        mockMvc.perform(get("/api/usuarios/me/preferencias")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Refresh-Token", "token-renovado"));
    }
}
