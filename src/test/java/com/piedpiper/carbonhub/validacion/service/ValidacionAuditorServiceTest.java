package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.mappers.ValidacionAuditorMapper;
import com.piedpiper.carbonhub.validacion.models.dtos.DecisionSolicitudRequestDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.RegistroAuditoriaInterna;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
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
        verify(envioCorreoValidacionService).enviar(eq("Ana"), eq("ana@correo.com"), eq(true), eq(null));
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
                eq("Ana"), eq("ana@correo.com"), eq(false), eq("La certificación adjunta está vencida."));
    }

    @Test
    void decisionInvalidaLanza422SinTocarNada() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, UUID.randomUUID(),
                new DecisionSolicitudRequestDTO("pendiente", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(solicitudValidacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazoConMotivoCortoLanza422() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, UUID.randomUUID(), rechazar("corto")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void rechazoSinMotivoLanza422() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, UUID.randomUUID(), rechazar(null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioSinRolAdministradorPlataformaLanza403() {
        Usuario otro = Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_EMPRESA).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, UUID.randomUUID(), aprobar()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void solicitudInexistenteLanza404() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, UUID.randomUUID(), aprobar()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void solicitudYaProcesadaLanza409SinSobrescribir() {
        SolicitudValidacion aprobada = solicitud(EstadoSolicitud.APROBADO);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(solicitudValidacionRepository.findById(aprobada.getId())).thenReturn(Optional.of(aprobada));

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, aprobada.getId(), rechazar("Motivo suficiente aqui.")))
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

        assertThatThrownBy(() -> service.resolver(ADMIN_ID, pendiente.getId(), aprobar()))
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
}
