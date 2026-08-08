package com.piedpiper.carbonhub.perfilpublico.config;

import com.piedpiper.carbonhub.perfilpublico.controller.PerfilPublicoController;
import com.piedpiper.carbonhub.perfilpublico.controller.PerfilPublicoExceptionHandler;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.EnlacePerfilService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoEvolucionService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que la cabecera Content-Security-Policy se incluye en todas las
 * respuestas del endpoint /api/perfil-publico/** (200, 404 y 500).
 */
@WebMvcTest(controllers = PerfilPublicoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class,
                        com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler.class}))
@Import({ContentSecurityPolicyFilter.class, PerfilPublicoExceptionHandler.class})
class ContentSecurityPolicyFilterTest {

    private static final String CSP_HEADER = "Content-Security-Policy";
    private static final String EXPECTED_POLICY = "default-src 'none'; frame-ancestors 'none'";
    private static final String SLUG = "empresa-verde";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilPublicoConsultaService perfilPublicoConsultaService;
    @MockitoBean
    private PerfilPublicoCertificacionesService perfilPublicoCertificacionesService;
    @MockitoBean
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;
    @MockitoBean
    private EnlacePerfilService enlacePerfilService;
    @MockitoBean
    private PerfilPublicoEvolucionService evolucionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void respuesta200IncluyeCabeceraContentSecurityPolicy() throws Exception {
        PerfilPublicoResponseDTO dto = new PerfilPublicoResponseDTO();
        dto.setNombreEmpresa("Empresa Verde S.A.");
        dto.setSectorIndustrial("Tecnología");
        dto.setPais("Costa Rica");
        dto.setNivelEcologico("Oro");
        dto.setFechaActualizacionNivel(Instant.parse("2025-01-15T10:00:00Z"));
        dto.setCertificacionesVigentes(3);
        dto.setInsigniasActivas(2);

        when(perfilPublicoConsultaService.obtenerPorSlug(SLUG)).thenReturn(dto);

        mockMvc.perform(get("/api/perfil-publico/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(header().string(CSP_HEADER, EXPECTED_POLICY));
    }

    @Test
    void respuesta404IncluyeCabeceraContentSecurityPolicy() throws Exception {
        when(perfilPublicoConsultaService.obtenerPorSlug(SLUG))
                .thenThrow(new PerfilNoEncontradoException(
                        "El perfil que buscas no existe o ya no está disponible."));

        mockMvc.perform(get("/api/perfil-publico/{slug}", SLUG))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CSP_HEADER, EXPECTED_POLICY));
    }

    @Test
    void respuesta500IncluyeCabeceraContentSecurityPolicy() throws Exception {
        when(perfilPublicoConsultaService.obtenerPorSlug(SLUG))
                .thenThrow(new QueryTimeoutException("Timeout simulado"));

        mockMvc.perform(get("/api/perfil-publico/{slug}", SLUG))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(CSP_HEADER, EXPECTED_POLICY));
    }

    @Test
    void rutaAjenaNoIncluyeCabeceraContentSecurityPolicy() throws Exception {
        // Una ruta que no empieza con /api/perfil-publico/ no debería
        // recibir la cabecera CSP del filtro.
        mockMvc.perform(get("/api/otra-ruta"))
                .andExpect(header().doesNotExist(CSP_HEADER));
    }

    @Test
    void respuestaNoContieneSetCookieEnEndpointPublico() throws Exception {
        PerfilPublicoResponseDTO dto = new PerfilPublicoResponseDTO();
        dto.setNombreEmpresa("Empresa Verde S.A.");
        dto.setSectorIndustrial("Tecnología");
        dto.setPais("Costa Rica");
        dto.setNivelEcologico("Oro");
        dto.setFechaActualizacionNivel(Instant.parse("2025-01-15T10:00:00Z"));
        dto.setCertificacionesVigentes(3);
        dto.setInsigniasActivas(2);

        when(perfilPublicoConsultaService.obtenerPorSlug(SLUG)).thenReturn(dto);

        mockMvc.perform(get("/api/perfil-publico/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
                    assertThat(setCookieHeaders).isEmpty();
                });
    }
}
