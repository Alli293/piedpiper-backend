package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de seguridad con la cadena de filtros real (SecurityConfig + JwtAuthenticationFilter).
 * Verifica que GET /api/catalogos/** es público (permitAll en SecurityConfig).
 */
@WebMvcTest(controllers = AuditorCatalogosController.class)
@Import(SecurityConfig.class)
class AuditorCatalogosSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void especialidadesSinAutenticacionDevuelve200() throws Exception {
        mockMvc.perform(get("/api/catalogos/especialidades"))
                .andExpect(status().isOk());
    }

    @Test
    void zonasSinAutenticacionDevuelve200() throws Exception {
        mockMvc.perform(get("/api/catalogos/zonas"))
                .andExpect(status().isOk());
    }
}
