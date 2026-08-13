package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.auth.service.LoginService;
import com.piedpiper.carbonhub.auth.service.RestablecerContrasenaService;
import com.piedpiper.carbonhub.auth.service.VerificarCorreoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SesionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class SesionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;
    @MockitoBean
    private VerificarCorreoService verificarCorreoService;
    @MockitoBean
    private RestablecerContrasenaService restablecerContrasenaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

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
    void loginCuentaNoVerificadaDevuelve403() throws Exception {
        when(loginService.login(any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN,
                        "Tu correo aún no ha sido verificado. Reenviar correo de verificación."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"CORREO\",\"email\":\"a@b.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isForbidden());
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

    @Test
    void verificarCorreoConTokenValidoDevuelve200() throws Exception {
        String tokenValido = "a".repeat(43);
        when(verificarCorreoService.verificar(tokenValido))
                .thenReturn(new MensajeResponseDTO("Tu correo fue verificado. Ya puedes iniciar sesión."));

        mockMvc.perform(post("/api/auth/verificar-correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + tokenValido + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu correo fue verificado. Ya puedes iniciar sesión."));
    }

    @Test
    void verificarCorreoConTokenInexistenteOExpiradoDevuelve410() throws Exception {
        String token = "a".repeat(43);
        when(verificarCorreoService.verificar(token))
                .thenThrow(ApiException.tokenVerificacionInvalido());

        mockMvc.perform(post("/api/auth/verificar-correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void verificarCorreoConTokenMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/verificar-correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-corto\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarCorreoConCuentaYaVerificadaDevuelve409() throws Exception {
        String token = "a".repeat(43);
        when(verificarCorreoService.verificar(token))
                .thenThrow(ApiException.correoYaVerificado());

        mockMvc.perform(post("/api/auth/verificar-correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reenviarVerificacionValidoDevuelve200() throws Exception {
        when(verificarCorreoService.reenviar("ana.perez@example.com")).thenReturn(
                new MensajeResponseDTO("Si tu cuenta requiere verificación, te enviamos un nuevo enlace."));

        mockMvc.perform(post("/api/auth/reenviar-verificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.perez@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("Si tu cuenta requiere verificación, te enviamos un nuevo enlace."));
    }

    @Test
    void reenviarVerificacionConEmailMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/reenviar-verificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reenviarVerificacionConExcesoDeSolicitudesDevuelve429() throws Exception {
        when(verificarCorreoService.reenviar("ana.perez@example.com"))
                .thenThrow(ApiException.reenviosVerificacionExcedidos());

        mockMvc.perform(post("/api/auth/reenviar-verificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.perez@example.com\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
