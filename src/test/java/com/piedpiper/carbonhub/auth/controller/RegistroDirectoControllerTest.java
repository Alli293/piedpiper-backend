package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioService;
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

@WebMvcTest(controllers = RegistroDirectoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class RegistroDirectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroUsuarioService registroUsuarioService;
    @MockitoBean
    private RegistroEmpresaService registroEmpresaService;
    @MockitoBean
    private RegistroAuditorService registroAuditorService;
    @MockitoBean
    private RegistroInvitacionService registroInvitacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String REGISTRO_JSON = "{\"idToken\":\"t\",\"aceptaTerminos\":true}";

    @Test
    void registroUsuarioValidoDevuelve201() throws Exception {
        when(registroUsuarioService.registrar(any()))
                .thenReturn(new AuthResponseDTO("jwt", "USUARIO_INDIVIDUAL", "ACTIVO", "/perfil"));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void registroSinAceptarTerminosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\",\"aceptaTerminos\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        when(registroUsuarioService.registrar(any())).thenThrow(ApiException.tokenInvalido());

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correoNoVerificadoDevuelve422() throws Exception {
        when(registroUsuarioService.registrar(any())).thenThrow(ApiException.correoNoVerificado());

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cuentaDuplicadaDevuelve409() throws Exception {
        when(registroUsuarioService.registrar(any()))
                .thenThrow(ApiException.cuentaDuplicada("Ya existe una cuenta con este correo."));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void registroEmpresaValidoDevuelve201() throws Exception {
        when(registroEmpresaService.registrar(any()))
                .thenReturn(new AuthResponseDTO("jwt", "ADMINISTRADOR_EMPRESA", "ACTIVO",
                        "/empresa/configuracion-inicial"));

        mockMvc.perform(post("/api/auth/registro/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.redirect").value("/empresa/configuracion-inicial"));
    }

    @Test
    void registroEmpresaTokenInvalidoDevuelve401() throws Exception {
        when(registroEmpresaService.registrar(any())).thenThrow(ApiException.tokenInvalido());

        mockMvc.perform(post("/api/auth/registro/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroEmpresaRepresentanteDuplicadoDevuelve409() throws Exception {
        when(registroEmpresaService.registrar(any())).thenThrow(ApiException.cuentaDuplicada(
                "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?"));

        mockMvc.perform(post("/api/auth/registro/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void registroAuditorValidoDevuelve201() throws Exception {
        when(registroAuditorService.registrar(any()))
                .thenReturn(new AuthResponseDTO("jwt", "AUDITOR_CERTIFICADO", "PENDIENTE_VALIDACION",
                        "/auditor/configuracion-inicial"));

        mockMvc.perform(post("/api/auth/registro/auditor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.redirect").value("/auditor/configuracion-inicial"));
    }

    @Test
    void registroAuditorTokenInvalidoDevuelve401() throws Exception {
        when(registroAuditorService.registrar(any())).thenThrow(ApiException.tokenInvalido());

        mockMvc.perform(post("/api/auth/registro/auditor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnauthorized());
    }
}
