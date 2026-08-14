package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioCorreoService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegistroCorreoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class RegistroCorreoControllerInvitacionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroInvitacionCorreoService registroInvitacionCorreoService;
    @MockitoBean
    private RegistroUsuarioCorreoService registroUsuarioCorreoService;
    @MockitoBean
    private RegistroEmpresaCorreoService registroEmpresaCorreoService;
    @MockitoBean
    private RegistroAuditorCorreoService registroAuditorCorreoService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String REQUEST_JSON =
            "{\"tokenInvitacion\":\"token-inv\",\"nombre\":\"Ana\",\"apellidos\":\"Torres\","
                    + "\"contrasena\":\"clave1234\",\"confirmarContrasena\":\"clave1234\",\"aceptaTerminos\":true}";

    @Test
    void registroPorInvitacionCorreoValidoDevuelve201() throws Exception {
        when(registroInvitacionCorreoService.registrar(any())).thenReturn(new AuthResponseDTO(
                "jwt-app", "USUARIO_GENERAL", "ACTIVO", "/perfil/configuracion-inicial"));

        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("USUARIO_GENERAL"))
                .andExpect(jsonPath("$.redirect").value("/perfil/configuracion-inicial"));
    }

    @Test
    void tokenDeInvitacionInexistenteDevuelve404() throws Exception {
        when(registroInvitacionCorreoService.registrar(any())).thenThrow(ApiException.invitacionInvalida());

        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void invitacionRevocadaDevuelve409() throws Exception {
        when(registroInvitacionCorreoService.registrar(any())).thenThrow(ApiException.invitacionNoDisponible());

        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void invitacionExpiradaDevuelve410() throws Exception {
        when(registroInvitacionCorreoService.registrar(any())).thenThrow(ApiException.invitacionExpirada());

        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isGone());
    }

    @Test
    void correoDuplicadoDevuelve409() throws Exception {
        when(registroInvitacionCorreoService.registrar(any())).thenThrow(ApiException.cuentaDuplicada(
                "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?"));

        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void sinTokenDeInvitacionDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellidos\":\"Torres\",\"contrasena\":\"clave1234\","
                                + "\"confirmarContrasena\":\"clave1234\",\"aceptaTerminos\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenInvitacion\":\"token-inv\",\"apellidos\":\"Torres\","
                                + "\"contrasena\":\"clave1234\",\"confirmarContrasena\":\"clave1234\","
                                + "\"aceptaTerminos\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinApellidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenInvitacion\":\"token-inv\",\"nombre\":\"Ana\","
                                + "\"contrasena\":\"clave1234\",\"confirmarContrasena\":\"clave1234\","
                                + "\"aceptaTerminos\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contrasenasQueNoCoincidenDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenInvitacion\":\"token-inv\",\"nombre\":\"Ana\",\"apellidos\":\"Torres\","
                                + "\"contrasena\":\"clave1234\",\"confirmarContrasena\":\"otra-clave1\","
                                + "\"aceptaTerminos\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinAceptarTerminosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/invitacion/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenInvitacion\":\"token-inv\",\"nombre\":\"Ana\",\"apellidos\":\"Torres\","
                                + "\"contrasena\":\"clave1234\",\"confirmarContrasena\":\"clave1234\","
                                + "\"aceptaTerminos\":false}"))
                .andExpect(status().isBadRequest());
    }
}
