package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.CompletarConfiguracionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditoria.service.ValidadorDocumentosPdf;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.DocumentoCredencialAuditorRepository;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfiguracionInicialAuditorServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private PerfilAuditorService perfilAuditorService;
    @Mock
    private SolicitudValidacionRepository solicitudValidacionRepository;
    @Mock
    private DocumentoCredencialAuditorRepository documentoCredencialAuditorRepository;

    private ConfiguracionInicialAuditorService service;

    @BeforeEach
    void configurar() {
        service = new ConfiguracionInicialAuditorService(
                usuarioRepository,
                perfilAuditorRepository,
                perfilAuditorService,
                solicitudValidacionRepository,
                documentoCredencialAuditorRepository,
                new ValidadorDocumentosPdf());

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(auditorPendiente()));
        when(perfilAuditorService.asegurarPerfil(any()))
                .thenAnswer(invocacion -> PerfilAuditor.builder().auditor(invocacion.getArgument(0)).build());
        when(solicitudValidacionRepository.save(any(SolicitudValidacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Usuario auditorPendiente() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .configuracionCompleta(false)
                .build();
    }

    private static CompletarConfiguracionAuditorRequestDTO datos(List<String> especialidades) {
        return new CompletarConfiguracionAuditorRequestDTO(5, especialidades, "Descripción profesional", null);
    }

    private static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("documentos", nombre, MediaType.APPLICATION_PDF_VALUE, contenidoPdf());
    }

    private static byte[] contenidoPdf() {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void completaLaConfiguracionYDejaLaSolicitudPendiente() {
        MensajeResponseDTO respuesta = service.completar(
                USUARIO_ID, datos(List.of("AGROINDUSTRIA", "MANUFACTURA")), List.of(pdf("cert.pdf")));

        assertThat(respuesta.getMensaje()).contains("en revisión");

        ArgumentCaptor<PerfilAuditor> perfilCaptor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(perfilCaptor.capture());
        PerfilAuditor perfil = perfilCaptor.getValue();
        assertThat(perfil.getAniosExperiencia()).isEqualTo(5);
        assertThat(perfil.getDescripcionProfesional()).isEqualTo("Descripción profesional");
        assertThat(perfil.getEspecialidades()).hasSize(2);
        assertThat(perfil.getActualizadoEn()).isNotNull();

        ArgumentCaptor<SolicitudValidacion> solicitudCaptor = ArgumentCaptor.forClass(SolicitudValidacion.class);
        verify(solicitudValidacionRepository).save(solicitudCaptor.capture());
        assertThat(solicitudCaptor.getValue().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        ArgumentCaptor<DocumentoCredencialAuditor> documentoCaptor =
                ArgumentCaptor.forClass(DocumentoCredencialAuditor.class);
        verify(documentoCredencialAuditorRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getNombreArchivo()).isEqualTo("cert.pdf");
        assertThat(documentoCaptor.getValue().getTipoContenido()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().isConfiguracionCompleta()).isTrue();
    }

    @Test
    void auditorQueNoEstaPendienteDeValidacionDevuelve409() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(
                Usuario.builder().id(USUARIO_ID).rol(Rol.AUDITOR_CERTIFICADO)
                        .estado(EstadoUsuario.ACTIVO).build()));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, datos(List.of("MANUFACTURA")), List.of(pdf("cert.pdf"))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(solicitudValidacionRepository, never()).save(any());
    }

    @Test
    void auditorConConfiguracionYaCompletaDevuelve409() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(
                Usuario.builder().id(USUARIO_ID).rol(Rol.AUDITOR_CERTIFICADO)
                        .estado(EstadoUsuario.PENDIENTE_VALIDACION).configuracionCompleta(true).build()));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, datos(List.of("MANUFACTURA")), List.of(pdf("cert.pdf"))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void especialidadesDuplicadasDevuelve400() {
        assertThatThrownBy(() -> service.completar(
                USUARIO_ID, datos(List.of("MANUFACTURA", "MANUFACTURA")), List.of(pdf("cert.pdf"))))
                .isInstanceOf(ApiException.class)
                .hasMessage("La lista de especialidades contiene duplicados.")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(solicitudValidacionRepository, never()).save(any());
    }

    @Test
    void especialidadesDuplicadasPorMayusculasYEspaciosDevuelve400() {
        assertThatThrownBy(() -> service.completar(
                USUARIO_ID, datos(List.of("manufactura", "MANUFACTURA ")), List.of(pdf("cert.pdf"))))
                .isInstanceOf(ApiException.class)
                .hasMessage("La lista de especialidades contiene duplicados.")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void especialidadFueraDelCatalogoDevuelve400() {
        assertThatThrownBy(() -> service.completar(
                USUARIO_ID, datos(List.of("BUCEO_RECREATIVO")), List.of(pdf("cert.pdf"))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(solicitudValidacionRepository, never()).save(any());
    }

    @Test
    void documentoSinNombreDeArchivoDevuelve400YNoPersisteNada() {
        MultipartFile sinNombre = new MockMultipartFile(
                "documentos", null, MediaType.APPLICATION_PDF_VALUE, contenidoPdf());

        assertThatThrownBy(() -> service.completar(USUARIO_ID, datos(List.of("MANUFACTURA")), List.of(sinNombre)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(solicitudValidacionRepository, never()).save(any());
        verify(documentoCredencialAuditorRepository, never()).save(any());
    }

    @Test
    void documentoSinTipoDeContenidoDevuelve400() {
        MultipartFile sinTipo = new MockMultipartFile("documentos", "cert.pdf", null, contenidoPdf());

        assertThatThrownBy(() -> service.completar(USUARIO_ID, datos(List.of("MANUFACTURA")), List.of(sinTipo)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void errorAlLeerElDocumentoDevuelve500() throws IOException {
        MultipartFile fallido = mock(MultipartFile.class);
        when(fallido.isEmpty()).thenReturn(false);
        when(fallido.getSize()).thenReturn((long) contenidoPdf().length);
        when(fallido.getOriginalFilename()).thenReturn("cert.pdf");
        when(fallido.getContentType()).thenReturn(MediaType.APPLICATION_PDF_VALUE);
        // Primera llamada: la validacion de estructura PDF lee el archivo con exito.
        // Segunda llamada: guardarDocumentos() vuelve a leerlo para persistirlo y esta vez falla.
        when(fallido.getBytes())
                .thenReturn(contenidoPdf())
                .thenThrow(new IOException("disco lleno"));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, datos(List.of("MANUFACTURA")), List.of(fallido)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
