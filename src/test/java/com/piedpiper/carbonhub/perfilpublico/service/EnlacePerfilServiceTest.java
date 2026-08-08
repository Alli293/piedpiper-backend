package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.exceptions.SlugCambiadoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;
import com.piedpiper.carbonhub.perfilpublico.models.entities.SlugHistorico;
import com.piedpiper.carbonhub.perfilpublico.repository.SlugHistoricoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del EnlacePerfilService.
 * Validates: Requirements 11.1
 */
@ExtendWith(MockitoExtension.class)
class EnlacePerfilServiceTest {

    private static final String BASE_URL = "https://carbonhub.app";
    private static final String OG_IMAGEN_FALLBACK = "https://carbonhub.app/images/og-default.png";

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private SlugHistoricoRepository slugHistoricoRepository;

    @Mock
    private QrGeneradorService qrGeneradorService;

    private EnlacePerfilService service;

    @BeforeEach
    void setUp() {
        service = new EnlacePerfilService(
                empresaRepository,
                slugHistoricoRepository,
                qrGeneradorService,
                BASE_URL,
                OG_IMAGEN_FALLBACK
        );
    }

    // ========================================================================
    // URL canónica
    // ========================================================================

    @Nested
    @DisplayName("Construcción de URL canónica")
    class UrlCanonica {

        @Test
        @DisplayName("URL canónica sigue patrón {baseUrl}/empresa/{slug}/reputacion")
        void construyeUrlCanonica_patronCorrecto() {
            Empresa empresa = buildEmpresaActiva("eco-verde", "EcoVerde S.A.", "ORO", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("eco-verde", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,abc123");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("eco-verde");

            assertThat(dto.getUrlCanonica()).isEqualTo("https://carbonhub.app/empresa/eco-verde/reputacion");
        }

        @Test
        @DisplayName("URL canónica con baseUrl sin trailing slash y slug con guiones")
        void construyeUrlCanonica_slugConGuiones() {
            Empresa empresa = buildEmpresaActiva("mi-empresa-verde-123", "Mi Empresa Verde", "PLATA", null);
            when(empresaRepository.findBySlugAndEstado("mi-empresa-verde-123", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,xyz");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("mi-empresa-verde-123");

            assertThat(dto.getUrlCanonica()).isEqualTo("https://carbonhub.app/empresa/mi-empresa-verde-123/reputacion");
        }

        @Test
        @DisplayName("ogUrl es idéntico a urlCanonica")
        void ogUrl_igualAUrlCanonica() {
            Empresa empresa = buildEmpresaActiva("test-slug", "Test S.A.", "BRONCE", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("test-slug", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,qr");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("test-slug");

            assertThat(dto.getOgUrl()).isEqualTo(dto.getUrlCanonica());
        }

        @Test
        @DisplayName("URL canónica con baseUrl diferente (entorno local)")
        void construyeUrlCanonica_baseUrlLocal() {
            EnlacePerfilService serviceLocal = new EnlacePerfilService(
                    empresaRepository, slugHistoricoRepository, qrGeneradorService,
                    "http://localhost:4200", OG_IMAGEN_FALLBACK
            );

            Empresa empresa = buildEmpresaActiva("local-slug", "Local Corp", "ORO", null);
            when(empresaRepository.findBySlugAndEstado("local-slug", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,local");

            EnlacePerfilDTO dto = serviceLocal.obtenerEnlacePerfil("local-slug");

            assertThat(dto.getUrlCanonica()).isEqualTo("http://localhost:4200/empresa/local-slug/reputacion");
        }
    }

    // ========================================================================
    // Metadatos Open Graph
    // ========================================================================

    @Nested
    @DisplayName("Generación de metadatos OG")
    class MetadatosOg {

        @Test
        @DisplayName("ogTitulo sigue formato: {nombreEmpresa} — Perfil de Reputación Ecológica | CarbonHub")
        void ogTitulo_formatoCorrecto() {
            Empresa empresa = buildEmpresaActiva("verde-corp", "Verde Corp S.A.", "PLATINO", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("verde-corp", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("verde-corp");

            assertThat(dto.getOgTitulo())
                    .isEqualTo("Verde Corp S.A. — Perfil de Reputación Ecológica | CarbonHub");
        }

        @Test
        @DisplayName("ogDescripcion sigue formato con nivel ecológico y nombre de empresa")
        void ogDescripcion_formatoCorrecto() {
            Empresa empresa = buildEmpresaActiva("bio-tech", "BioTech Solutions", "ORO", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("bio-tech", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("bio-tech");

            assertThat(dto.getOgDescripcion())
                    .isEqualTo("Nivel ecológico: ORO. Consulta el desempeño ambiental verificado de BioTech Solutions.");
        }

        @Test
        @DisplayName("ogImagen usa logoUrl de la empresa cuando tiene logoUrl")
        void ogImagen_conLogoUrl() {
            Empresa empresa = buildEmpresaActiva("con-logo", "Empresa Logo", "PLATA", "https://cdn.example.com/mi-logo.png");
            when(empresaRepository.findBySlugAndEstado("con-logo", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("con-logo");

            assertThat(dto.getOgImagen()).isEqualTo("https://cdn.example.com/mi-logo.png");
        }

        @Test
        @DisplayName("ogImagen usa fallback cuando logoUrl es null")
        void ogImagen_sinLogoUrl_usaFallback() {
            Empresa empresa = buildEmpresaActiva("sin-logo", "Empresa Sin Logo", "BRONCE", null);
            when(empresaRepository.findBySlugAndEstado("sin-logo", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("sin-logo");

            assertThat(dto.getOgImagen()).isEqualTo(OG_IMAGEN_FALLBACK);
        }

        @Test
        @DisplayName("ogImagen usa fallback cuando logoUrl es cadena vacía")
        void ogImagen_logoUrlVacio_usaFallback() {
            Empresa empresa = buildEmpresaActiva("logo-vacio", "Empresa Logo Vacío", "ORO", "");
            when(empresaRepository.findBySlugAndEstado("logo-vacio", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("logo-vacio");

            assertThat(dto.getOgImagen()).isEqualTo(OG_IMAGEN_FALLBACK);
        }

        @Test
        @DisplayName("ogDescripcion muestra 'Sin nivel' cuando nivelEcologico es null")
        void ogDescripcion_nivelNull_muestraSinNivel() {
            Empresa empresa = buildEmpresaActiva("sin-nivel", "Empresa Sin Nivel", null, "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("sin-nivel", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("sin-nivel");

            assertThat(dto.getOgDescripcion())
                    .isEqualTo("Nivel ecológico: Sin nivel. Consulta el desempeño ambiental verificado de Empresa Sin Nivel.");
        }
    }

    // ========================================================================
    // Fragmento HTML incrustable
    // ========================================================================

    @Nested
    @DisplayName("Generación del fragmento HTML incrustable")
    class FragmentoHtml {

        @Test
        @DisplayName("codigoIncrustar contiene enlace <a> con href a URL canónica")
        void codigoIncrustar_contieneEnlaceConUrlCanonica() {
            Empresa empresa = buildEmpresaActiva("sello-test", "Sello Corp", "PLATA", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("sello-test", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("sello-test");

            assertThat(dto.getCodigoIncrustar())
                    .contains("href=\"https://carbonhub.app/empresa/sello-test/reputacion\"");
        }

        @Test
        @DisplayName("codigoIncrustar contiene nombre de empresa y mención CarbonHub")
        void codigoIncrustar_contieneNombreYMencion() {
            Empresa empresa = buildEmpresaActiva("nombre-test", "Mi Empresa Verde", "ORO", null);
            when(empresaRepository.findBySlugAndEstado("nombre-test", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("nombre-test");

            assertThat(dto.getCodigoIncrustar())
                    .contains("Mi Empresa Verde")
                    .contains("Perfil verificado en CarbonHub");
        }

        @Test
        @DisplayName("codigoIncrustar incluye estilos CSS inline")
        void codigoIncrustar_conEstilosInline() {
            Empresa empresa = buildEmpresaActiva("estilo-test", "Estilo Corp", "BRONCE", null);
            when(empresaRepository.findBySlugAndEstado("estilo-test", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("estilo-test");

            assertThat(dto.getCodigoIncrustar()).contains("style=");
        }

        @Test
        @DisplayName("codigoIncrustar NO contiene scripts ni dependencias externas")
        void codigoIncrustar_sinScriptsNiDependencias() {
            Empresa empresa = buildEmpresaActiva("seguro-test", "Seguro Corp", "PLATINO", null);
            when(empresaRepository.findBySlugAndEstado("seguro-test", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("seguro-test");

            assertThat(dto.getCodigoIncrustar())
                    .doesNotContain("<script")
                    .doesNotContain("document.cookie");
        }

        @Test
        @DisplayName("codigoIncrustar escapa caracteres HTML en nombreEmpresa para evitar inyección")
        void codigoIncrustar_escapaNombreConHtmlMalicioso() {
            String nombreMalicioso = "<img src=x onerror=alert(1)>";
            Empresa empresa = buildEmpresaActiva("xss-test", nombreMalicioso, "ORO", null);
            when(empresaRepository.findBySlugAndEstado("xss-test", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("xss-test");

            // Los < y > están escapados, así que el navegador renderiza texto, no un tag HTML
            assertThat(dto.getCodigoIncrustar())
                    .doesNotContain("<img")
                    .doesNotContain("<script")
                    .contains("&lt;img src=x onerror=alert(1)&gt;");
        }
    }

    // ========================================================================
    // QR Base64
    // ========================================================================

    @Nested
    @DisplayName("Generación QR Base64")
    class QrBase64 {

        @Test
        @DisplayName("qrBase64 vacío cuando QrGeneradorService falla (retorna cadena vacía)")
        void qrBase64_vacioSiQrFalla() {
            Empresa empresa = buildEmpresaActiva("qr-falla", "QR Corp", "ORO", "https://logo.png");
            when(empresaRepository.findBySlugAndEstado("qr-falla", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("qr-falla");

            assertThat(dto.getQrBase64()).isEmpty();
            // El resto de los campos deben estar completos
            assertThat(dto.getUrlCanonica()).isNotBlank();
            assertThat(dto.getOgTitulo()).isNotBlank();
            assertThat(dto.getOgDescripcion()).isNotBlank();
            assertThat(dto.getOgImagen()).isNotBlank();
            assertThat(dto.getCodigoIncrustar()).isNotBlank();
            assertThat(dto.getOgUrl()).isNotBlank();
        }

        @Test
        @DisplayName("qrBase64 contiene valor retornado por QrGeneradorService cuando es exitoso")
        void qrBase64_conValorSiQrExitoso() {
            Empresa empresa = buildEmpresaActiva("qr-ok", "QR Ok Corp", "PLATA", null);
            when(empresaRepository.findBySlugAndEstado("qr-ok", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64("https://carbonhub.app/empresa/qr-ok/reputacion"))
                    .thenReturn("data:image/png;base64,encodedQrData");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("qr-ok");

            assertThat(dto.getQrBase64()).isEqualTo("data:image/png;base64,encodedQrData");
        }
    }

    // ========================================================================
    // SlugCambiadoException (redirect 301)
    // ========================================================================

    @Nested
    @DisplayName("Lanzamiento de SlugCambiadoException")
    class SlugCambiado {

        @Test
        @DisplayName("Lanza SlugCambiadoException cuando slug existe en historial y empresa está activa")
        void slugEnHistorial_empresaActiva_lanzaExcepcion() {
            UUID empresaId = UUID.randomUUID();
            when(empresaRepository.findBySlugAndEstado("slug-antiguo", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.empty());

            SlugHistorico historico = SlugHistorico.builder()
                    .id(UUID.randomUUID())
                    .empresaId(empresaId)
                    .slugAnterior("slug-antiguo")
                    .fechaCambio(Instant.now())
                    .build();
            when(slugHistoricoRepository.findBySlugAnterior("slug-antiguo"))
                    .thenReturn(Optional.of(historico));

            Empresa empresaVigente = buildEmpresaActiva("slug-nuevo", "Empresa Renombrada", "ORO", null);
            empresaVigente.setId(empresaId);
            when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaVigente));

            assertThatThrownBy(() -> service.obtenerEnlacePerfil("slug-antiguo"))
                    .isInstanceOf(SlugCambiadoException.class)
                    .satisfies(ex -> assertThat(((SlugCambiadoException) ex).getSlugVigente())
                            .isEqualTo("slug-nuevo"));
        }

        @Test
        @DisplayName("No lanza SlugCambiadoException cuando empresa en historial está inactiva → lanza PerfilNoEncontradoException")
        void slugEnHistorial_empresaInactiva_lanzaPerfilNoEncontrado() {
            UUID empresaId = UUID.randomUUID();
            when(empresaRepository.findBySlugAndEstado("slug-viejo", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.empty());

            SlugHistorico historico = SlugHistorico.builder()
                    .id(UUID.randomUUID())
                    .empresaId(empresaId)
                    .slugAnterior("slug-viejo")
                    .fechaCambio(Instant.now())
                    .build();
            when(slugHistoricoRepository.findBySlugAnterior("slug-viejo"))
                    .thenReturn(Optional.of(historico));

            Empresa empresaInactiva = buildEmpresaActiva("slug-actual", "Empresa Inactiva", "PLATA", null);
            empresaInactiva.setId(empresaId);
            empresaInactiva.setEstado(EstadoEmpresa.INACTIVO);
            when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaInactiva));

            assertThatThrownBy(() -> service.obtenerEnlacePerfil("slug-viejo"))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");
        }
    }

    // ========================================================================
    // PerfilNoEncontradoException
    // ========================================================================

    @Nested
    @DisplayName("Lanzamiento de PerfilNoEncontradoException")
    class PerfilNoEncontrado {

        @Test
        @DisplayName("Lanza excepción cuando empresa no existe (slug no encontrado en activas ni en historial)")
        void empresaNoExiste_lanzaExcepcion() {
            when(empresaRepository.findBySlugAndEstado("inexistente", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.empty());
            when(slugHistoricoRepository.findBySlugAnterior("inexistente"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerEnlacePerfil("inexistente"))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");
        }

        @Test
        @DisplayName("Lanza excepción cuando empresa está inactiva (findBySlugAndEstado no la encuentra)")
        void empresaInactiva_lanzaExcepcion() {
            when(empresaRepository.findBySlugAndEstado("empresa-inactiva", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.empty());
            when(slugHistoricoRepository.findBySlugAnterior("empresa-inactiva"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerEnlacePerfil("empresa-inactiva"))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");
        }
    }

    // ========================================================================
    // Validación de slug (formato inválido → rechazo sin consulta a BD)
    // ========================================================================

    @Nested
    @DisplayName("Rechazo de slug con formato inválido sin consulta a BD")
    class SlugInvalido {

        @Test
        @DisplayName("Slug con mayúsculas se normaliza y funciona si empresa existe")
        void slugConMayusculas_seNormalizaYFunciona() {
            Empresa empresa = buildEmpresaActiva("eco-verde", "EcoVerde S.A.", "ORO", null);
            when(empresaRepository.findBySlugAndEstado("eco-verde", EstadoEmpresa.ACTIVO))
                    .thenReturn(Optional.of(empresa));
            when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,x");

            EnlacePerfilDTO dto = service.obtenerEnlacePerfil("ECO-VERDE");

            assertThat(dto.getUrlCanonica()).contains("eco-verde");
        }

        @Test
        @DisplayName("Slug con caracteres especiales → PerfilNoEncontradoException sin consulta a BD")
        void slugConCaracteresEspeciales_rechaza() {
            assertThatThrownBy(() -> service.obtenerEnlacePerfil("empresa@#$%"))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");

            verify(empresaRepository, never()).findBySlugAndEstado(any(), any());
            verify(slugHistoricoRepository, never()).findBySlugAnterior(any());
        }

        @Test
        @DisplayName("Slug con espacios → PerfilNoEncontradoException sin consulta a BD")
        void slugConEspacios_rechaza() {
            assertThatThrownBy(() -> service.obtenerEnlacePerfil("mi empresa"))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");

            verify(empresaRepository, never()).findBySlugAndEstado(any(), any());
        }

        @Test
        @DisplayName("Slug vacío → PerfilNoEncontradoException sin consulta a BD")
        void slugVacio_rechaza() {
            assertThatThrownBy(() -> service.obtenerEnlacePerfil(""))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");

            verify(empresaRepository, never()).findBySlugAndEstado(any(), any());
        }

        @Test
        @DisplayName("Slug mayor a 120 caracteres → PerfilNoEncontradoException sin consulta a BD")
        void slugMuyLargo_rechaza() {
            String slugLargo = "a".repeat(121);

            assertThatThrownBy(() -> service.obtenerEnlacePerfil(slugLargo))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");

            verify(empresaRepository, never()).findBySlugAndEstado(any(), any());
        }

        @Test
        @DisplayName("Slug null → PerfilNoEncontradoException sin consulta a BD")
        void slugNull_rechaza() {
            assertThatThrownBy(() -> service.obtenerEnlacePerfil(null))
                    .isInstanceOf(PerfilNoEncontradoException.class)
                    .hasMessage("El perfil que buscas no existe o ya no está disponible.");

            verify(empresaRepository, never()).findBySlugAndEstado(any(), any());
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private Empresa buildEmpresaActiva(String slug, String nombre, String nivelEcologico, String logoUrl) {
        return Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombre)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@" + slug + ".com")
                .logoUrl(logoUrl)
                .nivelEcologico(nivelEcologico)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
    }
}
