package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ValidarTokenResetResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SesionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class SesionControllerResetContrasenaTest {

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

    private static final String MENSAJE_UNIFORME =
            "Si existe una cuenta con ese correo, te enviamos un enlace para restablecer tu contraseña.";

    @Test
    void solicitarResetContrasenaValidoDevuelve200ConMensajeUniforme() throws Exception {
        when(restablecerContrasenaService.solicitar("ana.perez@example.com"))
                .thenReturn(new MensajeResponseDTO(MENSAJE_UNIFORME));

        mockMvc.perform(post("/api/auth/solicitar-reset-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.perez@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_UNIFORME));
    }

    @Test
    void solicitarResetContrasenaConEmailMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/solicitar-reset-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validarTokenResetValidoDevuelve200ConElEmail() throws Exception {
        String token = "a".repeat(43);
        when(restablecerContrasenaService.validarToken(token))
                .thenReturn(new ValidarTokenResetResponseDTO("ana.perez@example.com"));

        mockMvc.perform(post("/api/auth/reset-contrasena/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"));
    }

    @Test
    void validarTokenResetInvalidoOExpiradoDevuelve410() throws Exception {
        String token = "a".repeat(43);
        when(restablecerContrasenaService.validarToken(token))
                .thenThrow(ApiException.tokenResetInvalido());

        mockMvc.perform(post("/api/auth/reset-contrasena/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void validarTokenResetMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-contrasena/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-corto\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restablecerContrasenaValidoDevuelve200() throws Exception {
        String token = "a".repeat(43);
        when(restablecerContrasenaService.restablecer(token, "claveNueva1"))
                .thenReturn(new MensajeResponseDTO("Tu contraseña fue actualizada. Ya puedes iniciar sesión."));

        mockMvc.perform(post("/api/auth/restablecer-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"nuevaContrasena\":\"claveNueva1\","
                                + "\"confirmarContrasena\":\"claveNueva1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu contraseña fue actualizada. Ya puedes iniciar sesión."));
    }

    @Test
    void restablecerContrasenaConContrasenasQueNoCoincidenDevuelve400() throws Exception {
        String token = "a".repeat(43);
        mockMvc.perform(post("/api/auth/restablecer-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"nuevaContrasena\":\"claveNueva1\","
                                + "\"confirmarContrasena\":\"otra-clave1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restablecerContrasenaConFormatoDeContrasenaInvalidoDevuelve400() throws Exception {
        String token = "a".repeat(43);
        mockMvc.perform(post("/api/auth/restablecer-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"nuevaContrasena\":\"soloLetras\","
                                + "\"confirmarContrasena\":\"soloLetras\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restablecerContrasenaConTokenInvalidoOExpiradoDevuelve410() throws Exception {
        String token = "a".repeat(43);
        when(restablecerContrasenaService.restablecer(token, "claveNueva1"))
                .thenThrow(ApiException.tokenResetInvalido());

        mockMvc.perform(post("/api/auth/restablecer-contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"nuevaContrasena\":\"claveNueva1\","
                                + "\"confirmarContrasena\":\"claveNueva1\"}"))
                .andExpect(status().isGone());
    }
}
