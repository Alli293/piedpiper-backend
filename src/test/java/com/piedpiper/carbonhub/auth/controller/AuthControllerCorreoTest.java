package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.auth.service.LoginService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerCorreoTest {

    @MockitoBean
    private RegistroInvitacionService registroInvitacionService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroUsuarioService registroUsuarioService;
    @MockitoBean
    private RegistroEmpresaService registroEmpresaService;
    @MockitoBean
    private RegistroAuditorService registroAuditorService;
    @MockitoBean
    private RegistroAuditorCorreoService registroAuditorCorreoService;
    @MockitoBean
    private LoginService loginService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private RegistroUsuarioCorreoService registroUsuarioCorreoService;
    @MockitoBean
    private RegistroEmpresaCorreoService registroEmpresaCorreoService;
    @MockitoBean
    private VerificarCorreoService verificarCorreoService;
    @MockitoBean
    private RestablecerContrasenaService restablecerContrasenaService;

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
    void verificarCorreoConTokenValidoDevuelve200() throws Exception {
        String tokenValido = "a".repeat(43);
        when(verificarCorreoService.verificar(tokenValido))
                .thenReturn(new MensajeResponseDTO("Tu correo fue verificado. Ya puedes iniciar sesión."));

        mockMvc.perform(get("/api/auth/verificar-correo").param("token", tokenValido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu correo fue verificado. Ya puedes iniciar sesión."));
    }

    @Test
    void verificarCorreoConTokenInexistenteOExpiradoDevuelve410() throws Exception {
        String token = "a".repeat(43);
        when(verificarCorreoService.verificar(token))
                .thenThrow(ApiException.tokenVerificacionInvalido());

        mockMvc.perform(get("/api/auth/verificar-correo").param("token", token))
                .andExpect(status().isGone());
    }

    @Test
    void verificarCorreoConTokenMalFormadoDevuelve400() throws Exception {
        when(verificarCorreoService.verificar("token-corto"))
                .thenThrow(ApiException.tokenVerificacionMalFormado());

        mockMvc.perform(get("/api/auth/verificar-correo").param("token", "token-corto"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarCorreoConCuentaYaVerificadaDevuelve409() throws Exception {
        String token = "a".repeat(43);
        when(verificarCorreoService.verificar(token))
                .thenThrow(ApiException.correoYaVerificado());

        mockMvc.perform(get("/api/auth/verificar-correo").param("token", token))
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
