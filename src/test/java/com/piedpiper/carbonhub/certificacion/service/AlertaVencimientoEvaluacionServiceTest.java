package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoEvaluacionServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private AlertaVencimientoEvaluacionService service;

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID CERTIFICACION_ID = UUID.randomUUID();

    @Test
    void certificacionA90DiasGeneraAlerta90Dias() {
        Certificacion certificacion = certificacionConVencimientoEn(90);

        service.evaluar(certificacion);

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        Alerta guardada = captor.getValue();
        assertThat(guardada.getTipoAlerta()).isEqualTo(TipoAlerta.DIAS_90);
        assertThat(guardada.getCertificacion()).isEqualTo(certificacion);
        assertThat(guardada.getEmpresa()).isEqualTo(certificacion.getEmpresa());
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.PENDIENTE);
        assertThat(guardada.getFechaGeneracion()).isNotNull();
    }

    @Test
    void certificacionA30DiasGeneraAlerta30Dias() {
        Certificacion certificacion = certificacionConVencimientoEn(30);

        service.evaluar(certificacion);

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoAlerta()).isEqualTo(TipoAlerta.DIAS_30);
    }

    @Test
    void certificacionA7DiasGeneraAlerta7Dias() {
        Certificacion certificacion = certificacionConVencimientoEn(7);

        service.evaluar(certificacion);

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoAlerta()).isEqualTo(TipoAlerta.DIAS_7);
    }

    @Test
    void certificacionFueraDeLosUmbralesNoGeneraAlerta() {
        Certificacion certificacion = certificacionConVencimientoEn(45);

        service.evaluar(certificacion);

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void noDuplicaAlertaSiYaExisteParaElMismoUmbral() {
        Certificacion certificacion = certificacionConVencimientoEn(30);
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_30))
                .thenReturn(true);

        service.evaluar(certificacion);

        verify(alertaRepository, never()).save(any());
    }

    @Test
    void consultaExistenciaConElIdDeLaCertificacionYElUmbralCalculado() {
        Certificacion certificacion = certificacionConVencimientoEn(7);

        service.evaluar(certificacion);

        verify(alertaRepository).existsByCertificacionIdAndTipoAlerta(eq(CERTIFICACION_ID), eq(TipoAlerta.DIAS_7));
    }

    private static Certificacion certificacionConVencimientoEn(int dias) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(CERTIFICACION_ID)
                .empresa(empresa)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(LocalDate.now().plusDays(dias))
                .build();
    }
}
