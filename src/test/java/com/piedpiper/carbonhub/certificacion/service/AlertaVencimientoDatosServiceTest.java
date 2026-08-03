package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.dtos.AlertaVencimientoNotificacionDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoDatosServiceTest {

    private static final UUID ALERTA_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String PLANTILLA_URL =
            "https://carbonhub.cr/empresa/{slug}/reputacion/certificaciones";

    @Mock
    private AlertaRepository alertaRepository;

    private AlertaVencimientoDatosService service;

    @BeforeEach
    void configurar() {
        service = new AlertaVencimientoDatosService(
                alertaRepository, new CatalogoTiposCertificacion(), PLANTILLA_URL);
    }

    @Test
    void armaElCorreoConElNombreDelCatalogoLaUrlDeLaEmpresaYElCodigoDelUmbral() {
        when(alertaRepository.buscarConEmpresaYCertificacion(ALERTA_ID))
                .thenReturn(Optional.of(alerta(TipoAlerta.DIAS_30, TipoCertificacion.CARBONO_NEUTRAL, 30)));

        AlertaVencimientoNotificacionDTO datos = service.datosDe(ALERTA_ID).orElseThrow();

        assertThat(datos.getIdAlerta()).isEqualTo(ALERTA_ID);
        assertThat(datos.getCorreoDestinatario()).isEqualTo("contacto@acme.cr");
        assertThat(datos.getNombreEmpresa()).isEqualTo("Acme S.A.");
        assertThat(datos.getTipoAlerta()).isEqualTo("30_dias");
        assertThat(datos.getUrlCertificacion())
                .isEqualTo("https://carbonhub.cr/empresa/acme-sa/reputacion/certificaciones");
        assertThat(datos.getNombreCertificacion())
                .isNotBlank()
                .isNotEqualTo(TipoCertificacion.CARBONO_NEUTRAL.getCodigo());
    }

    @Test
    void losDiasRestantesSeCalculanContraLaFechaRealDeVencimientoYNoContraElUmbral() {
        when(alertaRepository.buscarConEmpresaYCertificacion(ALERTA_ID))
                .thenReturn(Optional.of(alerta(TipoAlerta.DIAS_90, TipoCertificacion.CARBONO_NEUTRAL, 84)));

        AlertaVencimientoNotificacionDTO datos = service.datosDe(ALERTA_ID).orElseThrow();

        assertThat(datos.getTipoAlerta()).isEqualTo("90_dias");
        assertThat(datos.getDiasRestantes())
                .as("el umbral es 90 pero faltan 84 dias reales, el correo debe decir la verdad")
                .isEqualTo(84);
    }

    @Test
    void unaCertificacionYaVencidaDaDiasRestantesNegativos() {
        when(alertaRepository.buscarConEmpresaYCertificacion(ALERTA_ID))
                .thenReturn(Optional.of(alerta(TipoAlerta.DIAS_7, TipoCertificacion.CARBONO_NEUTRAL, -3)));

        assertThat(service.datosDe(ALERTA_ID).orElseThrow().getDiasRestantes()).isEqualTo(-3);
    }

    @Test
    void unaAlertaInexistenteDevuelveVacio() {
        when(alertaRepository.buscarConEmpresaYCertificacion(ALERTA_ID)).thenReturn(Optional.empty());

        assertThat(service.datosDe(ALERTA_ID)).isEmpty();
    }

    private static Alerta alerta(TipoAlerta tipoAlerta, TipoCertificacion tipo, int diasHastaVencer) {
        Empresa empresa = Empresa.builder()
                .nombreEmpresa("Acme S.A.")
                .correoCorporativo("contacto@acme.cr")
                .slug("acme-sa")
                .build();
        Certificacion certificacion = Certificacion.builder()
                .tipo(tipo)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(LocalDate.now(ZonasHorarias.COSTA_RICA).plusDays(diasHastaVencer))
                .build();
        return Alerta.builder()
                .id(ALERTA_ID)
                .empresa(empresa)
                .certificacion(certificacion)
                .tipoAlerta(tipoAlerta)
                .estado(EstadoAlerta.PENDIENTE)
                .fechaGeneracion(Instant.now())
                .build();
    }
}
