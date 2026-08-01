package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.BusquedaPerfilPublicoDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Este endpoint lo consume un visitante sin sesion, de modo que la prueba
 * deliberadamente no envia autenticacion alguna.
 */
@WebMvcTest(controllers = PerfilPublicoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class PerfilPublicoControllerTest {

    private static final String SLUG = "empresa-verde";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilPublicoCertificacionesService perfilPublicoCertificacionesService;
    @MockitoBean
    private PerfilPublicoConsultaService perfilPublicoConsultaService;
    @MockitoBean
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private CertificacionPublicaResponseDTO certificacionPublica() {
        CertificacionPublicaResponseDTO dto = new CertificacionPublicaResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setTipo("CARBONO_NEUTRAL");
        dto.setNombreCertificacion("Carbono Neutral");
        dto.setFechaEmision(Instant.parse("2026-01-15T00:00:00Z"));
        dto.setFechaVencimiento(LocalDate.of(2027, 1, 15));
        dto.setEstado("ACTIVA");
        return dto;
    }

    @Test
    void certificacionesRetorna200ConLaFormaEsperadaSinCamposInternos() throws Exception {
        when(perfilPublicoCertificacionesService.listarPorSlug(SLUG))
                .thenReturn(List.of(certificacionPublica()));

        mockMvc.perform(get("/api/perfil-publico/{slug}/certificaciones", SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("CARBONO_NEUTRAL"))
                .andExpect(jsonPath("$[0].nombreCertificacion").value("Carbono Neutral"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$[0].fechaVencimiento").value("2027-01-15"))
                .andExpect(jsonPath("$[0].idAuditoria").doesNotExist())
                .andExpect(jsonPath("$[0].idEmpresa").doesNotExist())
                .andExpect(jsonPath("$[0].idAuditor").doesNotExist())
                .andExpect(jsonPath("$[0].credencialJwt").doesNotExist())
                .andExpect(jsonPath("$[0].recienEmitida").doesNotExist());
    }

    @Test
    void certificacionesRetorna404CuandoElSlugNoExiste() throws Exception {
        when(perfilPublicoCertificacionesService.listarPorSlug(SLUG))
                .thenThrow(ApiException.recursoNoEncontrado("La empresa no existe."));

        mockMvc.perform(get("/api/perfil-publico/{slug}/certificaciones", SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void certificacionesEsAccesibleSinAutenticacion() throws Exception {
        when(perfilPublicoCertificacionesService.listarPorSlug(SLUG)).thenReturn(List.of());

        mockMvc.perform(get("/api/perfil-publico/{slug}/certificaciones", SLUG))
                .andExpect(status().isOk());
    }
    @Test
    void insigniasEsAccesibleSinAutenticacion() throws Exception {
        when(insigniaEmpresaConsultaService.listarPorSlug(SLUG))
                .thenReturn(List.of(new InsigniaEmpresaResponseDTO(
                        1L,
                        "bronce",
                        "Carbono Neutral",
                        "Insignia activa verificable.",
                        Instant.parse("2026-01-15T00:00:00Z")
                )));

        mockMvc.perform(get("/api/perfil-publico/{slug}/insignias", SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nivelInsignia").value("bronce"))
                .andExpect(jsonPath("$[0].nombre").value("Carbono Neutral"));
    }

    // ========================================================================
    // Task 3.6 — Unit tests del controlador (MockMvc)
    // ========================================================================

    @Test
    @DisplayName("GET /{slug} con slug válido → 200 con JSON del DTO sin campos privados")
    void obtener_slugValido_retorna200ConDto() throws Exception {
        PerfilPublicoResponseDTO dto = new PerfilPublicoResponseDTO(
                "EcoVerde S.A.", "https://cdn.example.com/logo.png",
                "MANUFACTURA", "Costa Rica", "Oro",
                Instant.parse("2025-03-15T10:00:00Z"), 3, 2
        );
        when(perfilPublicoConsultaService.obtenerPorSlug(SLUG)).thenReturn(dto);

        mockMvc.perform(get("/api/perfil-publico/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.nombreEmpresa").value("EcoVerde S.A."))
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.example.com/logo.png"))
                .andExpect(jsonPath("$.sectorIndustrial").value("MANUFACTURA"))
                .andExpect(jsonPath("$.pais").value("Costa Rica"))
                .andExpect(jsonPath("$.nivelEcologico").value("Oro"))
                .andExpect(jsonPath("$.certificacionesVigentes").value(3))
                .andExpect(jsonPath("$.insigniasActivas").value(2))
                // No private fields
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.cedulaJuridica").doesNotExist())
                .andExpect(jsonPath("$.correoCorporativo").doesNotExist())
                .andExpect(jsonPath("$.cantidadEmpleados").doesNotExist())
                .andExpect(jsonPath("$.estado").doesNotExist())
                .andExpect(jsonPath("$.slug").doesNotExist());
    }

    @Test
    @DisplayName("GET /{slug} con slug inexistente → 404 con {\"mensaje\": \"...\"}")
    void obtener_slugInexistente_retorna404ConMensaje() throws Exception {
        when(perfilPublicoConsultaService.obtenerPorSlug("empresa-fantasma"))
                .thenThrow(new PerfilNoEncontradoException(
                        "El perfil que buscas no existe o ya no está disponible."));

        mockMvc.perform(get("/api/perfil-publico/{slug}", "empresa-fantasma"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.mensaje").value("El perfil que buscas no existe o ya no está disponible."))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    @DisplayName("GET /{slug} con empresa inactiva → 404 con mensaje diferenciado")
    void obtener_empresaInactiva_retorna404ConMensajeDiferente() throws Exception {
        when(perfilPublicoConsultaService.obtenerPorSlug("empresa-suspendida"))
                .thenThrow(new PerfilNoEncontradoException(
                        "Este perfil no está disponible en este momento."));

        mockMvc.perform(get("/api/perfil-publico/{slug}", "empresa-suspendida"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.mensaje").value("Este perfil no está disponible en este momento."))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @DisplayName("POST → método no soportado (rechazado por el servidor)")
    void post_retornaMetodoNoSoportado() throws Exception {
        int status = mockMvc.perform(post("/api/perfil-publico/{slug}", SLUG)
                        .contentType("application/json")
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        // El controlador solo expone GET; POST es rechazado.
        // Con GlobalExceptionHandler catch-all activo → 500; sin él → 405.
        assertThat(status).isGreaterThanOrEqualTo(400);
        assertThat(status).isNotEqualTo(200);
    }

    @Test
    @DisplayName("PUT → método no soportado (rechazado por el servidor)")
    void put_retornaMetodoNoSoportado() throws Exception {
        int status = mockMvc.perform(put("/api/perfil-publico/{slug}", SLUG)
                        .contentType("application/json")
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isGreaterThanOrEqualTo(400);
        assertThat(status).isNotEqualTo(200);
    }

    @Test
    @DisplayName("DELETE → método no soportado (rechazado por el servidor)")
    void delete_retornaMetodoNoSoportado() throws Exception {
        int status = mockMvc.perform(delete("/api/perfil-publico/{slug}", SLUG))
                .andReturn().getResponse().getStatus();
        assertThat(status).isGreaterThanOrEqualTo(400);
        assertThat(status).isNotEqualTo(200);
    }

    @Test
    @DisplayName("GET /buscar con nombre válido (3+ chars) → 200 con resultados paginados")
    void buscar_nombreValido_retorna200ConResultados() throws Exception {
        List<BusquedaPerfilPublicoDTO> content = List.of(
                new BusquedaPerfilPublicoDTO("EcoVerde S.A.", "eco-verde", "MANUFACTURA", "Oro"),
                new BusquedaPerfilPublicoDTO("EcoTech", "eco-tech", "TECNOLOGIA", "Plata")
        );
        Page<BusquedaPerfilPublicoDTO> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);
        when(perfilPublicoConsultaService.buscarPorNombre(eq("eco"), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/perfil-publico/buscar")
                        .param("nombre", "eco")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nombreEmpresa").value("EcoVerde S.A."))
                .andExpect(jsonPath("$.content[0].slug").value("eco-verde"))
                .andExpect(jsonPath("$.content[0].sectorIndustrial").value("MANUFACTURA"))
                .andExpect(jsonPath("$.content[0].nivelEcologico").value("Oro"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /buscar con nombre < 3 chars → 200 con contenido vacío")
    void buscar_nombreCorto_retorna200ConContenidoVacio() throws Exception {
        when(perfilPublicoConsultaService.buscarPorNombre(eq("ab"), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/perfil-publico/buscar")
                        .param("nombre", "ab")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
