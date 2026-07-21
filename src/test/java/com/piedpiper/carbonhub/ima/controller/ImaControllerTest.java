package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.service.ImaService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ImaControllerTest.MethodSecurityTestConfig.class)
class ImaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImaService imaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void imaValidoDevuelve200ConPuntajes() throws Exception {
        ImaResponseDTO dto = ImaResponseDTO.builder()
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("66.7"))
                .puntajeIntensidadSectorial(new BigDecimal("55.0"))
                .ima(new BigDecimal("65.6"))
                .parcial(false)
                .intensidad(new BigDecimal("1.818182"))
                .calculatedAt(Instant.now())
                .build();
        when(imaService.obtenerIma(anyInt(), anyInt(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "6").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cobertura").value(75.0))
                .andExpect(jsonPath("$.consistencia").value(66.7))
                .andExpect(jsonPath("$.puntajeIntensidadSectorial").value(55.0))
                .andExpect(jsonPath("$.ima").value(65.6))
                .andExpect(jsonPath("$.parcial").value(false));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void mesInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "13").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void mesNegativoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "0").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void anioFueraDeRangoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/ima").param("anio", "1999").param("mes", "6").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void periodoFuturoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "12").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "USUARIO_GENERAL")
    void usuarioGeneralTieneAcceso() throws Exception {
        ImaResponseDTO dto = ImaResponseDTO.builder()
                .cobertura(BigDecimal.ZERO)
                .consistencia(BigDecimal.ZERO)
                .ima(BigDecimal.ZERO)
                .parcial(true)
                .motivoParcial("Aún no hay emisiones registradas para calcular tu IMA completo.")
                .calculatedAt(Instant.now())
                .build();
        when(imaService.obtenerIma(anyInt(), anyInt(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "6").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcial").value(true))
                .andExpect(jsonPath("$.motivoParcial").exists());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void auditorNoTieneAcceso() throws Exception {
        mockMvc.perform(get("/api/ima").param("anio", "2026").param("mes", "6").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void sinParametrosUsaDefaultsYDevuelve200() throws Exception {
        ImaResponseDTO dto = ImaResponseDTO.builder()
                .cobertura(new BigDecimal("50.0"))
                .consistencia(new BigDecimal("25.0"))
                .ima(new BigDecimal("37.5"))
                .parcial(true)
                .motivoParcial("Tu sector aún no tiene suficientes empresas (mínimo 5).")
                .calculatedAt(Instant.now())
                .build();
        when(imaService.obtenerIma(anyInt(), anyInt(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/ima").principal(new TestingAuthenticationToken("41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ima").value(37.5));
    }
}
