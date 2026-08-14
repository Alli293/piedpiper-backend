package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorRecomendadoResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendacionAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecomendacionAuditoresController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(RecomendacionAuditoresControllerTest.MethodSecurityTestConfig.class)
class RecomendacionAuditoresControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String CUERPO_VALIDO = """
            {"tipoAuditoria":"MANUFACTURA","especialidadBuscada":"MANUFACTURA",
             "zonaGeografica":"SAN_JOSE","soloDisponibles":true}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecomendacionAuditoresService recomendacionAuditoresService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void peticionValidaDevuelve200ConLosCandidatos() throws Exception {
        when(recomendacionAuditoresService.recomendar(any(), any()))
                .thenReturn(new RecomendacionAuditoresResponseDTO(List.of(recomendado()), true));

        mockMvc.perform(peticion(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendaciones[0].nombre").value("Ana Mora"))
                .andExpect(jsonPath("$.recomendaciones[0].justificacion").value("Encaja bien."))
                .andExpect(jsonPath("$.iaDisponible").value(true));
    }

    /** Degradación controlada vista desde el cliente: 200, tarjetas sin justificación y el aviso. */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void siLaIaNoEstaDisponibleDevuelve200ConJustificacionNula() throws Exception {
        AuditorRecomendadoResponseDTO sinJustificacion = recomendado();
        sinJustificacion.setJustificacion(null);
        when(recomendacionAuditoresService.recomendar(any(), any()))
                .thenReturn(new RecomendacionAuditoresResponseDTO(List.of(sinJustificacion), false));

        mockMvc.perform(peticion(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendaciones[0].justificacion").doesNotExist())
                .andExpect(jsonPath("$.iaDisponible").value(false));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void unUsuarioGeneralDeLaEmpresaNoPuedePedirRecomendaciones() throws Exception {
        mockMvc.perform(peticion(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verify(recomendacionAuditoresService, never()).recomendar(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void unAuditorNoPuedePedirRecomendaciones() throws Exception {
        mockMvc.perform(peticion(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verify(recomendacionAuditoresService, never()).recomendar(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unTipoDeAuditoriaFueraDelCatalogoDevuelve400() throws Exception {
        when(recomendacionAuditoresService.recomendar(any(), any()))
                .thenThrow(ApiException.tipoAuditoriaRecomendacionInvalido("BUCEO"));

        mockMvc.perform(peticion("""
                        {"tipoAuditoria":"BUCEO","especialidadBuscada":"MANUFACTURA",
                         "zonaGeografica":"SAN_JOSE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tipo de auditoría 'BUCEO' no es válido."));
    }

    /** Bean Validation corta antes de llegar al servicio: el campo vacío ni siquiera lo consulta. */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unCampoObligatorioVacioDevuelve400SinLlegarAlServicio() throws Exception {
        mockMvc.perform(peticion("""
                        {"tipoAuditoria":"","especialidadBuscada":"MANUFACTURA",
                         "zonaGeografica":"SAN_JOSE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Este campo es obligatorio.")));

        verify(recomendacionAuditoresService, never()).recomendar(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void sinCandidatosDevuelve200ConListaVacia() throws Exception {
        when(recomendacionAuditoresService.recomendar(any(), any()))
                .thenReturn(new RecomendacionAuditoresResponseDTO(List.of(), true));

        mockMvc.perform(peticion(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendaciones").isEmpty());
    }

    private static AuditorRecomendadoResponseDTO recomendado() {
        return new AuditorRecomendadoResponseDTO(
                UUID.randomUUID(), "Ana Mora", null, List.of("Manufactura"),
                BigDecimal.valueOf(4.8), true, 12, "Encaja bien.");
    }

    private static org.springframework.test.web.servlet.RequestBuilder peticion(String cuerpo) {
        return post("/api/auditores/recomendaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .principal(principal());
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, "n/a", List.of());
    }
}
