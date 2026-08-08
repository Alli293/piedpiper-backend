package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.NotificacionTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.NotificacionTransicionAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificacionTransicionRegistroServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");

    @Mock
    private NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository;

    private NotificacionTransicionRegistroService service;

    @BeforeEach
    void configurar() {
        service = new NotificacionTransicionRegistroService(notificacionTransicionAuditoriaRepository);
    }

    @Test
    void encolaUnaNotificacionParaLaEmpresaYOtraParaElAuditor() {
        service.encolar(solicitud("contacto@empresa.cr", "auditora@carbonhub.cr"),
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.AUDITOR_ACEPTA);

        assertThat(encoladas())
                .extracting(NotificacionTransicionAuditoria::getDestinatarioEmail)
                .containsExactly("contacto@empresa.cr", "auditora@carbonhub.cr");
    }

    /** Sin auditor asignado la empresa igual tiene que enterarse: es la que espera la respuesta. */
    @Test
    void sinAuditorAsignadoSoloSeEncolaLaDeLaEmpresa() {
        service.encolar(solicitud("contacto@empresa.cr", null),
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA);

        assertThat(encoladas())
                .extracting(NotificacionTransicionAuditoria::getDestinatarioEmail)
                .containsExactly("contacto@empresa.cr");
    }

    @Test
    void laNotificacionQuedaPendienteConLosDosEstadosDeLaTransicion() {
        service.encolar(solicitud("contacto@empresa.cr", null),
                EstadoSolicitudAuditoria.EN_REVISION,
                EstadoSolicitudAuditoria.REPORTE_CARGADO,
                EventoTransicionAuditoria.REPORTE_CARGADO);

        NotificacionTransicionAuditoria notificacion = encoladas().get(0);
        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacionTransicion.PENDIENTE);
        assertThat(notificacion.getEstadoAnterior()).isEqualTo(EstadoSolicitudAuditoria.EN_REVISION);
        assertThat(notificacion.getEstadoNuevo()).isEqualTo(EstadoSolicitudAuditoria.REPORTE_CARGADO);
        assertThat(notificacion.getNombreEmpresa()).isEqualTo("Empresa Demo");
        assertThat(notificacion.getFechaCreacion()).isNotNull();
    }

    /**
     * Encolar una direccion que no se puede entregar gastaria los tres intentos del barrido antes
     * de darse por vencida, y cada intento es una conexion al servidor de correo.
     */
    @Test
    void unCorreoConFormatoInvalidoNoSeEncolaYNoCortaElResto() {
        service.encolar(solicitud("no-es-un-correo", "auditora@carbonhub.cr"),
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.AUDITOR_ACEPTA);

        assertThat(encoladas())
                .extracting(NotificacionTransicionAuditoria::getDestinatarioEmail)
                .containsExactly("auditora@carbonhub.cr");
    }

    @Test
    void unCorreoInvalidoNoLanzaNiInterrumpeLaTransicion() {
        assertThatCode(() -> service.encolar(solicitud("no-es-un-correo", null),
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.AUDITOR_ACEPTA))
                .doesNotThrowAnyException();

        verifyNoInteractions(notificacionTransicionAuditoriaRepository);
    }

    @Test
    void unCorreoNuloTampocoSeEncola() {
        service.encolar(solicitud(null, null),
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.AUDITOR_ACEPTA);

        verify(notificacionTransicionAuditoriaRepository, never())
                .save(any(NotificacionTransicionAuditoria.class));
    }

    private List<NotificacionTransicionAuditoria> encoladas() {
        ArgumentCaptor<NotificacionTransicionAuditoria> captor =
                ArgumentCaptor.forClass(NotificacionTransicionAuditoria.class);
        verify(notificacionTransicionAuditoriaRepository, org.mockito.Mockito.atLeastOnce())
                .save(captor.capture());
        return captor.getAllValues();
    }

    private static SolicitudAuditoria solicitud(String correoEmpresa, String correoAuditor) {
        SolicitudAuditoria solicitud = SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .nombreEmpresa("Empresa Demo")
                        .correoCorporativo(correoEmpresa)
                        .build())
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .build();

        if (correoAuditor != null) {
            solicitud.setAuditor(Usuario.builder()
                    .id(UUID.fromString("c0ffee00-1111-2222-3333-444455556666"))
                    .nombre("Ana")
                    .apellidos("Auditora")
                    .email(correoAuditor)
                    .build());
        }
        return solicitud;
    }
}
