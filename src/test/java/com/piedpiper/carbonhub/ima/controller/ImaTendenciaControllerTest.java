package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;
import com.piedpiper.carbonhub.ima.service.ImaBenchmarkService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.service.ImaService;
import com.piedpiper.carbonhub.ima.service.ImaTendenciaService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ImaTendenciaControllerTest.MethodSecurityTestConfig.class)
class ImaTendenciaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImaService imaService;
    @MockitoBean
    private ImaBenchmarkService imaBenchmarkService;
    @MockitoBean
    private ImaTendenciaService imaTendenciaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private TestingAuthenticationToken principal(String rol) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", rol);
    }

    private ImaTendenciaResponseDTO tendencia() {
        return ImaTendenciaResponseDTO.builder()
                .mesesAtras(12)
                .sinDatosSectoriales(false)
                .eventos(List.of(
                        ImaEventoDTO.builder()
                                .mes("2026-06")
                                .tipo(TipoEventoIma.CRUCE_SECTOR)
                                .texto("En junio 2026 tu IMA superó el promedio de tu sector.")
                                .build()))
                .serie(List.of(
                        ImaTendenciaPuntoDTO.builder()
                                .mes("2026-05")
                                .imaEmpresa(new BigDecimal("68.0"))
                                .imaPromedioSector(new BigDecimal("63.5"))
                                .build(),
                        ImaTendenciaPuntoDTO.builder()
                                .mes("2026-06")
                                .imaEmpresa(new BigDecimal("71.0"))
                                .imaPromedioSector(null)
                                .build()))
                .build();
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void tendenciaValidaDevuelve200ConLaSerie() throws Exception {
        when(imaTendenciaService.obtenerTendencia(nullable(Integer.class), any(UUID.class)))
                .thenReturn(tendencia());

        mockMvc.perform(get("/api/ima/tendencia").principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesesAtras").value(12))
                .andExpect(jsonPath("$.serie.length()").value(2))
                .andExpect(jsonPath("$.serie[0].mes").value("2026-05"))
                .andExpect(jsonPath("$.serie[0].imaEmpresa").value(68.0))
                .andExpect(jsonPath("$.serie[0].imaPromedioSector").value(63.5))
                .andExpect(jsonPath("$.serie[1].imaPromedioSector").doesNotExist())
                .andExpect(jsonPath("$.eventos.length()").value(1))
                .andExpect(jsonPath("$.eventos[0].mes").value("2026-06"))
                .andExpect(jsonPath("$.eventos[0].tipo").value("CRUCE_SECTOR"))
                .andExpect(jsonPath("$.eventos[0].texto").exists());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void usuarioGeneralTieneAcceso() throws Exception {
        when(imaTendenciaService.obtenerTendencia(nullable(Integer.class), any(UUID.class)))
                .thenReturn(tendencia());

        mockMvc.perform(get("/api/ima/tendencia").param("mesesAtras", "6")
                        .principal(principal("ROLE_USUARIO_GENERAL")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void mesesAtrasCeroDevuelve400() throws Exception {
        // La validacion de la ventana vive en el servicio (resolverVentana); el controller
        // delega y deja que la ApiException se traduzca a 400 via el manejador global.
        when(imaTendenciaService.obtenerTendencia(nullable(Integer.class), any(UUID.class)))
                .thenThrow(ApiException.periodoImaInvalido("La ventana debe estar entre 1 y 12 meses."));

        mockMvc.perform(get("/api/ima/tendencia").param("mesesAtras", "0")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void mesesAtrasMayorADoceDevuelve400() throws Exception {
        when(imaTendenciaService.obtenerTendencia(nullable(Integer.class), any(UUID.class)))
                .thenThrow(ApiException.periodoImaInvalido("La ventana debe estar entre 1 y 12 meses."));

        mockMvc.perform(get("/api/ima/tendencia").param("mesesAtras", "13")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void auditorNoTieneAcceso() throws Exception {
        mockMvc.perform(get("/api/ima/tendencia").principal(principal("ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden());
    }
}
