package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditor.service.PerfilAuditorService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.mappers.ValidacionAuditorMapper;
import com.piedpiper.carbonhub.validacion.models.dtos.DecisionSolicitudRequestDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.DocumentoCredencialResumenResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudDetalleResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;
import com.piedpiper.carbonhub.validacion.models.entities.RegistroAuditoriaInterna;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.DocumentoCredencialAuditorRepository;
import com.piedpiper.carbonhub.validacion.repository.RegistroAuditoriaInternaRepository;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidacionAuditorServiceTest {

    @Mock
    private SolicitudValidacionRepository solicitudValidacionRepository;
    @Mock
    private RegistroAuditoriaInternaRepository registroAuditoriaInternaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EnvioCorreoValidacionService envioCorreoValidacionService;
    @Mock
    private PerfilAuditorService perfilAuditorService;
    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private DocumentoCredencialAuditorRepository documentoCredencialAuditorRepository;
    @Spy
    private ValidacionAuditorMapper validacionAuditorMapper =
            Mappers.getMapper(ValidacionAuditorMapper.class);

    @InjectMocks
    private ValidacionAuditorService service;

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private Usuario administrador() {
        return Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_PLATAFORMA).build();
    }

    private Usuario auditor() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Ana")
                .apellidos("Mora")
                .email("ana@correo.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .build();
    }

    private SolicitudValidacion solicitud(EstadoSolicitud estado) {
        return SolicitudValidacion.builder()
                .id(UUID.randomUUID())
                .auditor(auditor())
                .estado(estado)
                .fechaSolicitud(Instant.now())
                .build();
    }

    private DecisionSolicitudRequestDTO aprobar() {
        return new DecisionSolicitudRequestDTO("aprobado", null);
    }

    private DecisionSolicitudRequestDTO rechazar(String motivo) {
        return new DecisionSolicitudRequestDTO("rechazado", motivo);
    }

    @Test
    void aprobarActivaAlAuditorRegistraAuditoriaYEnviaCorreo() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
        when(solicitudValidacionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResueltaResponseDTO response = service.resolver(ADMIN_ID, pendiente.getId(), aprobar());

        assertThat(response.getEstado()).isEqualTo("APROBADO");
        assertThat(response.getEstadoAuditor()).isEqualTo("ACTIVO");
        assertThat(pendiente.getResueltaPor().getId()).isEqualTo(ADMIN_ID);
        assertThat(pendiente.getFechaResolucion()).isNotNull();

        ArgumentCaptor<RegistroAuditoriaInterna> captor =
                ArgumentCaptor.forClass(RegistroAuditoriaInterna.class);
        verify(registroAuditoriaInternaRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("validacion_auditor");
        assertThat(captor.getValue().getDecision()).isEqualTo("aprobado");
        assertThat(captor.getValue().getAdministradorId()).isEqualTo(ADMIN_ID);
        verify(envioCorreoValidacionService).enviar("Ana", "ana@correo.com", true, null);
        verify(perfilAuditorService).asegurarPerfil(pendiente.getAuditor());
    }

    @Test
    void rechazarConMotivoValidoMarcaRechazadoYGuardaElMotivo() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
        when(solicitudValidacionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResueltaResponseDTO response = service.resolver(
                ADMIN_ID, pendiente.getId(), rechazar("La certificación adjunta está vencida."));

        assertThat(response.getEstado()).isEqualTo("RECHAZADO");
        assertThat(response.getEstadoAuditor()).isEqualTo("RECHAZADO");
        assertThat(response.getMotivoRechazo()).isEqualTo("La certificación adjunta está vencida.");
        verify(envioCorreoValidacionService).enviar(
                "Ana", "ana@correo.com", false, "La certificación adjunta está vencida.");
        verify(perfilAuditorService, never()).asegurarPerfil(any());
    }

    @Test
    void decisionInvalidaLanza422SinTocarNada() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        UUID solicitudId = UUID.randomUUID();
        DecisionSolicitudRequestDTO decisionInvalida = new DecisionSolicitudRequestDTO("pendiente", null);

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, decisionInvalida))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(solicitudValidacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazoConMotivoCortoLanza422() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        UUID solicitudId = UUID.randomUUID();
        DecisionSolicitudRequestDTO motivoCorto = rechazar("corto");

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, motivoCorto))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void rechazoSinMotivoLanza422() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        UUID solicitudId = UUID.randomUUID();
        DecisionSolicitudRequestDTO sinMotivo = rechazar(null);

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, sinMotivo))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioSinRolAdministradorPlataformaLanza403() {
        Usuario otro = Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_EMPRESA).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(otro));
        UUID solicitudId = UUID.randomUUID();
        DecisionSolicitudRequestDTO decisionAprobar = aprobar();

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, decisionAprobar))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void solicitudInexistenteLanza404() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(any())).thenReturn(Optional.empty());
        UUID solicitudId = UUID.randomUUID();
        DecisionSolicitudRequestDTO decisionAprobar = aprobar();

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, decisionAprobar))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void solicitudYaProcesadaLanza409SinSobrescribir() {
        SolicitudValidacion aprobada = solicitud(EstadoSolicitud.APROBADO);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(aprobada.getId())).thenReturn(Optional.of(aprobada));
        UUID solicitudId = aprobada.getId();
        DecisionSolicitudRequestDTO decisionRechazar = rechazar("Motivo suficiente aqui.");

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, decisionRechazar))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(solicitudValidacionRepository, never()).saveAndFlush(any());
        verify(registroAuditoriaInternaRepository, never()).save(any());
        verify(envioCorreoValidacionService, never()).enviar(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    }

    @Test
    void conflictoDeBloqueoOptimistaLanza409() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
        when(solicitudValidacionRepository.saveAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(SolicitudValidacion.class, pendiente.getId()));
        UUID solicitudId = pendiente.getId();
        DecisionSolicitudRequestDTO decisionAprobar = aprobar();

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, solicitudId, decisionAprobar))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(registroAuditoriaInternaRepository, never()).save(any());
    }

    @Test
    void listarPendientesDevuelveLaPaginaOrdenadaConNombreCompleto() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findAllByEstadoOrderByFechaSolicitudAsc(
                eq(EstadoSolicitud.PENDIENTE), any()))
                .thenReturn(new PageImpl<>(List.of(pendiente)));

        PaginaSolicitudesResponseDTO respuesta = service.listarPendientes(ADMIN_ID, 0);

        assertThat(respuesta.getContenido()).hasSize(1);
        assertThat(respuesta.getContenido().getFirst().getNombreAuditor()).isEqualTo("Ana Mora");
        assertThat(respuesta.getContenido().getFirst().getEmail()).isEqualTo("ana@correo.com");
        assertThat(respuesta.getTotalElementos()).isEqualTo(1);
    }

    @Test
    void listarSinRolAdministradorPlataformaLanza403() {
        Usuario otro = Usuario.builder().id(ADMIN_ID).rol(Rol.USUARIO_GENERAL).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.listarPendientes(ADMIN_ID, 0))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtenerDetalleDevuelveElPerfilYElResumenDeDocumentos() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        PerfilAuditor perfil = PerfilAuditor.builder()
                .auditor(pendiente.getAuditor())
                .aniosExperiencia(8)
                .descripcionProfesional("Especialista en manufactura sostenible.")
                .sitioWeb("https://ana-mora.example.com")
                .especialidades(Set.of(EspecialidadAuditor.MANUFACTURA, EspecialidadAuditor.AGROINDUSTRIA))
                .build();
        List<DocumentoCredencialResumenResponseDTO> documentos = List.of(
                new DocumentoCredencialResumenResponseDTO(UUID.randomUUID(), "cert.pdf", 1024L));

        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
        when(perfilAuditorRepository.findByAuditorId(pendiente.getAuditor().getId()))
                .thenReturn(Optional.of(perfil));
        when(documentoCredencialAuditorRepository.resumenPorSolicitudId(pendiente.getId()))
                .thenReturn(documentos);

        SolicitudDetalleResponseDTO detalle = service.obtenerDetalle(ADMIN_ID, pendiente.getId());

        assertThat(detalle.getId()).isEqualTo(pendiente.getId());
        assertThat(detalle.getNombreAuditor()).isEqualTo("Ana Mora");
        assertThat(detalle.getEmail()).isEqualTo("ana@correo.com");
        assertThat(detalle.getEstado()).isEqualTo("PENDIENTE");
        assertThat(detalle.getAniosExperiencia()).isEqualTo(8);
        assertThat(detalle.getEspecialidades()).containsExactly("AGROINDUSTRIA", "MANUFACTURA");
        assertThat(detalle.getDescripcionProfesional()).isEqualTo("Especialista en manufactura sostenible.");
        assertThat(detalle.getSitioWeb()).isEqualTo("https://ana-mora.example.com");
        assertThat(detalle.getDocumentos()).hasSize(1);
        assertThat(detalle.getDocumentos().getFirst().getNombreArchivo()).isEqualTo("cert.pdf");
    }

    @Test
    void obtenerDetalleSinPerfilAunNoConfiguradoDevuelveCamposNulos() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(pendiente.getId())).thenReturn(Optional.of(pendiente));
        when(perfilAuditorRepository.findByAuditorId(pendiente.getAuditor().getId())).thenReturn(Optional.empty());
        when(documentoCredencialAuditorRepository.resumenPorSolicitudId(pendiente.getId())).thenReturn(List.of());

        SolicitudDetalleResponseDTO detalle = service.obtenerDetalle(ADMIN_ID, pendiente.getId());

        assertThat(detalle.getAniosExperiencia()).isNull();
        assertThat(detalle.getDescripcionProfesional()).isNull();
        assertThat(detalle.getSitioWeb()).isNull();
        assertThat(detalle.getEspecialidades()).isEmpty();
        assertThat(detalle.getDocumentos()).isEmpty();
    }

    @Test
    void obtenerDetalleDeSolicitudInexistenteLanza404() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        UUID solicitudId = UUID.randomUUID();
        when(solicitudValidacionRepository.findById(solicitudId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetalle(ADMIN_ID, solicitudId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerDetalleSinRolAdministradorPlataformaLanza403() {
        Usuario otro = Usuario.builder().id(ADMIN_ID).rol(Rol.AUDITOR_CERTIFICADO).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.obtenerDetalle(ADMIN_ID, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtenerDocumentoDevuelveElDocumentoCuandoPerteneceALaSolicitud() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        DocumentoCredencialAuditor documento = DocumentoCredencialAuditor.builder()
                .id(UUID.randomUUID())
                .solicitud(pendiente)
                .nombreArchivo("cert.pdf")
                .tipoContenido("application/pdf")
                .contenido(new byte[] {1, 2, 3})
                .build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.existsById(pendiente.getId())).thenReturn(true);
        when(documentoCredencialAuditorRepository.findById(documento.getId())).thenReturn(Optional.of(documento));

        DocumentoCredencialAuditor resultado =
                service.obtenerDocumento(ADMIN_ID, pendiente.getId(), documento.getId());

        assertThat(resultado).isSameAs(documento);
    }

    @Test
    void obtenerDocumentoDeSolicitudInexistenteLanza404() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        UUID solicitudId = UUID.randomUUID();
        when(solicitudValidacionRepository.existsById(solicitudId)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerDocumento(ADMIN_ID, solicitudId, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(documentoCredencialAuditorRepository, never()).findById(any());
    }

    @Test
    void obtenerDocumentoInexistenteLanza404() {
        SolicitudValidacion pendiente = solicitud(EstadoSolicitud.PENDIENTE);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.existsById(pendiente.getId())).thenReturn(true);
        UUID documentoId = UUID.randomUUID();
        when(documentoCredencialAuditorRepository.findById(documentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDocumento(ADMIN_ID, pendiente.getId(), documentoId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerDocumentoDeOtraSolicitudLanza404SinFiltrarPorSoloExistir() {
        SolicitudValidacion propia = solicitud(EstadoSolicitud.PENDIENTE);
        SolicitudValidacion ajena = solicitud(EstadoSolicitud.PENDIENTE);
        DocumentoCredencialAuditor documentoDeOtraSolicitud = DocumentoCredencialAuditor.builder()
                .id(UUID.randomUUID())
                .solicitud(ajena)
                .nombreArchivo("cert.pdf")
                .tipoContenido("application/pdf")
                .contenido(new byte[] {1})
                .build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.existsById(propia.getId())).thenReturn(true);
        when(documentoCredencialAuditorRepository.findById(documentoDeOtraSolicitud.getId()))
                .thenReturn(Optional.of(documentoDeOtraSolicitud));

        assertThatThrownBy(() -> service.obtenerDocumento(ADMIN_ID, propia.getId(), documentoDeOtraSolicitud.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerDocumentoSinRolAdministradorPlataformaLanza403() {
        Usuario otro = Usuario.builder().id(ADMIN_ID).rol(Rol.AUDITOR_CERTIFICADO).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.obtenerDocumento(ADMIN_ID, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
