package com.piedpiper.carbonhub.certificacion.service;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoSchedulerServiceTest {

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private AlertaVencimientoEvaluacionService alertaVencimientoEvaluacionService;

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
}
