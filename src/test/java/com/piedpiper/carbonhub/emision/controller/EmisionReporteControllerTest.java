package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO.PuntoMensual;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO.ResumenCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.service.EmisionComparacionService;
import com.piedpiper.carbonhub.emision.service.EmisionEvolucionService;
import com.piedpiper.carbonhub.emision.service.EmisionResumenService;
import com.piedpiper.carbonhub.emision.service.ReporteHuellaPdfService;
import com.piedpiper.carbonhub.exceptions.ApiException;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmisionReporteController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EmisionReporteControllerTest.MethodSecurityTestConfig.class)
class EmisionReporteControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisionResumenService emisionResumenService;
    @MockitoBean
    private EmisionComparacionService emisionComparacionService;
    @MockitoBean
    private EmisionEvolucionService emisionEvolucionService;
    @MockitoBean
    private ReporteHuellaPdfService reporteHuellaPdfService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String ADMIN_USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String GENERAL_USUARIO_ID = "db2ed1e7-6719-4595-844e-68efffe146cf";

    private TestingAuthenticationToken principal(String usuarioId, String authority) {
        return new TestingAuthenticationToken(usuarioId, "password", authority);
    }

    private static Authentication principalDe(String usuarioId) {
        return new UsernamePasswordAuthenticationToken(usuarioId, null);
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void comparacionEmisionesDevuelve200() throws Exception {
        ComparacionEmisionesResponseDTO response = new ComparacionEmisionesResponseDTO(
                2026,
                new BigDecimal("30.0000"),
                new BigDecimal("50.0000"),
                new BigDecimal("60.0"),
                "dentro",
                null,
                List.of());
        when(emisionComparacionService.comparar(any(), eq(2026))).thenReturn(response);

        mockMvc.perform(get("/api/emisiones/comparacion")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huellaAcumuladaT").value(30.0000))
                .andExpect(jsonPath("$.limiteT").value(50.0000))
                .andExpect(jsonPath("$.porcentajeConsumido").value(60.0))
                .andExpect(jsonPath("$.estado").value("dentro"))
                .andExpect(jsonPath("$.categorias").isArray());

        verify(emisionComparacionService).comparar(any(), eq(2026));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void comparacionUsuarioSinEmpresaDevuelve422() throws Exception {
        when(emisionComparacionService.comparar(any(), eq(2026)))
                .thenThrow(ApiException.empresaNoConfigurada());

        mockMvc.perform(get("/api/emisiones/comparacion")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("anio", "2026"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void exportarReportePdfDevuelveArchivoDescargable() throws Exception {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        when(reporteHuellaPdfService.generar(any(), eq(2026), eq(7))).thenReturn(pdf);
        when(reporteHuellaPdfService.nombreArchivo(2026, 7)).thenReturn("reporte-huella-2026-07.pdf");

        mockMvc.perform(get("/api/emisiones/reporte/pdf")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("anio", "2026")
                        .param("mes", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-huella-2026-07.pdf\""))
                .andExpect(content().bytes(pdf));

        verify(reporteHuellaPdfService).generar(any(), eq(2026), eq(7));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionConAnioValidoDevuelve200ConDocePuntos() throws Exception {
        EvolucionMensualResponseDTO dto = crearEvolucionConDatos(2026);
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
    void resumenComoAdministradorDevuelve200ConLaEstructuraEsperada() throws Exception {
        when(emisionResumenService.resumen(eq(2026), eq(null), any())).thenReturn(resumenValido());

        mockMvc.perform(get("/api/emisiones/resumen").param("anio", "2026")
                        .principal(principalDe("41ce47ab-a46c-4306-8c46-2688dc97fa73")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.totalKg").value(1000.0))
                .andExpect(jsonPath("$.totalT").value(1.0))
                .andExpect(jsonPath("$.categorias.length()").value(4))
                .andExpect(jsonPath("$.categorias[0].categoria").value("ELECTRICIDAD"))
                .andExpect(jsonPath("$.categorias[0].totalKg").value(500.0))
                .andExpect(jsonPath("$.categorias[0].porcentaje").value(50.0))
                .andExpect(jsonPath("$.categorias[3].categoria").value("ENVIO"));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "USUARIO_GENERAL")
    void resumenComoUsuarioGeneralDevuelve200() throws Exception {
        when(emisionResumenService.resumen(eq(2026), eq(3), any())).thenReturn(resumenValido());

        mockMvc.perform(get("/api/emisiones/resumen").param("anio", "2026").param("mes", "3")
                        .principal(principalDe("db2ed1e7-6719-4595-844e-68efffe146cf")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionSinParametroAnioUsaAnioActualDevuelve200() throws Exception {
        EvolucionMensualResponseDTO dto = crearEvolucionVacia(2026);
        when(emisionEvolucionService.obtenerEvolucion(any(), any(UUID.class))).thenReturn(dto);

        mockMvc.perform(get("/api/emisiones/evolucion")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.serie.length()").value(12));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void resumenConMesFueraDeRangoDevuelve400() throws Exception {
        when(emisionResumenService.resumen(eq(2026), eq(13), any()))
                .thenThrow(ApiException.mesInvalido());

        mockMvc.perform(get("/api/emisiones/resumen").param("anio", "2026").param("mes", "13")
                        .principal(principalDe("41ce47ab-a46c-4306-8c46-2688dc97fa73")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El mes debe estar entre 1 y 12."));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionConMesesSinDatosDevuelveCero() throws Exception {
        EvolucionMensualResponseDTO dto = crearEvolucionVacia(2020);
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
                        .principal(principal(GENERAL_USUARIO_ID, "ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void evolucionConAnioInvalidoDevuelve400() throws Exception {
        when(emisionEvolucionService.obtenerEvolucion(any(), any(UUID.class)))
                .thenThrow(ApiException.anioInvalido());

        mockMvc.perform(get("/api/emisiones/evolucion").param("anio", "1800")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void resumenSinAnioDevuelve400() throws Exception {
        mockMvc.perform(get("/api/emisiones/resumen")
                        .principal(principalDe("41ce47ab-a46c-4306-8c46-2688dc97fa73")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void comparacionConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/emisiones/comparacion")
                        .param("anio", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    private static EmisionResumenResponseDTO resumenValido() {
        return EmisionResumenResponseDTO.builder()
                .anio(2026)
                .totalKg(new BigDecimal("1000.000"))
                .totalT(new BigDecimal("1.000"))
                .categorias(List.of(
                        categoria(CategoriaEmision.ELECTRICIDAD, "500.000", "50.0"),
                        categoria(CategoriaEmision.FLOTA, "300.000", "30.0"),
                        categoria(CategoriaEmision.VUELO, "0", "0.0"),
                        categoria(CategoriaEmision.ENVIO, "200.000", "20.0")))
                .build();
    }

    private static ResumenCategoriaDTO categoria(CategoriaEmision categoria, String totalKg,
                                                 String porcentaje) {
        return ResumenCategoriaDTO.builder()
                .categoria(categoria)
                .totalKg(new BigDecimal(totalKg))
                .porcentaje(new BigDecimal(porcentaje))
                .build();
    }

    private EvolucionMensualResponseDTO crearEvolucionConDatos(int anio) {
        List<PuntoMensual> serie = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            BigDecimal valor = (mes == 1 || mes == 3 || mes == 7)
                    ? new BigDecimal("150.500")
                    : BigDecimal.ZERO;
            serie.add(new PuntoMensual(mes, valor));
        }
        return new EvolucionMensualResponseDTO(anio, serie);
    }

    private EvolucionMensualResponseDTO crearEvolucionVacia(int anio) {
        List<PuntoMensual> serie = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            serie.add(new PuntoMensual(mes, BigDecimal.ZERO));
        }
        return new EvolucionMensualResponseDTO(anio, serie);
    }
}
