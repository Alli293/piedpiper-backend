package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.dtos.DecisionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DecisionAuditorServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID OTRO_AUDITOR_ID = UUID.fromString("deadbeef-1111-2222-3333-444455556666");
    private static final long HORAS_PLAZO = 120;

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private NotificacionTransicionRegistroService notificacionTransicionRegistroService;

    private DecisionAuditorService service;

    @BeforeEach
    void configurar() {
        TransicionEstadoAuditoriaService transiciones = new TransicionEstadoAuditoriaService(
                transicionEstadoAuditoriaRepository,
                new ValidadorTransicionAuditoria(),
                notificacionTransicionRegistroService);

        service = new DecisionAuditorService(
                solicitudAuditoriaRepository, usuarioRepository, transiciones, HORAS_PLAZO);
        ReflectionTestUtils.setField(service, "horasParaResponder", HORAS_PLAZO);

        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(auditor()));
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitudAsignada(Duration.ofHours(2))));
        when(solicitudAuditoriaRepository.saveAndFlush(any(SolicitudAuditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void aceptarDejaLaSolicitudEnRevisionRegistrandoLasDosTransiciones() {
        service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.EN_REVISION);
        assertThat(guardada.getFechaAceptacion()).isNotNull();
        assertThat(guardada.getAuditor()).isNotNull();

        List<TransicionEstadoAuditoria> registradas = capturarTransiciones();
        assertThat(registradas).hasSize(2);
        assertThat(registradas.get(0).getEvento()).isEqualTo(EventoTransicionAuditoria.AUDITOR_ACEPTA);
        assertThat(registradas.get(0).getEstadoNuevo())
                .isEqualTo(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO);
        assertThat(registradas.get(1).getEvento()).isEqualTo(EventoTransicionAuditoria.INICIO_REVISION);
        assertThat(registradas.get(1).getEstadoNuevo()).isEqualTo(EstadoSolicitudAuditoria.EN_REVISION);
        assertThat(registradas).allSatisfy(transicion ->
                assertThat(transicion.getActor()).isEqualTo(ActorTransicionAuditoria.AUDITOR));
    }

    @Test
    void rechazarLiberaLaAsignacionSinTocarElEstadoYGuardaElMotivo() {
        service.responder(SOLICITUD_ID, datos("rechazada", "No tengo disponibilidad este trimestre"),
                AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(guardada.getAuditor()).isNull();
        assertThat(guardada.getOrigenAsignacion()).isNull();
        assertThat(guardada.getFechaAsignacion()).isNull();
        assertThat(guardada.getMotivoRechazo()).isEqualTo("No tengo disponibilidad este trimestre");
        assertThat(guardada.getFechaRechazo()).isNotNull();

        assertThat(capturarTransiciones()).singleElement().satisfies(transicion ->
                assertThat(transicion.getEvento()).isEqualTo(EventoTransicionAuditoria.AUDITOR_RECHAZA));
    }

    @Test
    void rechazarSinMotivoDevuelve422YNoTocaLaSolicitud() {
        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("rechazada", "  "), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaDecisionQueNoEsAceptadaNiRechazadaDevuelve422() {
        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("quizas", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void otroAuditorIntentandoResponderDevuelve403() {
        when(usuarioRepository.findById(OTRO_AUDITOR_ID)).thenReturn(Optional.of(auditor()));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), OTRO_AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unaSolicitudSinAsignacionVigenteDevuelve409() {
        SolicitudAuditoria liberada = solicitudAsignada(Duration.ofHours(2));
        liberada.setAuditor(null);
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(liberada));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Esta solicitud ya no está disponible para tu respuesta.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void responderDosVecesDevuelve409LaSegunda() {
        SolicitudAuditoria yaAceptada = solicitudAsignada(Duration.ofHours(2));
        yaAceptada.setFechaAceptacion(Instant.now());
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(yaAceptada));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void aceptarPasadas120HorasDevuelve409() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitudAsignada(Duration.ofHours(HORAS_PLAZO + 1))));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void aceptarJustoEnElLimiteDe120HorasDevuelve409() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitudAsignada(Duration.ofHours(HORAS_PLAZO))));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * El rechazo no mira el plazo: la historia solo se lo exige a la aceptacion, y si el plazo
     * vencio de verdad el barrido ya habria liberado la asignacion.
     */
    @Test
    void rechazarPasadoElPlazoSigueSiendoValidoMientrasLaAsignacionSigaVigente() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitudAsignada(Duration.ofHours(HORAS_PLAZO + 5))));

        service.responder(SOLICITUD_ID, datos("rechazada", "Conflicto de intereses con la empresa"),
                AUDITOR_ID);

        assertThat(capturarGuardada().getAuditor()).isNull();
    }

    @Test
    void unaSolicitudInexistenteDevuelve404() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unaCarreraAlGuardarSeTraduceAlMismo409QueLaValidacion() {
        when(solicitudAuditoriaRepository.saveAndFlush(any(SolicitudAuditoria.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(SolicitudAuditoria.class, SOLICITUD_ID));

        assertThatThrownBy(() -> service.responder(SOLICITUD_ID, datos("aceptada", null), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void laDecisionAceptaMayusculasYEspacios() {
        service.responder(SOLICITUD_ID, datos("  ACEPTADA  ", null), AUDITOR_ID);

        assertThat(capturarGuardada().getEstado()).isEqualTo(EstadoSolicitudAuditoria.EN_REVISION);
    }

    private SolicitudAuditoria capturarGuardada() {
        ArgumentCaptor<SolicitudAuditoria> captor = ArgumentCaptor.forClass(SolicitudAuditoria.class);
        verify(solicitudAuditoriaRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private List<TransicionEstadoAuditoria> capturarTransiciones() {
        ArgumentCaptor<TransicionEstadoAuditoria> captor =
                ArgumentCaptor.forClass(TransicionEstadoAuditoria.class);
        verify(transicionEstadoAuditoriaRepository, org.mockito.Mockito.atLeastOnce())
                .save(captor.capture());
        return captor.getAllValues();
    }

    private static DecisionAuditorRequestDTO datos(String decision, String motivo) {
        return new DecisionAuditorRequestDTO(decision, motivo);
    }

    private static SolicitudAuditoria solicitudAsignada(Duration desdeLaAsignacion) {
        return SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .id(UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01"))
                        .nombreEmpresa("Acme S.A.")
                        .correoCorporativo("contacto@acme.cr")
                        .build())
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .auditor(auditor())
                .origenAsignacion(OrigenAsignacion.MANUAL)
                .fechaAsignacion(Instant.now().minus(desdeLaAsignacion))
                .build();
    }

    private static Usuario auditor() {
        return Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build();
    }
}
