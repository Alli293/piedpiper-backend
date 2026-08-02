package com.piedpiper.carbonhub.insignia.controller;

import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaOpenBadgesService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InsigniaEmpresaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                }))
@AutoConfigureMockMvc(addFilters = false)
class InsigniaEmpresaControllerTest {

    private static final String USUARIO_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    @MockitoBean
    private InsigniaEmpresaOpenBadgesService insigniaEmpresaOpenBadgesService;

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void descargarJsonLdRetornaCredencialOpenBadges() throws Exception {
        UUID idInsigniaEmpresa = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(insigniaEmpresaOpenBadgesService.generarParaEmpresaAutenticada(
                UUID.fromString(USUARIO_ID), idInsigniaEmpresa))
                .thenReturn(new InsigniaEmpresaOpenBadgesService.DocumentoInsigniaOpenBadges(
                        "carbono-neutral.jsonld",
                        Map.of(
                                "@context", List.of("https://www.w3.org/ns/credentials/v2"),
                                "type", List.of("VerifiableCredential", "OpenBadgeCredential"),
                                "name", "Carbono Neutral - Bronce"
                        )));

        mockMvc.perform(get("/api/insignias/{id}/jsonld", idInsigniaEmpresa)
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carbono Neutral - Bronce"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"carbono-neutral.jsonld\""));
    }

    @Test
    void verificacionJwtPublicaRetornaVcJwtParaOpenBadges() throws Exception {
        UUID idInsigniaEmpresa = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(insigniaEmpresaOpenBadgesService.generarJwtPublico(idInsigniaEmpresa))
                .thenReturn("header.payload.signature");

        mockMvc.perform(get("/api/insignias/{id}/verificacion.jwt", idInsigniaEmpresa))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vc+ld+json+jwt"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getContentAsString()).isEqualTo("header.payload.signature"));
    }
}
