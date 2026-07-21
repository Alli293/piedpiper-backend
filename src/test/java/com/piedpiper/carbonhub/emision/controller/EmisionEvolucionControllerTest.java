package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO.PuntoMensual;
import com.piedpiper.carbonhub.emision.service.EmisionConsultaService;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;
import com.piedpiper.carbonhub.emision.service.EmisionEnvioService;
import com.piedpiper.carbonhub.emision.service.EmisionEvolucionService;
import com.piedpiper.carbonhub.emision.service.EmisionFlotaService;
import com.piedpiper.carbonhub.emision.service.EmisionVueloService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmisionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EmisionEvolucionControllerTest.MethodSecurityTestConfig.class)
class EmisionEvolucionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisionElectricidadService emisionElectricidadService;
    @MockitoBean
    private EmisionFlotaService emisionFlotaService;
    @MockitoBean
    private EmisionEnvioService emisionEnvioService;
    @MockitoBean
    private EmisionVueloService emisionVueloService;
    @MockitoBean
    private EmisionConsultaService emisionConsultaService;
    @MockitoBean
    private EmisionEvolucionService emisionEvolucionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String ADMIN_USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    private TestingAuthenticationToken principal(String usuarioId, String authority) {
        return new TestingAuthenticationToken(usuarioId, "password", authority);
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionConAnioValidoDevuelve200ConDocePuntos() throws Exception {
        EvolucionMensualDTO dto = crearEvolucionConDatos(2026);
        when(emisionEvolucionService.obtenerEvolucion(any(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/emisiones/evolucion").param("anio", "2026")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.serie.length()").value(12))
                .andExpect(jsonPath("$.serie[0].mes").value(1))
                .andExpect(jsonPath("$.serie[11].mes").value(12));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionSinParametroAnioUsaAnioActualDevuelve200() throws Exception {
        EvolucionMensualDTO dto = crearEvolucionVacia(2026);
        when(emisionEvolucionService.obtenerEvolucion(any(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/emisiones/evolucion")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.serie.length()").value(12));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionConMesesSinDatosDevuelveCero() throws Exception {
        EvolucionMensualDTO dto = crearEvolucionVacia(2020);
        when(emisionEvolucionService.obtenerEvolucion(any(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/emisiones/evolucion").param("anio", "2020")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalCarbonKg").value(0))
                .andExpect(jsonPath("$.serie[5].totalCarbonKg").value(0))
                .andExpect(jsonPath("$.serie[11].totalCarbonKg").value(0));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void evolucionConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/emisiones/evolucion").param("anio", "2026")
                        .principal(new TestingAuthenticationToken(
                                "db2ed1e7-6719-4595-844e-68efffe146cf", "password", "ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden());
    }

    private EvolucionMensualDTO crearEvolucionConDatos(int anio) {
        List<PuntoMensual> serie = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            BigDecimal valor = (mes == 1 || mes == 3 || mes == 7)
                    ? new BigDecimal("150.500")
                    : BigDecimal.ZERO;
            serie.add(PuntoMensual.builder().mes(mes).totalCarbonKg(valor).build());
        }
        return EvolucionMensualDTO.builder().anio(anio).serie(serie).build();
    }

    private EvolucionMensualDTO crearEvolucionVacia(int anio) {
        List<PuntoMensual> serie = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            serie.add(PuntoMensual.builder().mes(mes).totalCarbonKg(BigDecimal.ZERO).build());
        }
        return EvolucionMensualDTO.builder().anio(anio).serie(serie).build();
    }
}
