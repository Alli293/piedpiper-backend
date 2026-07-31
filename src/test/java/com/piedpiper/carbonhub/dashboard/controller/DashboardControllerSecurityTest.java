package com.piedpiper.carbonhub.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.dashboard.service.DashboardCertificacionesService;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class})
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardHuellaService dashboardHuellaService;
    @MockitoBean
    private DashboardCertificacionesService dashboardCertificacionesService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/dashboard/huella")
                        .param("periodo", "mes_actual"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void certificacionesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/dashboard/certificaciones"))
                .andExpect(status().isUnauthorized());
    }
}
