package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.service.DocumentoRespaldoDescargaService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.common.NombresArchivo;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentoRespaldoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(DocumentoRespaldoControllerTest.MethodSecurityTestConfig.class)
class DocumentoRespaldoControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String SOLICITUD_ID = "9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f";
    private static final String DOCUMENTO_ID = "0b1c2d3e-4f50-4162-8374-859607182930";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoRespaldoDescargaService documentoRespaldoDescargaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void devuelveElPdfComoAdjuntoParaEvitarContenidoActivoEnElNavegador() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenReturn(documento("inventario-2025.pdf"));

        mockMvc.perform(peticion())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("inventario-2025.pdf")));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void elAuditorTambienPuedePrevisualizarlo() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenReturn(documento("inventario-2025.pdf"));

        mockMvc.perform(peticion()).andExpect(status().isOk());
    }

    /**
     * El nombre lo elige quien sube el archivo. Con un salto de linea sin sanear, el valor cierra la
     * cabecera y lo que sigue se interpreta como una cabecera nueva de la respuesta.
     */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unNombreConSaltosDeLineaNoInyectaCabeceras() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenReturn(documento("malo\r\nSet-Cookie: robado=1.pdf"));

        String disposition = mockMvc.perform(peticion())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.CONTENT_DISPOSITION);

        // Lo que hace segura la cabecera es que no queden saltos de linea: el texto en si puede
        // seguir apareciendo dentro del nombre, pero sin CR/LF no puede abrir una cabecera nueva.
        assertThat(disposition).doesNotContain("\r").doesNotContain("\n");
        assertThat(disposition).contains("malo__Set-Cookie");
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unNombreVacioCaeAlNombrePorDefecto() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenReturn(documento("   "));

        mockMvc.perform(peticion())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(NombresArchivo.NOMBRE_POR_DEFECTO)));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unDocumentoDeOtraSolicitudDevuelve404() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenThrow(ApiException.documentoRespaldoNoEncontrado());

        mockMvc.perform(peticion()).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unaSolicitudAjenaDevuelve403() throws Exception {
        when(documentoRespaldoDescargaService.obtener(any(), any(), any()))
                .thenThrow(ApiException.solicitudAuditoriaAjena());

        mockMvc.perform(peticion()).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void unRolSinRelacionConLaAuditoriaNoLlegaAlEndpoint() throws Exception {
        mockMvc.perform(peticion()).andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.RequestBuilder peticion() {
        return get("/api/auditorias/{idSolicitud}/documentos/{idDocumento}", SOLICITUD_ID, DOCUMENTO_ID)
                .principal(principal());
    }

    private static DocumentoRespaldo documento(String nombreArchivo) {
        return DocumentoRespaldo.builder()
                .nombreArchivo(nombreArchivo)
                .tipoContenido("application/pdf")
                .contenido("%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, "n/a", List.of());
    }
}
