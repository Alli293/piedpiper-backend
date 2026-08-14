package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Prueba únicamente la parte de base de datos de la recomendación de
 * renovación (selección determinista + cálculo de impacto): sin
 * {@code RecomendacionRenovacionIaService} de por medio, porque esta
 * clase ya no la conoce — ver el porqué de la separación en su javadoc.
 */
@ExtendWith(MockitoExtension.class)
class RecomendacionRenovacionConsultaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.now(ZonasHorarias.COSTA_RICA);

    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private CatalogoTiposCertificacion catalogoTiposCertificacion;

    private RecomendacionRenovacionConsultaService service;

    @BeforeEach
    void setUp() {
        service = new RecomendacionRenovacionConsultaService(
                emisionEmpresaService,
                certificacionRepository,
                emisionRepository,
                catalogoTiposCertificacion,
                new VencimientoPresentacionService(catalogoTiposCertificacion),
                new RecomendacionRenovacionSeleccionService());
        lenient().when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        lenient().when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        stubDefinicion(TipoCertificacion.CARBONO_NEUTRAL, "Carbono Neutral", 12);
        stubDefinicion(TipoCertificacion.INVENTARIO_GEI, "Inventario de GEI", 12);
    }

    @Test
    void sinCertificacionesConAlertaNoHayPrioritaria() {
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        assertThat(service.obtenerCertificacionPrioritaria(USUARIO_ID)).isEmpty();
    }

    @Test
    void sinCertificacionesDentroDelUmbralNoHayPrioritaria() {
        Certificacion lejana = certificacion(HOY.plusDays(120), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(lejana));

        assertThat(service.obtenerCertificacionPrioritaria(USUARIO_ID)).isEmpty();
    }

    @Test
    void unaCertificacionAExactamente90DiasSeIncluye() {
        Certificacion a90 = certificacion(HOY.plusDays(90), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(a90));

        assertThat(service.obtenerCertificacionPrioritaria(USUARIO_ID)).isPresent();
    }

    @Test
    void unaCertificacionA91DiasSeExcluye() {
        Certificacion a91 = certificacion(HOY.plusDays(91), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(a91));

        assertThat(service.obtenerCertificacionPrioritaria(USUARIO_ID)).isEmpty();
    }

    @Test
    void identificaLaCertificacionPrioritariaPorDiasRestantes() {
        Certificacion a30 = certificacion(HOY.plusDays(30), TipoCertificacion.INVENTARIO_GEI);
        Certificacion a7 = certificacion(HOY.plusDays(7), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(a7, a30));

        Optional<CertAlertaDTO> prioritaria = service.obtenerCertificacionPrioritaria(USUARIO_ID);

        assertThat(prioritaria).isPresent();
        assertThat(prioritaria.get().getNombreCertificacion()).isEqualTo("Carbono Neutral");
        assertThat(prioritaria.get().getDiasRestantes()).isEqualTo(7);
    }

    @Test
    void calculaElImpactoEnHuellaSumandoLasEmisionesDeLaVentanaDeVigencia() {
        Certificacion cert = certificacion(HOY.plusDays(7), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(cert));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(new BigDecimal("30000"));

        Optional<CertAlertaDTO> prioritaria = service.obtenerCertificacionPrioritaria(USUARIO_ID);

        assertThat(prioritaria).isPresent();
        assertThat(prioritaria.get().getImpactoHuellaT()).isEqualByComparingTo("30.0000");
    }

    @Test
    void sinRegistrosDeEmisionElImpactoEsCero() {
        Certificacion cert = certificacion(HOY.plusDays(7), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(cert));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(null);

        Optional<CertAlertaDTO> prioritaria = service.obtenerCertificacionPrioritaria(USUARIO_ID);

        assertThat(prioritaria).isPresent();
        assertThat(prioritaria.get().getImpactoHuellaT()).isEqualByComparingTo("0.0000");
    }

    private void stubDefinicion(TipoCertificacion tipo, String nombre, int vigenciaMeses) {
        lenient().when(catalogoTiposCertificacion.buscar(tipo)).thenReturn(Optional.of(
                new DefinicionCertificacion(tipo, nombre, "descripcion", vigenciaMeses,
                        TipoLogroOpenBadges.CERTIFICATE, "criterio")));
    }

    private static Certificacion certificacion(LocalDate fechaVencimiento, TipoCertificacion tipo) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .tipo(tipo)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaEmision(Instant.now())
                .fechaVencimiento(fechaVencimiento)
                .build();
    }
}
