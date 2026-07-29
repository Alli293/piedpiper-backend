package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoEvaluacionServiceTest {

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private AlertaVencimientoEvaluacionService service;

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID CERTIFICACION_ID = UUID.randomUUID();

    @BeforeEach
    void devolverLaMismaAlertaQueSeGuarda() {
        // save() de un mock devuelve null por defecto; el servicio arma el
        // resultado a partir de lo que save() devuelve, asi que el stub
        // hace que se comporte como el repositorio real (echo del argumento).
        lenient().when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void certificacionA90DiasSinAlertasPreviasGeneraSoloAlerta90Dias() {
        stubCertificacion(certificacionConVencimientoEn(90));

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).hasSize(1);
        Alerta alerta = generadas.get(0);
        assertThat(alerta.getTipoAlerta()).isEqualTo(TipoAlerta.DIAS_90);
        assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.PENDIENTE);
        assertThat(alerta.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        assertThat(alerta.getCertificacion().getId()).isEqualTo(CERTIFICACION_ID);
        assertThat(alerta.getFechaGeneracion()).isNotNull();
    }

    @Test
    void certificacionA30DiasConAlerta90YaExistenteGeneraSoloAlerta30Dias() {
        stubCertificacion(certificacionConVencimientoEn(30));
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_90))
                .thenReturn(true);

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta).containsExactly(TipoAlerta.DIAS_30);
    }

    @Test
    void certificacionA7DiasConAlertas90Y30YaExistentesGeneraSoloAlerta7Dias() {
        stubCertificacion(certificacionConVencimientoEn(7));
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_90))
                .thenReturn(true);
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_30))
                .thenReturn(true);

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta).containsExactly(TipoAlerta.DIAS_7);
    }

    @Test
    void certificacionA91DiasNoGeneraNingunaAlerta() {
        stubCertificacion(certificacionConVencimientoEn(91));

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).isEmpty();
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void certificacionA89DiasSinAlertaPreviaRecuperaElUmbralDe90DiasPerdido() {
        // El cron no corrio el dia exacto en que faltaban 90 dias; al correr
        // un dia despues (89 dias restantes) igual debe generar la alerta,
        // no perderla para siempre.
        stubCertificacion(certificacionConVencimientoEn(89));

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta).containsExactly(TipoAlerta.DIAS_90);
    }

    @Test
    void procesoQueNoCorrioUnDiaGeneraLaAlertaDe30DiasQueQuedoPendiente() {
        stubCertificacion(certificacionConVencimientoEn(29));
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_90))
                .thenReturn(true);

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta).containsExactly(TipoAlerta.DIAS_30);
    }

    @Test
    void certificacionA8DiasConAlertas90Y30YaExistentesNoGeneraAlerta7DiasAun() {
        stubCertificacion(certificacionConVencimientoEn(8));
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_90))
                .thenReturn(true);
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_30))
                .thenReturn(true);

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).isEmpty();
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void certificacionA6DiasConAlertas90Y30YaExistentesGeneraAlerta7Dias() {
        stubCertificacion(certificacionConVencimientoEn(6));
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_90))
                .thenReturn(true);
        when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, TipoAlerta.DIAS_30))
                .thenReturn(true);

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta).containsExactly(TipoAlerta.DIAS_7);
    }

    @Test
    void certificacionYaVencidaSinAlertasPreviasRecuperaLosTresUmbrales() {
        stubCertificacion(certificacionConVencimientoEn(-5));

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).extracting(Alerta::getTipoAlerta)
                .containsExactly(TipoAlerta.DIAS_90, TipoAlerta.DIAS_30, TipoAlerta.DIAS_7);
    }

    @Test
    void certificacionYaVencidaConLosTresUmbralesYaGeneradosNoGeneraNada() {
        stubCertificacion(certificacionConVencimientoEn(-5));
        for (TipoAlerta tipo : TipoAlerta.values()) {
            when(alertaRepository.existsByCertificacionIdAndTipoAlerta(CERTIFICACION_ID, tipo)).thenReturn(true);
        }

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).isEmpty();
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void certificacionInexistenteNoGeneraNada() {
        when(certificacionRepository.findById(CERTIFICACION_ID)).thenReturn(Optional.empty());

        List<Alerta> generadas = service.evaluar(CERTIFICACION_ID);

        assertThat(generadas).isEmpty();
        verify(alertaRepository, never()).save(any());
    }

    private void stubCertificacion(Certificacion certificacion) {
        when(certificacionRepository.findById(CERTIFICACION_ID)).thenReturn(Optional.of(certificacion));
    }

    private static Certificacion certificacionConVencimientoEn(int dias) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(CERTIFICACION_ID)
                .empresa(empresa)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(LocalDate.now(ZonasHorarias.COSTA_RICA).plusDays(dias))
                .build();
    }
}
