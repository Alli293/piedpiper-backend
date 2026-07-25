package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialResponseDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.user.service.PerfilInicialService;
import com.piedpiper.carbonhub.user.service.PreferenciasUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

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

    private static final String REQUEST_JSON = """
            {"idioma":"INGLES","moneda":"USD","unidades":"METRICO"}""";

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication AUTHENTICATION = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_INDIVIDUAL")));

    @Test
    void guardadoValidoDevuelve200ConLasPreferencias() throws Exception {
        when(preferenciasUsuarioService.actualizarPreferencias(any(UUID.class), any()))
                .thenReturn(new PreferenciasUsuarioResponseDTO("INGLES", "USD", "METRICO"));

        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("INGLES"))
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.unidades").value("METRICO"));
    }

    @Test
    void valorFueraDeCatalogoDevuelve422() throws Exception {
        when(preferenciasUsuarioService.actualizarPreferencias(any(UUID.class), any()))
                .thenThrow(ApiException.valorNoSoportado("El idioma seleccionado no está soportado."));

        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idioma":"FRANCES","moneda":"USD","unidades":"METRICO"}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void campoVacioDevuelve400PorValidacionDelDTO() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idioma":"","moneda":"USD","unidades":"METRICO"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fallaDePersistenciaDevuelve500() throws Exception {
        when(preferenciasUsuarioService.actualizarPreferencias(any(UUID.class), any()))
                .thenThrow(ApiException.errorInterno(
                        "No se pudieron guardar tus preferencias. Intenta nuevamente."));

        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("No se pudieron guardar tus preferencias. Intenta nuevamente."));
    }

    @Test
    void obtenerPreferenciasDevuelve200ConLosValoresDelPerfil() throws Exception {
        when(preferenciasUsuarioService.obtenerPreferencias(any(UUID.class)))
                .thenReturn(new PreferenciasUsuarioResponseDTO("ESPANOL", "CRC", "METRICO"));

        mockMvc.perform(get("/api/usuarios/me/preferencias")
                        .principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("ESPANOL"))
                .andExpect(jsonPath("$.moneda").value("CRC"))
                .andExpect(jsonPath("$.unidades").value("METRICO"));
    }

    private static final String PERFIL_JSON = """
            {"nombreVisible":"Ana G.",
             "preferencias":{"idioma":"ESPANOL","moneda":"CRC","unidades":"METRICO"}}""";

    private PerfilInicialResponseDTO perfilResponse() {
        return new PerfilInicialResponseDTO("Ana G.", "Ana", "Gómez",
                new PreferenciasUsuarioResponseDTO("ESPANOL", "CRC", "METRICO"),
                "USUARIO_INDIVIDUAL", true, "/ecoruta", null);
    }

    @Test
    void perfilInicialGuardadoValidoDevuelve200ConRedireccion() throws Exception {
        when(perfilInicialService.completar(any(UUID.class), any())).thenReturn(perfilResponse());

        mockMvc.perform(put("/api/usuarios/me/perfil-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERFIL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracionCompleta").value(true))
                .andExpect(jsonPath("$.redirect").value("/ecoruta"));
    }

    @Test
    void perfilInicialEdicionFueraDeRolDevuelve403() throws Exception {
        when(perfilInicialService.completar(any(UUID.class), any()))
                .thenThrow(ApiException.accesoDenegado(
                        "No puedes modificar los datos de la empresa con tu rol."));

        mockMvc.perform(put("/api/usuarios/me/perfil-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERFIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilInicialPreferenciaFueraDeCatalogoDevuelve422() throws Exception {
        when(perfilInicialService.completar(any(UUID.class), any()))
                .thenThrow(ApiException.valorNoSoportado("El idioma seleccionado no está soportado."));

        mockMvc.perform(put("/api/usuarios/me/perfil-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERFIL_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void perfilInicialNombreCortoDevuelve400PorValidacionDelDTO() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/perfil-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreVisible":"A",
                                 "preferencias":{"idioma":"ESPANOL","moneda":"CRC","unidades":"METRICO"}}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void perfilInicialObtenerDevuelve200ConElEstadoDelPerfil() throws Exception {
        when(perfilInicialService.obtener(any(UUID.class))).thenReturn(perfilResponse());

        mockMvc.perform(get("/api/usuarios/me/perfil-inicial")
                        .principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("USUARIO_INDIVIDUAL"));
    }
}
