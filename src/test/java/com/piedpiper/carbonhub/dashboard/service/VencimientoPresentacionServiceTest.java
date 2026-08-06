package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VencimientoPresentacionServiceTest {

    @Mock
    private CatalogoTiposCertificacion catalogoTiposCertificacion;

    @InjectMocks
    private VencimientoPresentacionService service;

    @Test
    void nombreLegibleSaleDelCatalogo() {
        when(catalogoTiposCertificacion.buscar(TipoCertificacion.CARBONO_NEUTRAL)).thenReturn(Optional.of(
                new DefinicionCertificacion(TipoCertificacion.CARBONO_NEUTRAL, "Carbono Neutral",
                        "descripcion", 12, TipoLogroOpenBadges.CERTIFICATE, "criterio")));

        String nombre = service.nombreLegible(certificacion(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(nombre).isEqualTo("Carbono Neutral");
    }

    @Test
    void nombreLegibleUsaElCodigoComoRespaldoSiNoHayDefinicion() {
        lenient().when(catalogoTiposCertificacion.buscar(TipoCertificacion.CARBONO_NEUTRAL))
                .thenReturn(Optional.empty());

        String nombre = service.nombreLegible(certificacion(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(nombre).isEqualTo("carbono_neutral");
    }

    @Test
    void urgenciaEsVencidaCuandoDiasRestantesEsCero() {
        assertThat(service.urgenciaPara(0)).isEqualTo("vencida");
    }

    @Test
    void urgenciaEsVencidaCuandoDiasRestantesEsNegativo() {
        assertThat(service.urgenciaPara(-24)).isEqualTo("vencida");
    }

    @Test
    void urgenciaEsElUmbralMasEstrictoQueCalza() {
        assertThat(service.urgenciaPara(5)).isEqualTo("7_dias");
        assertThat(service.urgenciaPara(7)).isEqualTo("7_dias");
        assertThat(service.urgenciaPara(20)).isEqualTo("30_dias");
        assertThat(service.urgenciaPara(88)).isEqualTo("90_dias");
    }

    @Test
    void urgenciaCaeEnElUmbralMasLaxoCuandoSuperaLos90Dias() {
        assertThat(service.urgenciaPara(400)).isEqualTo("90_dias");
    }

    private static Certificacion certificacion(TipoCertificacion tipo) {
        Empresa empresa = Empresa.builder().id(UUID.randomUUID()).build();
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .tipo(tipo)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(LocalDate.now())
                .build();
    }
}
