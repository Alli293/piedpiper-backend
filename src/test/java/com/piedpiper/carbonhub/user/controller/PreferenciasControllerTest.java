package com.piedpiper.carbonhub.user.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasResponseDTO;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.user.service.PreferenciasService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PreferenciasController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class PreferenciasControllerTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UsernamePasswordAuthenticationToken PRINCIPAL =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferenciasService preferenciasService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void guardadoValidoDevuelve200ConPreferencias() throws Exception {
        when(preferenciasService.actualizar(eq(USUARIO_ID), any()))
                .thenReturn(new PreferenciasResponseDTO("INGLES", "USD", "METRICO"));

        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idioma\":\"INGLES\",\"moneda\":\"USD\",\"unidades\":\"METRICO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("INGLES"))
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.unidades").value("METRICO"));
    }

    @Test
    void valorFueraDeCatalogoDevuelve422() throws Exception {
        when(preferenciasService.actualizar(eq(USUARIO_ID), any()))
                .thenThrow(ApiException.preferenciaInvalida("idioma"));

        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idioma\":\"FRANCES\",\"moneda\":\"CRC\",\"unidades\":\"METRICO\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void camposVaciosDevuelven400() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/preferencias")
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idioma\":\"\",\"moneda\":\"\",\"unidades\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerDevuelve200ConPreferenciasDelPerfil() throws Exception {
        when(preferenciasService.obtener(USUARIO_ID))
                .thenReturn(new PreferenciasResponseDTO("ESPANOL", "CRC", "METRICO"));

        mockMvc.perform(get("/api/usuarios/me/preferencias").principal(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idioma").value("ESPANOL"))
                .andExpect(jsonPath("$.moneda").value("CRC"))
                .andExpect(jsonPath("$.unidades").value("METRICO"));
    }
}
