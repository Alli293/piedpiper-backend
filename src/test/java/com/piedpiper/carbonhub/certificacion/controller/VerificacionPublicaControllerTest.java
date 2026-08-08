package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.exceptions.ApiException;
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

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Igual que {@link CertificacionEmisorControllerTest}: la consumen terceros
 * sin sesion, asi que la prueba deliberadamente no envia autenticacion.
 */
@WebMvcTest(controllers = VerificacionPublicaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class VerificacionPublicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaCertificacionService consultaCertificacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void unCodigoValidoEsAccesibleSinAutenticacionYDevuelveElResultado() throws Exception {
        VerificacionCredencialDTO dto = new VerificacionCredencialDTO();
        dto.setEstado("valida_vigente");
        dto.setTipo("CARBONO_NEUTRAL");
        dto.setNombreCertificacion("Carbono Neutral");
        dto.setEmpresa("EcoCorp");
        dto.setAuditor("Ana Perez");
        dto.setEntidadCertificadora("CarbonHub");
        dto.setFechaEmision(Instant.parse("2026-01-15T00:00:00Z"));
        dto.setFechaVencimiento(LocalDate.of(2027, 1, 15));
        dto.setFechaConsulta(Instant.now());
        when(consultaCertificacionService.verificarPorCodigo("CH-2026-8F4A19KD")).thenReturn(dto);

        mockMvc.perform(get("/api/verificar/CH-2026-8F4A19KD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("valida_vigente"))
                .andExpect(jsonPath("$.empresa").value("EcoCorp"))
                .andExpect(jsonPath("$.entidadCertificadora").value("CarbonHub"));
    }

    @Test
    void unCodigoInexistenteOMalFormadoDevuelve404() throws Exception {
        when(consultaCertificacionService.verificarPorCodigo("CH-2026-NOEXISTE"))
                .thenThrow(ApiException.recursoNoEncontrado("Credencial no encontrada."));

        mockMvc.perform(get("/api/verificar/CH-2026-NOEXISTE"))
                .andExpect(status().isNotFound());
    }
}
