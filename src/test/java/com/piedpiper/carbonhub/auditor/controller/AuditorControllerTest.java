package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.FiltrarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.service.AuditorDirectorioService;
import com.piedpiper.carbonhub.auditor.service.PerfilPublicoAuditorService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AuditorControllerTest.MethodSecurityTestConfig.class)
class AuditorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditorDirectorioService auditorDirectorioService;
    @MockitoBean
    private PerfilPublicoAuditorService perfilPublicoAuditorService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static TestingAuthenticationToken principal(String rol) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", "ROLE_" + rol);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void listarDevuelve200ConLaPaginaDeAuditores() throws Exception {
        AuditorResumenResponseDTO auditor = new AuditorResumenResponseDTO(
                UUID.randomUUID(), "Ana Mora", null, List.of("AGROINDUSTRIA"),
                new BigDecimal("4.5"), 30, true, 42, 8, "SAN_JOSE");
        when(auditorDirectorioService.listar(any()))
                .thenReturn(new PaginaAuditoresResponseDTO(List.of(auditor), 1, 0, 1));

        mockMvc.perform(get("/api/auditores").principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Ana Mora"))
                .andExpect(jsonPath("$.contenido[0].auditoriasCompletadas").value(42))
                .andExpect(jsonPath("$.totalResultados").value(1))
                .andExpect(jsonPath("$.paginaActual").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void auditorCertificadoTambienPuedeConsultarElDirectorio() throws Exception {
        when(auditorDirectorioService.listar(any()))
                .thenReturn(new PaginaAuditoresResponseDTO(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/auditores").principal(principal("AUDITOR_CERTIFICADO")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void ordenamientoInvalidoDevuelve400() throws Exception {
        when(auditorDirectorioService.listar(any()))
                .thenThrow(ApiException.ordenamientoAuditoresInvalido());

        mockMvc.perform(get("/api/auditores").param("ordenamiento", "POR_PRECIO")
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void losFiltrosDeLaConsultaLleganAlServicio() throws Exception {
        when(auditorDirectorioService.listar(any()))
                .thenReturn(new PaginaAuditoresResponseDTO(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/auditores")
                        .param("especialidades", "AGROINDUSTRIA", "MANUFACTURA")
                        .param("soloDisponibles", "true")
                        .param("zonaGeografica", "SAN_JOSE")
                        .param("calificacionMinima", "4.0")
                        .param("pagina", "2")
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltrarAuditoresRequestDTO> captor =
                ArgumentCaptor.forClass(FiltrarAuditoresRequestDTO.class);
        verify(auditorDirectorioService).listar(captor.capture());
        FiltrarAuditoresRequestDTO filtros = captor.getValue();
        assertThat(filtros.getEspecialidades()).containsExactly("AGROINDUSTRIA", "MANUFACTURA");
        assertThat(filtros.getSoloDisponibles()).isTrue();
        assertThat(filtros.getZonaGeografica()).isEqualTo("SAN_JOSE");
        assertThat(filtros.getCalificacionMinima()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(filtros.getPagina()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void especialidadInvalidaDevuelve400() throws Exception {
        when(auditorDirectorioService.listar(any()))
                .thenThrow(ApiException.especialidadAuditorInvalida("INVALIDA"));

        mockMvc.perform(get("/api/auditores").param("especialidades", "INVALIDA")
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_PLATAFORMA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/auditores").principal(principal("ADMINISTRADOR_PLATAFORMA")))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // Task 7.2 — Unit tests para GET /api/auditores/{auditorId}
    // ========================================================================

    @Test
    @DisplayName("GET /{auditorId} con UUID válido retorna 200 con JSON del DTO")
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerPerfilPublicoConUuidValidoRetorna200ConDto() throws Exception {
        UUID auditorId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        PerfilPublicoAuditorResponseDTO dto = new PerfilPublicoAuditorResponseDTO(
                auditorId,
                "Carlos Ramírez",
                "https://cdn.example.com/foto.png",
                "Auditor certificado con 10 años de experiencia.",
                List.of("AGROINDUSTRIA", "MANUFACTURA"),
                List.of(new CertificacionPublicaDTO("Carbono Neutral", "CarbonHub",
                        LocalDate.of(2027, 6, 15), false)),
                true,
                new BigDecimal("4.7"),
                15,
                42,
                new BigDecimal("2.3"),
                List.of(new DistribucionSectorDTO("AGROINDUSTRIA", new BigDecimal("60.0"))),
                Collections.emptyList()
        );

        when(perfilPublicoAuditorService.obtenerPerfilPublico(auditorId)).thenReturn(dto);

        mockMvc.perform(get("/api/auditores/{auditorId}", auditorId)
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditorId").value(auditorId.toString()))
                .andExpect(jsonPath("$.nombre").value("Carlos Ramírez"))
                .andExpect(jsonPath("$.fotoPerfil").value("https://cdn.example.com/foto.png"))
                .andExpect(jsonPath("$.descripcionProfesional").value("Auditor certificado con 10 años de experiencia."))
                .andExpect(jsonPath("$.especialidades[0]").value("AGROINDUSTRIA"))
                .andExpect(jsonPath("$.especialidades[1]").value("MANUFACTURA"))
                .andExpect(jsonPath("$.certificaciones[0].nombre").value("Carbono Neutral"))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.calificacionPromedio").value(4.7))
                .andExpect(jsonPath("$.totalResenas").value(15))
                .andExpect(jsonPath("$.auditoriasCompletadas").value(42))
                .andExpect(jsonPath("$.tiempoPromedioRespuestaDias").value(2.3))
                .andExpect(jsonPath("$.distribucionSectores[0].sector").value("AGROINDUSTRIA"))
                .andExpect(jsonPath("$.resenas").isArray());
    }

    @Test
    @DisplayName("GET /{auditorId} con UUID inválido retorna 400 con mensaje de error")
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerPerfilPublicoConUuidInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/auditores/{auditorId}", "not-a-uuid")
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos inválidos o incompletos."));
    }

    @Test
    @DisplayName("GET /{auditorId} cuando perfil no existe retorna 404")
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerPerfilPublicoPerfilNoExisteRetorna404() throws Exception {
        UUID auditorId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        when(perfilPublicoAuditorService.obtenerPerfilPublico(auditorId))
                .thenThrow(ApiException.recursoNoEncontrado("El perfil solicitado no está disponible."));

        mockMvc.perform(get("/api/auditores/{auditorId}", auditorId)
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("El perfil solicitado no está disponible."));
    }

    @Test
    @DisplayName("GET /{auditorId} con error inesperado retorna 500")
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void obtenerPerfilPublicoErrorInesperadoRetorna500() throws Exception {
        UUID auditorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(perfilPublicoAuditorService.obtenerPerfilPublico(auditorId))
                .thenThrow(new RuntimeException("Error interno simulado"));

        mockMvc.perform(get("/api/auditores/{auditorId}", auditorId)
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado. Por favor, intenta nuevamente."));
    }
}
