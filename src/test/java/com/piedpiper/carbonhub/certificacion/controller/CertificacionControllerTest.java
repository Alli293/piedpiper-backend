package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CertificacionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(CertificacionControllerTest.MethodSecurityTestConfig.class)
class CertificacionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication ADMIN_EMPRESA = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_EMPRESA")));
    private static final Authentication ROL_INCORRECTO = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_GENERAL")));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaCertificacionService consultaCertificacionService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private CertificacionResponseDTO respuesta(String nombre) {
        return new CertificacionResponseDTO(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "CARBONO_NEUTRAL", nombre,
                Instant.now(), LocalDate.of(2027, 1, 10), "ACTIVA", true, "jwt.firmado.aqui", false,
                "https://carbonhub.example/api/certificaciones/verificar");
    }

    private CertificacionResumenResponseDTO resumen(String nombre) {
        return new CertificacionResumenResponseDTO(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "CARBONO_NEUTRAL", nombre,
                Instant.now(), LocalDate.of(2027, 1, 10), "ACTIVA", true,
                "https://carbonhub.example/api/certificaciones/verificar");
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void listarDevuelve200ConLasCertificacionesDeLaEmpresa() throws Exception {
        when(consultaCertificacionService.listar(any()))
                .thenReturn(List.of(resumen("Carbono Neutral"), resumen("Inventario de GEI")));

        mockMvc.perform(get("/api/certificaciones").principal(ADMIN_EMPRESA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCertificacion").value("Carbono Neutral"))
                .andExpect(jsonPath("$[0].credencialJwt").doesNotExist())
                .andExpect(jsonPath("$[1].nombreCertificacion").value("Inventario de GEI"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void detalleDevuelve200ConLaCredencial() throws Exception {
        when(consultaCertificacionService.detalle(any(), any()))
                .thenReturn(respuesta("Carbono Neutral"));

        mockMvc.perform(get("/api/certificaciones/" + UUID.randomUUID()).principal(ADMIN_EMPRESA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credencialJwt").value("jwt.firmado.aqui"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void detalleDeOtraEmpresaDevuelve404YNoRevelaSuExistencia() throws Exception {
        when(consultaCertificacionService.detalle(any(), any()))
                .thenThrow(ApiException.recursoNoEncontrado("La certificacion no existe."));

        mockMvc.perform(get("/api/certificaciones/" + UUID.randomUUID()).principal(ADMIN_EMPRESA))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void descargarJsonLdDevuelve200ConContentDispositionYTipoVcLdJson() throws Exception {
        UUID certificacionId = UUID.randomUUID();
        when(consultaCertificacionService.descargarJsonLd(any(), any()))
                .thenReturn(Map.of("id", "urn:uuid:algo", "name", "Carbono Neutral"));

        mockMvc.perform(get("/api/certificaciones/" + certificacionId + "/jsonld").principal(ADMIN_EMPRESA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vc+ld+json"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"certificacion-" + certificacionId + ".jsonld\""))
                .andExpect(jsonPath("$.name").value("Carbono Neutral"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void descargarJsonLdDeOtraEmpresaDevuelve404YNoRevelaSuExistencia() throws Exception {
        when(consultaCertificacionService.descargarJsonLd(any(), any()))
                .thenThrow(ApiException.recursoNoEncontrado("La certificacion no existe."));

        mockMvc.perform(get("/api/certificaciones/" + UUID.randomUUID() + "/jsonld").principal(ADMIN_EMPRESA))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_GENERAL")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/certificaciones").principal(ROL_INCORRECTO))
                .andExpect(status().isForbidden());

        verify(consultaCertificacionService, never()).listar(any());
    }
}
