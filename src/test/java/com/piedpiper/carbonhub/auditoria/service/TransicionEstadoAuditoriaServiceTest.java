package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransicionEstadoAuditoriaServiceTest {

    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");

    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private NotificacionTransicionRegistroService notificacionTransicionRegistroService;

    private TransicionEstadoAuditoriaService service;

    @BeforeEach
    void configurar() {
        service = new TransicionEstadoAuditoriaService(
                transicionEstadoAuditoriaRepository,
                new ValidadorTransicionAuditoria(),
                notificacionTransicionRegistroService);
    }

    @Test
    void aplicarCambiaElEstadoYDejaLaTransicionEnElHistorial() {
        SolicitudAuditoria solicitud = solicitud(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);

        service.aplicar(solicitud, EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.AUDITOR, auditor());

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO);

        TransicionEstadoAuditoria registrada = capturarRegistrada();
        assertThat(registrada.getEstadoAnterior()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(registrada.getEstadoNuevo()).isEqualTo(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO);
        assertThat(registrada.getEvento()).isEqualTo(EventoTransicionAuditoria.AUDITOR_ACEPTA);
        assertThat(registrada.getActor()).isEqualTo(ActorTransicionAuditoria.AUDITOR);
        assertThat(registrada.getResponsableId()).isEqualTo(AUDITOR_ID);
        assertThat(registrada.getResponsableNombre()).isEqualTo("Ana Auditora");
        assertThat(registrada.getFecha()).isNotNull();
    }

    @Test
    void unaTransicionInvalidaNoTocaElEstadoNiRegistraNada() {
        SolicitudAuditoria solicitud = solicitud(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);

        assertThatThrownBy(() -> service.aplicar(solicitud, EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.AUDITOR, auditor()))
                .isInstanceOf(ApiException.class);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
        verify(transicionEstadoAuditoriaRepository, never()).save(any());
        verifyNoInteractions(notificacionTransicionRegistroService);
    }

    @Test
    void unaTransicionDelSistemaQuedaSinResponsableIdPeroConActorSistema() {
        SolicitudAuditoria solicitud = solicitud(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);

        service.aplicar(solicitud, EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA,
                ActorTransicionAuditoria.SISTEMA, null);

        TransicionEstadoAuditoria registrada = capturarRegistrada();
        assertThat(registrada.getResponsableId()).isNull();
        assertThat(registrada.getResponsableNombre()).isNull();
        assertThat(registrada.getActor()).isEqualTo(ActorTransicionAuditoria.SISTEMA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    @Test
    void cadaTransicionEncolaLaNotificacionCorrespondiente() {
        SolicitudAuditoria solicitud = solicitud(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);

        service.aplicar(solicitud, EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.AUDITOR, auditor());

        verify(notificacionTransicionRegistroService).encolar(solicitud,
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.AUDITOR_ACEPTA);
    }

    @Test
    void registrarCreacionDejaLaPrimeraEntradaDeLaLineaDeTiempo() {
        SolicitudAuditoria solicitud = solicitud(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);

        service.registrarCreacion(solicitud, auditor());

        TransicionEstadoAuditoria registrada = capturarRegistrada();
        assertThat(registrada.getEvento()).isEqualTo(EventoTransicionAuditoria.SOLICITUD_CREADA);
        assertThat(registrada.getActor()).isEqualTo(ActorTransicionAuditoria.EMPRESA);
        assertThat(registrada.getEstadoAnterior()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(registrada.getEstadoNuevo()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    private TransicionEstadoAuditoria capturarRegistrada() {
        ArgumentCaptor<TransicionEstadoAuditoria> captor =
                ArgumentCaptor.forClass(TransicionEstadoAuditoria.class);
        verify(transicionEstadoAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static SolicitudAuditoria solicitud(EstadoSolicitudAuditoria estado) {
        return SolicitudAuditoria.builder()
                .id(UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f"))
                .estado(estado)
                .build();
    }

    private static Usuario auditor() {
        return Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build();
    }
}
