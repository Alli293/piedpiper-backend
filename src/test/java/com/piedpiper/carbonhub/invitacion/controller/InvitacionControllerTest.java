package com.piedpiper.carbonhub.invitacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionResponseDTO;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvitacionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(InvitacionControllerTest.MethodSecurityTestConfig.class)
class InvitacionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitacionService invitacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication AUTHENTICATION = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_EMPRESA")));
    private static final Authentication AUTHENTICATION_ROL_INCORRECTO = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_GENERAL")));

    private InvitacionResponseDTO respuesta(String estado) {
        return new InvitacionResponseDTO(
                UUID.randomUUID(), "colab@correo.com", estado, Instant.now(), Instant.now());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void emitirValidaDevuelve201() throws Exception {
        when(invitacionService.emitir(any(), any())).thenReturn(respuesta("ENVIADA"));

        mockMvc.perform(post("/api/empresas/invitaciones")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"colab@correo.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("colab@correo.com"))
                .andExpect(jsonPath("$.estado").value("ENVIADA"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void emitirConCorreoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/empresas/invitaciones")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void emitirDuplicadaDevuelve409() throws Exception {
        when(invitacionService.emitir(any(), any()))
                .thenThrow(ApiException.invitacionPendiente());

        mockMvc.perform(post("/api/empresas/invitaciones")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"colab@correo.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_GENERAL")
    void emitirSinRolAdministradorDevuelve403() throws Exception {
        mockMvc.perform(post("/api/empresas/invitaciones")
                        .principal(AUTHENTICATION_ROL_INCORRECTO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"colab@correo.com\"}"))
                .andExpect(status().isForbidden());

        verify(invitacionService, never()).emitir(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void listarDevuelve200ConLasInvitaciones() throws Exception {
        when(invitacionService.listar(any()))
                .thenReturn(List.of(respuesta("ENVIADA"), respuesta("REVOCADA")));

        mockMvc.perform(get("/api/empresas/invitaciones").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$[1].estado").value("REVOCADA"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void revocarDevuelve200ConElEstadoRevocada() throws Exception {
        when(invitacionService.revocar(any(), any())).thenReturn(respuesta("REVOCADA"));

        mockMvc.perform(post("/api/empresas/invitaciones/" + UUID.randomUUID() + "/revocar")
                        .principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVOCADA"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void revocarInexistenteDevuelve404() throws Exception {
        when(invitacionService.revocar(any(), any()))
                .thenThrow(ApiException.invitacionNoEncontrada());

        mockMvc.perform(post("/api/empresas/invitaciones/" + UUID.randomUUID() + "/revocar")
                        .principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }
}
