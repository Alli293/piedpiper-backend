package com.piedpiper.carbonhub.certificacion.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CertificacionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class})
class CertificacionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaCertificacionService consultaCertificacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/certificaciones/" + UUID.randomUUID() + "/jsonld"))
                .andExpect(status().isUnauthorized());
    }
}
