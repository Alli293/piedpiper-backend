package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CertificacionAdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(CertificacionAdminControllerTest.MethodSecurityTestConfig.class)
class CertificacionAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication ADMIN_PLATAFORMA = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_PLATAFORMA")));
    private static final Authentication ROL_INCORRECTO = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_EMPRESA")));

    private static final String CUERPO = """
            {
              "idAuditoria": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "idEmpresa": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
              "idAuditor": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
              "resultadoAuditoria": "aprobada",
              "fechaAuditoria": "2026-01-10",
              "tipo": "CARBONO_NEUTRAL"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisionCertificacionPort emisionCertificacionPort;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private CertificacionResponseDTO respuesta(boolean recienEmitida) {
        return new CertificacionResponseDTO(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "CARBONO_NEUTRAL", "Carbono Neutral",
                Instant.now(), LocalDate.of(2027, 1, 10), "ACTIVA", true, "jwt.firmado.aqui",
                recienEmitida, "https://carbonhub.example/api/certificaciones/verificar");
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_PLATAFORMA")
    void reintentoQueEmiteDevuelve201() throws Exception {
        when(emisionCertificacionPort.emitirPorAuditoriaAprobada(any()))
                .thenReturn(respuesta(true));

        mockMvc.perform(post("/api/certificaciones/reintentos")
                        .principal(ADMIN_PLATAFORMA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCertificacion").value("Carbono Neutral"))
                .andExpect(jsonPath("$.credencialJwt").value("jwt.firmado.aqui"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_PLATAFORMA")
    void reintentoDeUnaCertificacionYaExistenteDevuelve200() throws Exception {
        when(emisionCertificacionPort.emitirPorAuditoriaAprobada(any()))
                .thenReturn(respuesta(false));

        mockMvc.perform(post("/api/certificaciones/reintentos")
                        .principal(ADMIN_PLATAFORMA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recienEmitida").value(false));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_PLATAFORMA")
    void reintentoConResultadoNoAprobadoDevuelve422() throws Exception {
        when(emisionCertificacionPort.emitirPorAuditoriaAprobada(any()))
                .thenThrow(ApiException.resultadoAuditoriaNoAprobado());

        mockMvc.perform(post("/api/certificaciones/reintentos")
                        .principal(ADMIN_PLATAFORMA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_PLATAFORMA")
    void reintentoSinTipoDevuelve400() throws Exception {
        String sinTipo = CUERPO.replace("""
                ,
                  "tipo": "CARBONO_NEUTRAL"\
                """, "");

        mockMvc.perform(post("/api/certificaciones/reintentos")
                        .principal(ADMIN_PLATAFORMA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinTipo))
                .andExpect(status().isBadRequest());

        verify(emisionCertificacionPort, never()).emitirPorAuditoriaAprobada(any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/certificaciones/reintentos")
                        .principal(ROL_INCORRECTO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());

        verify(emisionCertificacionPort, never()).emitirPorAuditoriaAprobada(any());
    }
}
