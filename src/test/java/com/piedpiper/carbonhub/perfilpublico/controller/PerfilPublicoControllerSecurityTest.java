package com.piedpiper.carbonhub.perfilpublico.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PerfilPublicoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class})
class PerfilPublicoControllerSecurityTest {

    private static final String SLUG = "cafe-del-valle-test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilPublicoCertificacionesService perfilPublicoCertificacionesService;
    @MockitoBean
    private PerfilPublicoConsultaService perfilPublicoConsultaService;
    @MockitoBean
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void insigniasPublicasNoRequierenAutenticacion() throws Exception {
        when(insigniaEmpresaConsultaService.listarPorSlug(SLUG))
                .thenReturn(List.of(new InsigniaEmpresaResponseDTO(
                        1L,
                        "bronce",
                        "Carbono Neutral",
                        "Insignia activa verificable.",
                        Instant.parse("2026-01-15T00:00:00Z")
                )));

        mockMvc.perform(get("/api/perfil-publico/{slug}/insignias", SLUG))
                .andExpect(status().isOk());
    }
}
