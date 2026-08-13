package com.piedpiper.carbonhub.meta.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.meta.service.MetaService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MetaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CorsConfig.class})
class MetaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetaService metaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void postSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/metas")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/metas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putSinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/api/metas/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/metas/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
