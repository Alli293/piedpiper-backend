package com.piedpiper.carbonhub.establecimiento.controller;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;
import com.piedpiper.carbonhub.establecimiento.models.dtos.EstablecimientoBannerResponseDTO;
import com.piedpiper.carbonhub.establecimiento.service.EstablecimientoBannerService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EstablecimientoController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        CorsConfig.class
})
class EstablecimientoControllerTest {

    private static final String USUARIO_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID EMPRESA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstablecimientoBannerService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getBannerConCodigoIsoValidoRetorna200ConElDto() throws Exception {
        BannerOrigenDTO banner = new BannerOrigenDTO(
                "Costa Rica", "\uD83C\uDDE8\uD83C\uDDF7", "https://flagcdn.com/cr.svg", "CR");
        when(service.obtenerBanner(eq(EMPRESA_ID)))
                .thenReturn(new EstablecimientoBannerResponseDTO(banner));

        mockMvc.perform(get("/api/establecimientos/{id}/banner", EMPRESA_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banner.nombrePais").value("Costa Rica"))
                .andExpect(jsonPath("$.banner.banderaUrlSvg").value("https://flagcdn.com/cr.svg"))
                .andExpect(jsonPath("$.banner.codigoIso").value("CR"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getBannerConCountriesDevCaidoRetorna200ConBannerNull() throws Exception {
        when(service.obtenerBanner(eq(EMPRESA_ID)))
                .thenReturn(new EstablecimientoBannerResponseDTO(null));

        mockMvc.perform(get("/api/establecimientos/{id}/banner", EMPRESA_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banner").doesNotExist());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getBannerConEstablecimientoInexistenteRetorna404() throws Exception {
        when(service.obtenerBanner(eq(EMPRESA_ID)))
                .thenThrow(ApiException.recursoNoEncontrado("Este establecimiento no fue encontrado."));

        mockMvc.perform(get("/api/establecimientos/{id}/banner", EMPRESA_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Este establecimiento no fue encontrado."));
    }

    @Test
    void getBannerSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/establecimientos/{id}/banner", EMPRESA_ID))
                .andExpect(status().isUnauthorized());
    }
}
