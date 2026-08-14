package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
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
class RegistroCorreoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroUsuarioCorreoService registroUsuarioCorreoService;
    @MockitoBean
    private RegistroEmpresaCorreoService registroEmpresaCorreoService;
    @MockitoBean
    private RegistroAuditorCorreoService registroAuditorCorreoService;
    @MockitoBean
    private RegistroInvitacionCorreoService registroInvitacionCorreoService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String REGISTRO_USUARIO_JSON = """
            {"nombre":"Ana","apellidos":"Perez","email":"ana.perez@example.com",
            "contrasena":"clave123","confirmarContrasena":"clave123","aceptaTerminos":true}""";

    private static final String REGISTRO_EMPRESA_JSON = """
            {"nombreAdmin":"Ana","apellidosAdmin":"Perez","emailAdmin":"admin@acme.com",
            "contrasena":"clave123","confirmarContrasena":"clave123","aceptaTerminos":true}""";

    @Test
    void registroUsuarioCorreoValidoDevuelve201() throws Exception {
        when(registroUsuarioCorreoService.registrar(any())).thenReturn(
                new RegistroPendienteResponseDTO(
                        "Te enviamos un correo de verificación a tu bandeja de entrada.",
                        "ana.perez@example.com"));

        mockMvc.perform(post("/api/auth/registro/usuario/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_USUARIO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"));
    }

    @Test
    void registroUsuarioCorreoDuplicadoDevuelve409() throws Exception {
        when(registroUsuarioCorreoService.registrar(any())).thenThrow(
                ApiException.cuentaDuplicada("Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?"));

        mockMvc.perform(post("/api/auth/registro/usuario/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_USUARIO_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void registroEmpresaCorreoValidoDevuelve201() throws Exception {
        when(registroEmpresaCorreoService.registrar(any())).thenReturn(
                new RegistroPendienteResponseDTO(
                        "Te enviamos un correo de verificación a tu bandeja de entrada.",
                        "admin@acme.com"));

        mockMvc.perform(post("/api/auth/registro/empresa/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_EMPRESA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@acme.com"));
    }

    @Test
    void registroEmpresaCorreoDuplicadoDevuelve409() throws Exception {
        when(registroEmpresaCorreoService.registrar(any())).thenThrow(
                ApiException.cuentaDuplicada("Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?"));

        mockMvc.perform(post("/api/auth/registro/empresa/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_EMPRESA_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void registroAuditorCorreoValidoDevuelve201() throws Exception {
        when(registroAuditorCorreoService.registrar(any()))
                .thenReturn(new RegistroPendienteResponseDTO(
                        "Registro exitoso. Revisa tu correo para verificar tu cuenta.",
                        "auditor@example.com"));

        String body = """
                {"nombre":"Carlos","apellidos":"Ramirez","email":"auditor@example.com",\
                "contrasena":"segura123","confirmarContrasena":"segura123","aceptaTerminos":true}""";

        mockMvc.perform(post("/api/auth/registro/auditor/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("auditor@example.com"))
                .andExpect(jsonPath("$.mensaje").value("Registro exitoso. Revisa tu correo para verificar tu cuenta."));
    }

    @Test
    void registroAuditorCorreoDuplicadoDevuelve409() throws Exception {
        when(registroAuditorCorreoService.registrar(any()))
                .thenThrow(ApiException.cuentaDuplicada(
                        "Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?"));

        String body = """
                {"nombre":"Carlos","apellidos":"Ramirez","email":"auditor@example.com",\
                "contrasena":"segura123","confirmarContrasena":"segura123","aceptaTerminos":true}""";

        mockMvc.perform(post("/api/auth/registro/auditor/correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
