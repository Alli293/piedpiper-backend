package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorTransicionAuditoriaTest {

    private final ValidadorTransicionAuditoria validador = new ValidadorTransicionAuditoria();

    @Test
    void aceptacionDelAuditorLlevaDeSolicitudEnviadaAAuditorAsignado() {
        assertThat(validador.destinoDe(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.AUDITOR))
                .isEqualTo(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO);
    }

    @Test
    void elInicioDeRevisionLlevaDeAuditorAsignadoAEnRevision() {
        assertThat(validador.destinoDe(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EventoTransicionAuditoria.INICIO_REVISION,
                ActorTransicionAuditoria.AUDITOR))
                .isEqualTo(EstadoSolicitudAuditoria.EN_REVISION);
    }

    @Test
    void elRechazoDelAuditorNoCambiaElEstado() {
        assertThat(validador.destinoDe(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.AUDITOR_RECHAZA,
                ActorTransicionAuditoria.AUDITOR))
                .isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    @Test
    void elVencimientoLoOriginaElSistemaYTampocoCambiaElEstado() {
        assertThat(validador.destinoDe(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA,
                ActorTransicionAuditoria.SISTEMA))
                .isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    @Test
    void unResultadoAprobadoDesdeReporteCargadoEmiteLaCertificacion() {
        assertThat(validador.destinoDe(EstadoSolicitudAuditoria.REPORTE_CARGADO,
                EventoTransicionAuditoria.RESULTADO_APROBADA,
                ActorTransicionAuditoria.AUDITOR))
                .isEqualTo(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
    }

    @Test
    void unaCombinacionQueNoEstaEnLaTablaDevuelve422() {
        assertThatThrownBy(() -> validador.destinoDe(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.AUDITOR))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void unActorDistintoDelAutorizadoParaEseEventoDevuelve422() {
        assertThatThrownBy(() -> validador.destinoDe(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.AUDITOR_ACEPTA,
                ActorTransicionAuditoria.EMPRESA))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void laEmpresaNoPuedeDisparaElVencimientoQueLeCorrespondeAlSistema() {
        assertThatThrownBy(() -> validador.destinoDe(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA,
                ActorTransicionAuditoria.EMPRESA))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void noSePuedeSaltarDeSolicitudEnviadaDirectoAEnRevision() {
        assertThat(validador.destinoPermitido(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.INICIO_REVISION))
                .isEmpty();
    }

    @Test
    void unaCertificacionEmitidaEsUnEstadoFinalParaTodoEvento() {
        for (EventoTransicionAuditoria evento : EventoTransicionAuditoria.values()) {
            assertThat(validador.destinoPermitido(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA, evento))
                    .as("la certificacion emitida no deberia admitir el evento %s", evento)
                    .isEmpty();
        }
    }

    /**
     * Es a proposito, no un hueco de la tabla: cuando el auditor deja observaciones, la solicitud
     * queda cerrada y la empresa tiene que abrir una nueva con la documentacion corregida. No hay
     * camino de vuelta a revision sobre la misma solicitud, porque el periodo auditado y los
     * documentos ya no serian los que se revisaron.
     */
    @Test
    void observacionesPendientesEsUnEstadoFinalParaTodoEvento() {
        for (EventoTransicionAuditoria evento : EventoTransicionAuditoria.values()) {
            assertThat(validador.destinoPermitido(EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES, evento))
                    .as("una solicitud con observaciones no deberia admitir el evento %s", evento)
                    .isEmpty();
        }
    }
}
