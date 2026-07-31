package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.certificacion.service.FirmanteCredencialService;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCredencialOpenBadges;
import com.piedpiper.carbonhub.certificacion.service.ListaEstadoCredencialesService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Estos endpoints los consume un verificador externo sin sesion, de modo que la
 * prueba deliberadamente no envia autenticacion alguna.
 */
@WebMvcTest(controllers = {CertificacionEmisorController.class, CertificacionController.class},
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(CertificacionEmisorControllerTest.CatalogoTestConfig.class)
class CertificacionEmisorControllerTest {

    @TestConfiguration
    static class CatalogoTestConfig {
        @Bean
        CatalogoTiposCertificacion catalogoTiposCertificacion() {
            return new CatalogoTiposCertificacion();
        }

        @Bean
        GeneradorCredencialOpenBadges generadorCredencialOpenBadges(
                FirmanteCredencialService firmanteCredencialService) {
            return new GeneradorCredencialOpenBadges(
                    firmanteCredencialService, "https://carbonhub.example", "CarbonHub");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirmanteCredencialService firmanteCredencialService;
    @MockitoBean
    private ConsultaCertificacionService consultaCertificacionService;
    @MockitoBean
    private ListaEstadoCredencialesService listaEstadoCredencialesService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void elPerfilDelEmisorEsAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/certificaciones/emisor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Profile"))
                .andExpect(jsonPath("$.name").value("CarbonHub"))
                .andExpect(jsonPath("$.id")
                        .value("https://carbonhub.example/api/certificaciones/emisor"))
                .andExpect(jsonPath("$.['@context'][0]")
                        .value("https://www.w3.org/ns/credentials/v2"))
                .andExpect(jsonPath("$.['@context'][1]")
                        .value("https://purl.imsglobal.org/spec/ob/v3p0/context-3.0.3.json"));
    }

    @Test
    void elJwksEsAccesibleSinAutenticacion() throws Exception {
        when(firmanteCredencialService.jwksPublico())
                .thenReturn(Map.of("keys", java.util.List.of(Map.of("kty", "RSA"))));

        mockMvc.perform(get("/api/certificaciones/emisor/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void laDefinicionDelLogroEsAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/certificaciones/logros/carbono_neutral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Achievement"))
                .andExpect(jsonPath("$.name").value("Carbono Neutral"))
                .andExpect(jsonPath("$.achievementType").value("Certification"))
                .andExpect(jsonPath("$.criteria.narrative").isNotEmpty());
    }

    @Test
    void unLogroDesconocidoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/certificaciones/logros/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void laListaDeEstadoEsAccesibleSinAutenticacion() throws Exception {
        when(listaEstadoCredencialesService.generar()).thenReturn("jwt.de.la.lista");

        mockMvc.perform(get("/api/certificaciones/estado/lista"))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt.de.la.lista"));
    }

    @Test
    void laVerificacionEsAccesibleSinAutenticacion() throws Exception {
        UUID certificacionId = UUID.randomUUID();
        when(consultaCertificacionService.verificarPublica(certificacionId))
                .thenReturn(Map.of("id", "urn:uuid:algo", "name", "Carbono Neutral"));

        mockMvc.perform(get("/api/certificaciones/" + certificacionId + "/verificar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carbono Neutral"));
    }

    @Test
    void laVerificacionDeUnIdInexistenteDevuelve404() throws Exception {
        UUID certificacionId = UUID.randomUUID();
        when(consultaCertificacionService.verificarPublica(certificacionId))
                .thenThrow(ApiException.recursoNoEncontrado("La certificacion no existe."));

        mockMvc.perform(get("/api/certificaciones/" + certificacionId + "/verificar"))
                .andExpect(status().isNotFound());
    }

    /**
     * {@code /api/certificaciones/emisor} tambien encaja en el patron
     * {@code /api/certificaciones/{certificacionId}} de CertificacionController.
     * Con ambos controladores cargados se comprueba que gana la ruta literal:
     * si la resolviera el otro handler, el endpoint publico quedaria detras de
     * {@code @PreAuthorize} y ningun verificador externo podria usarlo.
     */
    @Test
    void laRutaDelEmisorNoLaCapturaElHandlerDeDetallePorId() throws Exception {
        mockMvc.perform(get("/api/certificaciones/emisor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Profile"));

        verifyNoInteractions(consultaCertificacionService);
    }
}
