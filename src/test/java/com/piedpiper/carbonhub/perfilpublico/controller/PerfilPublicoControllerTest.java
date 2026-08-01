package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Este endpoint lo consume un visitante sin sesion, de modo que la prueba
 * deliberadamente no envia autenticacion alguna.
 */
@WebMvcTest(controllers = PerfilPublicoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
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
}
