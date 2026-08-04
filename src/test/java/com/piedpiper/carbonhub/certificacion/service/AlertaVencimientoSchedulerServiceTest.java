package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoSchedulerServiceTest {

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private AlertaVencimientoEvaluacionService alertaVencimientoEvaluacionService;
    @Mock
    private AlertaVencimientoNotificacionService alertaVencimientoNotificacionService;

    @InjectMocks
    private AlertaVencimientoSchedulerService service;

    @Test
    void invocaElServicioDeEvaluacionParaCadaCertificacionActiva() {
        Certificacion certificacionA = Certificacion.builder().id(UUID.randomUUID()).build();
        Certificacion certificacionB = Certificacion.builder().id(UUID.randomUUID()).build();
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(certificacionA, certificacionB));

        service.evaluarVencimientos();

        verify(alertaVencimientoEvaluacionService).evaluar(certificacionA.getId());
        verify(alertaVencimientoEvaluacionService).evaluar(certificacionB.getId());
    }

    @Test
    void errorAlEvaluarUnaCertificacionNoDetieneElProcesamientoDeLasDemas() {
        Certificacion certificacionConError = Certificacion.builder().id(UUID.randomUUID()).build();
        Certificacion certificacionOk = Certificacion.builder().id(UUID.randomUUID()).build();
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(certificacionConError, certificacionOk));
        doThrow(new RuntimeException("fallo inesperado"))
                .when(alertaVencimientoEvaluacionService).evaluar(certificacionConError.getId());

        service.evaluarVencimientos();

        verify(alertaVencimientoEvaluacionService).evaluar(certificacionConError.getId());
        verify(alertaVencimientoEvaluacionService).evaluar(certificacionOk.getId());
    }

    @Test
    void soloConsultaCertificacionesConEstadoActiva() {
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA)).thenReturn(List.of());

        service.evaluarVencimientos();

        verify(certificacionRepository).findByEstado(EstadoCertificacion.ACTIVA);
    }

    @Test
    void notificaCadaAlertaGenerada() {
        Certificacion certificacion = Certificacion.builder().id(UUID.randomUUID()).build();
        Alerta primera = Alerta.builder().id(UUID.randomUUID()).build();
        Alerta segunda = Alerta.builder().id(UUID.randomUUID()).build();
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(certificacion));
        when(alertaVencimientoEvaluacionService.evaluar(certificacion.getId()))
                .thenReturn(List.of(primera, segunda));

        service.evaluarVencimientos();

        verify(alertaVencimientoNotificacionService).notificar(primera.getId());
        verify(alertaVencimientoNotificacionService).notificar(segunda.getId());
    }

    @Test
    void cuandoNoSeGeneraNingunaAlertaNoSeNotificaNada() {
        Certificacion certificacion = Certificacion.builder().id(UUID.randomUUID()).build();
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(certificacion));
        when(alertaVencimientoEvaluacionService.evaluar(certificacion.getId())).thenReturn(List.of());

        service.evaluarVencimientos();

        verifyNoInteractions(alertaVencimientoNotificacionService);
    }

    @Test
    void unFalloAlNotificarUnaAlertaNoImpideNotificarLasSiguientes() {
        Certificacion certificacion = Certificacion.builder().id(UUID.randomUUID()).build();
        Alerta conError = Alerta.builder().id(UUID.randomUUID()).build();
        Alerta siguiente = Alerta.builder().id(UUID.randomUUID()).build();
        when(certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(certificacion));
        when(alertaVencimientoEvaluacionService.evaluar(certificacion.getId()))
                .thenReturn(List.of(conError, siguiente));
        doThrow(new RuntimeException("fallo inesperado"))
                .when(alertaVencimientoNotificacionService).notificar(conError.getId());

        service.evaluarVencimientos();

        verify(alertaVencimientoNotificacionService).notificar(siguiente.getId());
    }
}
