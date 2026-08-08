package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.NotificacionTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.NotificacionTransicionAuditoriaRepository;
import com.piedpiper.carbonhub.notification.service.EmailTransicionAuditoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacionTransicionEnvioServiceTest {

    private static final UUID NOTIFICACION_ID = UUID.fromString("0b1c2d3e-4f50-4162-8374-859607182930");
    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");

    @Mock
    private NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository;
    @Mock
    private NotificacionTransicionEstadoEnvioService notificacionTransicionEstadoEnvioService;
    @Mock
    private EmailTransicionAuditoriaService emailTransicionAuditoriaService;

    private NotificacionTransicionEnvioService service;

    @BeforeEach
    void configurar() {
        service = new NotificacionTransicionEnvioService(
                notificacionTransicionAuditoriaRepository,
                notificacionTransicionEstadoEnvioService,
                emailTransicionAuditoriaService,
                "https://carbonhub.cr");

        when(notificacionTransicionAuditoriaRepository
                .findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaCreacionAsc(any(), anyInt()))
                .thenReturn(List.of(pendiente()));
        when(notificacionTransicionEstadoEnvioService.reclamar(NOTIFICACION_ID)).thenReturn(true);
    }

    @Test
    void unaNotificacionPendienteSeEnviaYQuedaMarcadaComoEnviada() {
        service.enviarPendientes();

        verify(emailTransicionAuditoriaService).enviarCambioEstado(
                eq("contacto@acme.cr"), eq("Acme S.A."), eq("Acme S.A."),
                eq("Auditor asignado"), anyString());
        verify(notificacionTransicionEstadoEnvioService).marcarEnviada(NOTIFICACION_ID);
    }

    @Test
    void elEnlaceApuntaAlDetalleDeLaSolicitud() {
        service.enviarPendientes();

        verify(emailTransicionAuditoriaService).enviarCambioEstado(
                anyString(), anyString(), anyString(), anyString(),
                eq("https://carbonhub.cr/empresa/auditorias/" + SOLICITUD_ID));
    }

    /**
     * El orden es la garantia de que un corte entre el envio y el conteo no reenvie el correo: si se
     * enviara antes de reclamar, el siguiente barrido encontraria la notificacion todavia pendiente
     * y la mandaria de nuevo.
     */
    @Test
    void seReclamaAntesDeEnviarParaQueUnCorteNoDupliqueElCorreo() {
        service.enviarPendientes();

        InOrder orden = inOrder(notificacionTransicionEstadoEnvioService, emailTransicionAuditoriaService);
        orden.verify(notificacionTransicionEstadoEnvioService).reclamar(NOTIFICACION_ID);
        orden.verify(emailTransicionAuditoriaService).enviarCambioEstado(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void siOtroBarridoSeLaLlevoPrimeroNoSeEnviaNada() {
        when(notificacionTransicionEstadoEnvioService.reclamar(NOTIFICACION_ID)).thenReturn(false);

        service.enviarPendientes();

        verifyNoInteractions(emailTransicionAuditoriaService);
        verify(notificacionTransicionEstadoEnvioService, never()).marcarEnviada(any());
    }

    @Test
    void siElEnvioFallaNoSeMarcaEnviadaYSeEvaluaSiAgotoLosIntentos() {
        doThrow(new IllegalStateException("smtp caido"))
                .when(emailTransicionAuditoriaService)
                .enviarCambioEstado(anyString(), anyString(), anyString(), anyString(), anyString());

        service.enviarPendientes();

        verify(notificacionTransicionEstadoEnvioService, never()).marcarEnviada(any());
        verify(notificacionTransicionEstadoEnvioService).marcarFallidaSiAgotoIntentos(NOTIFICACION_ID);
    }

    @Test
    void unaNotificacionQueFallaNoCortaElRestoDelLote() {
        NotificacionTransicionAuditoria segunda = pendiente();
        UUID segundoId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        segunda.setId(segundoId);
        segunda.setDestinatarioEmail("ana@auditores.cr");

        when(notificacionTransicionAuditoriaRepository
                .findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaCreacionAsc(any(), anyInt()))
                .thenReturn(List.of(pendiente(), segunda));
        when(notificacionTransicionEstadoEnvioService.reclamar(segundoId)).thenReturn(true);
        doThrow(new IllegalStateException("smtp caido"))
                .when(emailTransicionAuditoriaService)
                .enviarCambioEstado(eq("contacto@acme.cr"), anyString(), anyString(), anyString(), anyString());

        service.enviarPendientes();

        verify(notificacionTransicionEstadoEnvioService).marcarFallidaSiAgotoIntentos(NOTIFICACION_ID);
        verify(notificacionTransicionEstadoEnvioService).marcarEnviada(segundoId);
    }

    private static NotificacionTransicionAuditoria pendiente() {
        return NotificacionTransicionAuditoria.builder()
                .id(NOTIFICACION_ID)
                .solicitud(SolicitudAuditoria.builder().id(SOLICITUD_ID).build())
                .destinatarioEmail("contacto@acme.cr")
                .destinatarioNombre("Acme S.A.")
                .nombreEmpresa("Acme S.A.")
                .estadoAnterior(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .estadoNuevo(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO)
                .estado(EstadoNotificacionTransicion.PENDIENTE)
                .fechaCreacion(Instant.parse("2026-08-01T10:00:00Z"))
                .build();
    }
}
