package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroUsuarioService registroUsuarioService;
    @MockitoBean
    private RegistroEmpresaService registroEmpresaService;
    @MockitoBean
    private RegistroAuditorService registroAuditorService;
    @MockitoBean
    private LoginService loginService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void registroUsuarioValidoDevuelve201() throws Exception {
        when(registroUsuarioService.registrar(any()))
                .thenReturn(new AuthResponse("jwt", "USUARIO_INDIVIDUAL", "ACTIVO", "/perfil"));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\",\"aceptaTerminos\":true}"))
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
                        .content("{\"idToken\":\"t\",\"aceptaTerminos\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correoNoVerificadoDevuelve422() throws Exception {
        when(registroUsuarioService.registrar(any())).thenThrow(ApiException.correoNoVerificado());

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\",\"aceptaTerminos\":true}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cuentaDuplicadaDevuelve409() throws Exception {
        when(registroUsuarioService.registrar(any()))
                .thenThrow(ApiException.cuentaDuplicada("Ya existe una cuenta con este correo."));

        mockMvc.perform(post("/api/auth/registro/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\",\"aceptaTerminos\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void loginCredencialesMalasDevuelve401() throws Exception {
        when(loginService.login(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"CORREO\",\"email\":\"a@b.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginBloqueadoDevuelve429() throws Exception {
        when(loginService.login(any()))
                .thenThrow(new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"CORREO\",\"email\":\"a@b.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void loginGoogleSinCuentaDevuelve404() throws Exception {
        when(loginService.login(any()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "No encontramos una cuenta."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"GOOGLE\",\"idToken\":\"t\"}"))
                .andExpect(status().isNotFound());
    }
}
