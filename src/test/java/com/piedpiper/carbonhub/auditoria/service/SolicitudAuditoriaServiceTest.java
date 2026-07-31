package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.CrearSolicitudAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudAuditoriaServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CertificacionActivaConsulta certificacionActivaConsulta;

    private SolicitudAuditoriaService service;

    @BeforeEach
    void configurar() {
        service = new SolicitudAuditoriaService(
                solicitudAuditoriaRepository,
                usuarioRepository,
                empresaRepository,
                certificacionActivaConsulta,
                new ValidadorDocumentosPdf(),
                new SolicitudAuditoriaMapperImpl());

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConEmpresa()));
        when(empresaRepository.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa()));
        when(certificacionActivaConsulta.fechaVencimientoCertificacionActiva(EMPRESA_ID))
                .thenReturn(Optional.empty());
        when(solicitudAuditoriaRepository.existeSolicitudEnCursoTraslapada(any(), anyCollection(), any(), any()))
                .thenReturn(false);
        when(solicitudAuditoriaRepository.save(any(SolicitudAuditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaSolicitudInicialEnviadaConSusDocumentosEnLaMismaTransaccion() {
        CrearSolicitudAuditoriaRequestDTO datos = datos(
                LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(1), "Auditoría anual");

        SolicitudAuditoriaResponseDTO respuesta =
                service.crear(datos, List.of(pdf("uno.pdf"), pdf("dos.pdf")), USUARIO_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(guardada.getTipoCertificacion()).isEqualTo(TipoCertificacionSolicitud.INICIAL);
        assertThat(guardada.getPeriodoInicio()).isEqualTo(datos.getPeriodoInicio());
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        assertThat(guardada.getDocumentos())
                .hasSize(2)
                .allSatisfy(documento -> {
                    assertThat(documento.getSolicitud()).isSameAs(guardada);
                    assertThat(documento.getTipoContenido()).isEqualTo("application/pdf");
                    assertThat(documento.getContenido()).isNotEmpty();
                    assertThat(documento.getFechaCarga()).isNotNull();
                })
                .extracting(DocumentoRespaldo::getNombreArchivo)
                .containsExactly("uno.pdf", "dos.pdf");

        assertThat(respuesta.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(respuesta.getTipoCertificacion()).isEqualTo(TipoCertificacionSolicitud.INICIAL);
        assertThat(respuesta.getDocumentos()).hasSize(2);
    }

    @Test
    void laRespuestaNuncaIncluyeElContenidoBinarioDeLosDocumentos() {
        SolicitudAuditoriaResponseDTO respuesta = service.crear(
                datos(LocalDate.now().minusMonths(3), LocalDate.now().minusDays(1), null),
                List.of(pdf("uno.pdf")),
                USUARIO_ID);

        assertThat(respuesta.getDocumentos()).singleElement().satisfies(documento -> {
            assertThat(documento.getNombreArchivo()).isEqualTo("uno.pdf");
            assertThat(documento.getTamanioBytes()).isPositive();
        });
        assertThat(DocumentoRespaldoResponseDTO.class.getDeclaredFields())
                .noneMatch(campo -> campo.getType() == byte[].class);
    }

    @Test
    void certificacionActivaGeneraRenovacionConPeriodoInicioCalculadoPorElServidor() {
        LocalDate vencimiento = LocalDate.now().minusMonths(2);
        when(certificacionActivaConsulta.fechaVencimientoCertificacionActiva(EMPRESA_ID))
                .thenReturn(Optional.of(vencimiento));

        service.crear(
                datos(LocalDate.now().minusYears(3), LocalDate.now().plusMonths(1), null),
                List.of(pdf("uno.pdf")),
                USUARIO_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getTipoCertificacion()).isEqualTo(TipoCertificacionSolicitud.RENOVACION);
        assertThat(guardada.getPeriodoInicio()).isEqualTo(vencimiento.plusDays(1));
    }

    @Test
    void periodoQueIniciaEnFechaFuturaDevuelve422() {
        assertThatThrownBy(() -> service.crear(
                datos(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2), null),
                List.of(pdf("uno.pdf")),
                USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("El período a auditar no puede iniciar en una fecha futura.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void periodoMayorADoceMesesDevuelve422() {
        LocalDate inicio = LocalDate.now().minusYears(2);

        assertThatThrownBy(() -> service.crear(
                datos(inicio, inicio.plusMonths(12).plusDays(1), null),
                List.of(pdf("uno.pdf")),
                USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("El período a auditar no puede exceder 12 meses.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void bloqueaLaEmpresaAntesDeValidarElTraslape() {
        service.crear(datosValidos(), List.of(pdf("uno.pdf")), USUARIO_ID);

        InOrder orden = inOrder(empresaRepository, solicitudAuditoriaRepository);
        orden.verify(empresaRepository).bloquearPorId(EMPRESA_ID);
        orden.verify(solicitudAuditoriaRepository)
                .existeSolicitudEnCursoTraslapada(any(), anyCollection(), any(), any());
        orden.verify(solicitudAuditoriaRepository).save(any());
    }

    @Test
    void noTomaElLockSiElPeriodoYaEsInvalido() {
        assertThatThrownBy(() -> service.crear(
                datos(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2), null),
                List.of(pdf("uno.pdf")),
                USUARIO_ID))
                .isInstanceOf(ApiException.class);

        verify(empresaRepository, never()).bloquearPorId(any());
    }

    @Test
    void periodoDeExactamenteDoceMesesSeAcepta() {
        LocalDate inicio = LocalDate.now().minusYears(2);
        when(solicitudAuditoriaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.crear(datos(inicio, inicio.plusMonths(12), null), List.of(pdf("uno.pdf")), USUARIO_ID);

        verify(solicitudAuditoriaRepository).save(any());
    }

    @Test
    void periodoFinAnteriorOIgualAlInicioDevuelve422() {
        LocalDate inicio = LocalDate.now().minusMonths(3);

        assertThatThrownBy(() -> service.crear(
                datos(inicio, inicio, null), List.of(pdf("uno.pdf")), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("La fecha de fin del período debe ser posterior a la fecha de inicio.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void solicitudSinDocumentosDevuelve400() {
        assertThatThrownBy(() -> service.crear(datosValidos(), null, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Debes adjuntar al menos un documento de respaldo.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void masDeDiezDocumentosDevuelve400() {
        List<MultipartFile> once = IntStream.rangeClosed(1, 11)
                .mapToObj(indice -> pdf("respaldo-" + indice + ".pdf"))
                .map(MultipartFile.class::cast)
                .toList();

        assertThatThrownBy(() -> service.crear(datosValidos(), once, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Puedes adjuntar un máximo de 10 documentos.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void archivoQueSuperaQuinceMegabytesDevuelve400() {
        MultipartFile pesado = mock(MultipartFile.class);
        when(pesado.isEmpty()).thenReturn(false);
        when(pesado.getSize()).thenReturn(ValidadorDocumentosPdf.TAMANIO_MAXIMO_BYTES + 1);

        assertThatThrownBy(() -> service.crear(datosValidos(), List.of(pesado), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("El archivo no puede superar 15 MB.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void archivoQueNoEsPdfSegunSusMagicBytesDevuelve400() {
        MultipartFile disfrazado = new MockMultipartFile("documentos", "respaldo.pdf",
                "application/pdf", "PK en realidad es un zip".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.crear(datosValidos(), List.of(disfrazado), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void periodoTraslapadoConOtraSolicitudEnCursoDevuelve409() {
        when(solicitudAuditoriaRepository.existeSolicitudEnCursoTraslapada(
                eq(EMPRESA_ID), anyCollection(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.crear(datosValidos(), List.of(pdf("uno.pdf")), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe una solicitud de auditoría en curso para un período que se "
                        + "traslapa con el seleccionado.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void usuarioSinEmpresaConfiguradaDevuelve422() {
        when(usuarioRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(Usuario.builder().id(USUARIO_ID).build()));

        assertThatThrownBy(() -> service.crear(datosValidos(), List.of(pdf("uno.pdf")), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Debes completar la configuración de tu empresa antes de realizar esta acción.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private SolicitudAuditoria capturarGuardada() {
        ArgumentCaptor<SolicitudAuditoria> captor = ArgumentCaptor.forClass(SolicitudAuditoria.class);
        verify(solicitudAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Usuario usuarioConEmpresa() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(empresa())
                .build();
    }

    private static Empresa empresa() {
        return Empresa.builder().id(EMPRESA_ID).build();
    }

    private static CrearSolicitudAuditoriaRequestDTO datosValidos() {
        return datos(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(1), null);
    }

    private static CrearSolicitudAuditoriaRequestDTO datos(LocalDate inicio, LocalDate fin, String descripcion) {
        return new CrearSolicitudAuditoriaRequestDTO(inicio, fin, descripcion);
    }

    private static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("documentos", nombre, "application/pdf",
                "%PDF-1.7 contenido de prueba".getBytes(StandardCharsets.US_ASCII));
    }
}
