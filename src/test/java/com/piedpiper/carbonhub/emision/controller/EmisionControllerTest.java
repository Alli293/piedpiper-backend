package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionElectricidadResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO.ResumenCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO.CombustibleResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionVueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;
import com.piedpiper.carbonhub.emision.service.EmisionConsultaService;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;
import com.piedpiper.carbonhub.emision.service.EmisionEnvioService;
import com.piedpiper.carbonhub.emision.service.EmisionFlotaService;
import com.piedpiper.carbonhub.emision.service.EmisionResumenService;
import com.piedpiper.carbonhub.emision.service.EmisionVueloService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmisionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EmisionControllerTest.MethodSecurityTestConfig.class)
class EmisionControllerTest {

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
    private EmisionResumenService emisionResumenService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String ADMIN_USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String GENERAL_USUARIO_ID = "db2ed1e7-6719-4595-844e-68efffe146cf";

    private TestingAuthenticationToken principal(String usuarioId, String authority) {
        return new TestingAuthenticationToken(usuarioId, "password", authority);
    }

    private static final String REQUEST_VALIDO = "{\"titulo\":\"Consumo oficina central\","
            + "\"electricityValue\":500,\"electricityUnit\":\"kwh\",\"fechaActividad\":\"2026-07-01\"}";
    private static final String VUELO_REQUEST_VALIDO = "{\"passengers\":2,\"distanceUnit\":\"km\","
            + "\"fechaActividad\":\"2026-07-01\","
            + "\"legs\":[{\"departureAirport\":\"SFO\",\"destinationAirport\":\"YYZ\","
            + "\"cabinClass\":\"economy\"}]}";

    private static final String REQUEST_FLOTA_VALIDO = "{\"titulo\":\"Recorrido Toyota Corolla\","
            + "\"tipoVehiculo\":\"AUTOMOVIL\",\"combustible\":\"GASOLINA\",\"distanceValue\":100,"
            + "\"distanceUnit\":\"km\",\"fechaActividad\":\"2026-07-01\"}";

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroValidoComoAdministradorEmpresaDevuelve201() throws Exception {
        EmisionElectricidadResponseDTO response = EmisionElectricidadResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.ELECTRICIDAD)
                .titulo("Consumo oficina central")
                .fechaActividad(LocalDate.now())
                .electricityValue(new BigDecimal("500"))
                .electricityUnit(UnidadElectricidad.KWH)
                .carbonKg(new BigDecimal("237.5"))
                .carbonMt(new BigDecimal("0.2375"))
                .factorEmisionId("ci-estimate-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(emisionElectricidadService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "USUARIO_GENERAL")
    void registroValidoComoUsuarioGeneralDevuelve201() throws Exception {
        EmisionElectricidadResponseDTO response = EmisionElectricidadResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.ELECTRICIDAD)
                .titulo("Consumo oficina central")
                .fechaActividad(LocalDate.now())
                .electricityValue(new BigDecimal("500"))
                .electricityUnit(UnidadElectricidad.KWH)
                .carbonKg(new BigDecimal("237.5"))
                .carbonMt(new BigDecimal("0.2375"))
                .factorEmisionId("ci-estimate-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(emisionElectricidadService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .principal(principal(GENERAL_USUARIO_ID, "ROLE_USUARIO_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void cuerpoInvalidoDevuelve400() throws Exception {
        String invalido = "{\"titulo\":\"Consumo\",\"electricityValue\":-5,\"electricityUnit\":\"kwh\","
                + "\"fechaActividad\":\"2026-07-01\"}";

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    // --- Tests para /api/emisiones/envio ---

    private static final String ENVIO_REQUEST_VALIDO = "{\"titulo\":\"Envío de mercancía\","
            + "\"weightValue\":200,\"weightUnit\":\"KG\",\"distanceValue\":500,"
            + "\"distanceUnit\":\"KM\",\"transportMethod\":\"TRUCK\",\"fechaActividad\":\"2026-07-01\"}";

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroEnvioValidoComoAdministradorEmpresaDevuelve201() throws Exception {
        EmisionEnvioResponseDTO response = EmisionEnvioResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.ENVIO)
                .titulo("Envío de mercancía")
                .fechaActividad(LocalDate.now())
                .weightValue(new BigDecimal("200"))
                .weightUnit(UnidadPeso.KG)
                .distanceValue(new BigDecimal("500"))
                .distanceUnit(UnidadDistancia.KM)
                .transportMethod(MetodoTransporte.TRUCK)
                .carbonKg(new BigDecimal("35.50"))
                .carbonMt(new BigDecimal("0.036"))
                .factorEmisionId("ci-estimate-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(emisionEnvioService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/envio")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVIO_REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carbonKg").value(35.50))
                .andExpect(jsonPath("$.transportMethod").value("TRUCK"))
                // UnidadDistancia se serializa con @JsonValue: "km", no "KM"
                .andExpect(jsonPath("$.distanceUnit").value("km"));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoEnvioDevuelve403() throws Exception {
        mockMvc.perform(post("/api/emisiones/envio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVIO_REQUEST_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    // --- Tests para /api/emisiones/flota ---

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarTiposVehiculoDevuelve200() throws Exception {
        when(emisionFlotaService.listarTiposVehiculo()).thenReturn(
                List.of(new TipoVehiculoResponseDTO("AUTOMOVIL", "Automóvil",
                        List.of(new CombustibleResponseDTO("GASOLINA", "Gasolina")))));

        mockMvc.perform(get("/api/emisiones/flota/tipos-vehiculo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("AUTOMOVIL"))
                .andExpect(jsonPath("$[0].nombre").value("Automóvil"))
                .andExpect(jsonPath("$[0].combustibles[0].id").value("GASOLINA"));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroFlotaValidoComoAdministradorEmpresaDevuelve201() throws Exception {
        EmisionFlotaResponseDTO response = EmisionFlotaResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.FLOTA)
                .titulo("Recorrido Toyota Corolla")
                .fechaActividad(LocalDate.now())
                .carbonKg(new BigDecimal("18.9"))
                .carbonMt(new BigDecimal("0.0189"))
                .factorEmisionId("climatiq-factor-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .tipoVehiculo(TipoVehiculo.AUTOMOVIL)
                .combustible(Combustible.GASOLINA)
                .distanceValue(new BigDecimal("100"))
                .distanceUnit(UnidadDistancia.KM)
                .build();
        when(emisionFlotaService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/flota")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_FLOTA_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carbonKg").value(18.9))
                .andExpect(jsonPath("$.tipoVehiculo").value("AUTOMOVIL"))
                .andExpect(jsonPath("$.distanceUnit").value("km"));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "USUARIO_GENERAL")
    void registroFlotaValidoComoUsuarioGeneralDevuelve201() throws Exception {
        EmisionFlotaResponseDTO response = EmisionFlotaResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.FLOTA)
                .titulo("Recorrido Toyota Corolla")
                .fechaActividad(LocalDate.now())
                .carbonKg(new BigDecimal("18.9"))
                .carbonMt(new BigDecimal("0.0189"))
                .factorEmisionId("climatiq-factor-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .tipoVehiculo(TipoVehiculo.AUTOMOVIL)
                .combustible(Combustible.GASOLINA)
                .distanceValue(new BigDecimal("100"))
                .distanceUnit(UnidadDistancia.KM)
                .build();
        when(emisionFlotaService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/flota")
                        .principal(principal(GENERAL_USUARIO_ID, "ROLE_USUARIO_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_FLOTA_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "USUARIO_GENERAL")
    void registroEnvioValidoComoUsuarioGeneralDevuelve201() throws Exception {
        EmisionEnvioResponseDTO response = EmisionEnvioResponseDTO.builder()
                .id(UUID.randomUUID())
                .categoria(CategoriaEmision.ENVIO)
                .titulo("Envío de mercancía")
                .fechaActividad(LocalDate.now())
                .weightValue(new BigDecimal("200"))
                .weightUnit(UnidadPeso.KG)
                .distanceValue(new BigDecimal("500"))
                .distanceUnit(UnidadDistancia.KM)
                .transportMethod(MetodoTransporte.TRUCK)
                .carbonKg(new BigDecimal("35.50"))
                .carbonMt(new BigDecimal("0.036"))
                .factorEmisionId("ci-estimate-id")
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(emisionEnvioService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/envio")
                        .principal(principal(GENERAL_USUARIO_ID, "ROLE_USUARIO_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVIO_REQUEST_VALIDO))
                .andExpect(status().isCreated());
    }

    // --- Tests para /api/emisiones/vuelo ---

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarEmisionesDevuelve200() throws Exception {
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setId(UUID.randomUUID());
        response.setCategoria(CategoriaEmision.VUELO);
        response.setTitulo("Viaje aereo SFO-YYZ");
        response.setCarbonKg(new BigDecimal("237.5"));
        when(emisionConsultaService.listar(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/emisiones")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("VUELO"))
                .andExpect(jsonPath("$[0].carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void cuerpoFlotaInvalidoDevuelve400() throws Exception {
        String invalido = "{\"titulo\":\"Recorrido\",\"tipoVehiculo\":\"AUTOMOVIL\",\"combustible\":\"GASOLINA\","
                + "\"distanceValue\":-5,\"distanceUnit\":\"km\",\"fechaActividad\":\"2026-07-01\"}";

        mockMvc.perform(post("/api/emisiones/flota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void obtenerEmisionDevuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setId(id);
        response.setCategoria(CategoriaEmision.VUELO);
        response.setTitulo("Viaje aereo SFO-YYZ");
        when(emisionConsultaService.obtener(eq(id), any())).thenReturn(response);

        mockMvc.perform(get("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.categoria").value("VUELO"));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void obtenerEmisionNoEncontradaDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(emisionConsultaService.obtener(eq(id), any()))
                .thenThrow(ApiException.recursoNoEncontrado("No se encontro la emision solicitada."));

        mockMvc.perform(get("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void actualizarVueloDevuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setId(id);
        response.setCarbonKg(new BigDecimal("237.5"));
        when(emisionVueloService.actualizar(eq(id), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/emisiones/vuelo/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VUELO_REQUEST_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void combinacionTipoVehiculoCombustibleInvalidaDevuelve400() throws Exception {
        when(emisionFlotaService.registrar(any(), any())).thenThrow(ApiException.combinacionVehiculoInvalida());
        String invalido = "{\"titulo\":\"Recorrido camión\",\"tipoVehiculo\":\"CAMION_PESADO\","
                + "\"combustible\":\"DIESEL\",\"distanceValue\":100,\"distanceUnit\":\"km\","
                + "\"fechaActividad\":\"2026-07-01\"}";

        mockMvc.perform(post("/api/emisiones/flota")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void eliminarEmisionDevuelve204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNoContent());

        verify(emisionConsultaService).eliminar(eq(id), any());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void eliminarEmisionNoEncontradaDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(ApiException.recursoNoEncontrado("No se encontro la emision solicitada."))
                .when(emisionConsultaService).eliminar(eq(id), any());

        mockMvc.perform(delete("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoEnFlotaDevuelve403() throws Exception {
        mockMvc.perform(post("/api/emisiones/flota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_FLOTA_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void endpointsNuevosConRolNoAutorizadoDevuelven403() throws Exception {
        UUID id = UUID.randomUUID();
        String mensajeEsperado = "No tiene permisos para realizar esta acción.";

        mockMvc.perform(get("/api/emisiones"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
        mockMvc.perform(get("/api/emisiones/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
        mockMvc.perform(put("/api/emisiones/vuelo/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VUELO_REQUEST_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
        mockMvc.perform(delete("/api/emisiones/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroVueloValidoDevuelve201() throws Exception {
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setCarbonKg(new BigDecimal("237.5"));
        when(emisionVueloService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/vuelo")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VUELO_REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroVueloSinTrayectosDevuelve400() throws Exception {
        String request = "{\"passengers\":2,\"fechaActividad\":\"2026-07-01\",\"legs\":[]}";

        mockMvc.perform(post("/api/emisiones/vuelo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroVueloConCabinaInvalidaDevuelve400() throws Exception {
        String request = "{\"passengers\":2,\"distanceUnit\":\"km\",\"fechaActividad\":\"2026-07-01\","
                + "\"legs\":[{\"departureAirport\":\"SFO\",\"destinationAirport\":\"YYZ\","
                + "\"cabinClass\":\"first\"}]}";

        mockMvc.perform(post("/api/emisiones/vuelo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroVueloConUnidadDistanciaInvalidaDevuelve400() throws Exception {
        String request = "{\"passengers\":2,\"distanceUnit\":\"league\",\"fechaActividad\":\"2026-07-01\","
                + "\"legs\":[{\"departureAirport\":\"SFO\",\"destinationAirport\":\"YYZ\","
                + "\"cabinClass\":\"economy\"}]}";

        mockMvc.perform(post("/api/emisiones/vuelo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroVueloConDemasiadosPasajerosDevuelve400() throws Exception {
        String request = "{\"passengers\":1001,\"distanceUnit\":\"km\",\"fechaActividad\":\"2026-07-01\","
                + "\"legs\":[{\"departureAirport\":\"SFO\",\"destinationAirport\":\"YYZ\","
                + "\"cabinClass\":\"economy\"}]}";

        mockMvc.perform(post("/api/emisiones/vuelo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
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
    void resumenConMesFueraDeRangoDevuelve400() throws Exception {
        when(emisionResumenService.resumen(eq(2026), eq(13), any()))
                .thenThrow(ApiException.mesConsultaInvalido());

        mockMvc.perform(get("/api/emisiones/resumen").param("anio", "2026").param("mes", "13")
                        .principal(principalDe("41ce47ab-a46c-4306-8c46-2688dc97fa73")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El mes debe estar entre 1 y 12."));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void resumenSinAnioDevuelve400() throws Exception {
        mockMvc.perform(get("/api/emisiones/resumen")
                        .principal(principalDe("41ce47ab-a46c-4306-8c46-2688dc97fa73")))
                .andExpect(status().isBadRequest());
    }

    private static Authentication principalDe(String usuarioId) {
        return new UsernamePasswordAuthenticationToken(usuarioId, null);
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

}
