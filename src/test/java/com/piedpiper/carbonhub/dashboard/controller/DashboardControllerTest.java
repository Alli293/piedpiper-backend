package com.piedpiper.carbonhub.dashboard.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.dashboard.models.dtos.CalendarioVencimientosResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertificacionVencimientoDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.CalendarioVencimientosService;
import com.piedpiper.carbonhub.dashboard.service.DashboardCertificacionesService;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

@WebMvcTest(controllers = DashboardController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                }))
@AutoConfigureMockMvc(addFilters = false)
@Import(DashboardControllerTest.MethodSecurityTestConfig.class)
class DashboardControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardHuellaService dashboardHuellaService;
    @MockitoBean
    private DashboardCertificacionesService dashboardCertificacionesService;
    @MockitoBean
    private CalendarioVencimientosService calendarioVencimientosService;

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerHuellaMesActualDevuelve200() throws Exception {
        when(dashboardHuellaService.obtenerResumen(UUID.fromString(USUARIO_ID), "mes_actual", 2021))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        new BigDecimal("5.2360"),
                        new BigDecimal("30.9"),
                        true
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("periodo", "mes_actual")
                        .param("anio", "2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huellaTotalT").value(5.2360))
                .andExpect(jsonPath("$.variacionPorcentual").value(30.9))
                .andExpect(jsonPath("$.tieneDatos").value(true));

        verify(dashboardHuellaService).obtenerResumen(UUID.fromString(USUARIO_ID), "mes_actual", 2021);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void periodoInvalidoUsaMesActualPorDefecto() throws Exception {
        when(dashboardHuellaService.obtenerResumen(UUID.fromString(USUARIO_ID), "otro", 2021))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        BigDecimal.ZERO,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("periodo", "otro")
                        .param("anio", "2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoSeleccionado").value("mes_actual"))
                .andExpect(jsonPath("$.tieneDatos").value(false));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void periodoOmitidoUsaMesActualPorDefecto() throws Exception {
        when(dashboardHuellaService.obtenerResumen(UUID.fromString(USUARIO_ID), null, null))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        BigDecimal.ZERO,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoSeleccionado").value("mes_actual"));

        verify(dashboardHuellaService).obtenerResumen(eq(UUID.fromString(USUARIO_ID)), isNull(), isNull());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/dashboard/huella")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO"))
                        .param("periodo", "mes_actual"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerCertificacionesDevuelve200ConLosTresConteos() throws Exception {
        when(dashboardCertificacionesService.obtenerResumen(UUID.fromString(USUARIO_ID)))
                .thenReturn(new ResumenCertificacionesDashboardResponseDTO(5, 3, 1));

        mockMvc.perform(get("/api/dashboard/certificaciones")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activas").value(5))
                .andExpect(jsonPath("$.proximasAVencer").value(3))
                .andExpect(jsonPath("$.vencidas").value(1));

        verify(dashboardCertificacionesService).obtenerResumen(UUID.fromString(USUARIO_ID));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void obtenerCertificacionesRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/dashboard/certificaciones")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerCalendarioDevuelve200ConElMapaDeVencimientos() throws Exception {
        CertificacionVencimientoDTO vencimiento = new CertificacionVencimientoDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "Carbono Neutral", "30_dias");
        when(calendarioVencimientosService.obtenerCalendario(UUID.fromString(USUARIO_ID), "2026-07"))
                .thenReturn(new CalendarioVencimientosResponseDTO(
                        "2026-07", Map.of("2026-07-18", List.of(vencimiento))));

        mockMvc.perform(get("/api/dashboard/calendario")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("mes", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesVisualizado").value("2026-07"))
                .andExpect(jsonPath("$.vencimientosPorFecha['2026-07-18'][0].nombre").value("Carbono Neutral"))
                .andExpect(jsonPath("$.vencimientosPorFecha['2026-07-18'][0].urgencia").value("30_dias"));

        verify(calendarioVencimientosService).obtenerCalendario(UUID.fromString(USUARIO_ID), "2026-07");
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerCalendarioSinParametroMesLoPasaComoNull() throws Exception {
        when(calendarioVencimientosService.obtenerCalendario(UUID.fromString(USUARIO_ID), null))
                .thenReturn(new CalendarioVencimientosResponseDTO("2026-07", Map.of()));

        mockMvc.perform(get("/api/dashboard/calendario")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk());

        verify(calendarioVencimientosService).obtenerCalendario(eq(UUID.fromString(USUARIO_ID)), isNull());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void obtenerCalendarioRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/dashboard/calendario")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }
}