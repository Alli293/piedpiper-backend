package com.piedpiper.carbonhub.invitacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionPublicaResponseDTO;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvitacionPublicaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class InvitacionPublicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitacionService invitacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void resolverTokenValidoDevuelve200ConCorreoYEmpresa() throws Exception {
        String token = "a".repeat(43);
        when(invitacionService.resolver(any()))
                .thenReturn(new InvitacionPublicaResponseDTO("c***@correo.com", "Acme S.A."));

        mockMvc.perform(post("/api/auth/invitaciones/resolver")
                        .contentType("application/json")
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnmascarado").value("c***@correo.com"))
                .andExpect(jsonPath("$.nombreEmpresa").value("Acme S.A."));
    }

    @Test
    void resolverTokenInexistenteDevuelve404() throws Exception {
        when(invitacionService.resolver(any())).thenThrow(ApiException.invitacionInvalida());

        mockMvc.perform(post("/api/auth/invitaciones/resolver")
                        .contentType("application/json")
                        .content("{\"token\":\"" + "a".repeat(43) + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolverTokenExpiradoDevuelve410() throws Exception {
        when(invitacionService.resolver(any())).thenThrow(ApiException.invitacionExpirada());

        mockMvc.perform(post("/api/auth/invitaciones/resolver")
                        .contentType("application/json")
                        .content("{\"token\":\"" + "a".repeat(43) + "\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void resolverTokenMalFormadoDevuelve400SinConsultarElServicio() throws Exception {
        mockMvc.perform(post("/api/auth/invitaciones/resolver")
                        .contentType("application/json")
                        .content("{\"token\":\"token-corto\"}"))
                .andExpect(status().isBadRequest());
    }
}
