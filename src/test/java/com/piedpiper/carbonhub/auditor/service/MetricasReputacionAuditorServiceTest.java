package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.DistribucionSectorAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MetricasReputacionAuditorServiceTest {

    private static final UUID AUDITOR_ID =
            UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-10T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;

    private MetricasReputacionAuditorService service;

    @BeforeEach
    void configurar() {
        service = new MetricasReputacionAuditorService(
                perfilAuditorRepository,
                solicitudAuditoriaRepository,
                CLOCK);
    }

    @Test
    void primeraAuditoriaActualizaMetricasPersistidas() {
        PerfilAuditor perfil = perfil();
        preparar(perfil, List.of(solicitud(SectorIndustrial.AGROINDUSTRIA, 1)));

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isEqualTo(1);
        assertThat(perfil.getTiempoPromedioRespuestaDias()).isEqualByComparingTo("1.0");
        assertThat(perfil.getDistribucionSectores()).singleElement()
                .satisfies(distribucion -> {
                    assertThat(distribucion.getSector()).isEqualTo("AGROINDUSTRIA");
                    assertThat(distribucion.getCantidad()).isEqualTo(1);
                    assertThat(distribucion.getPorcentaje()).isEqualByComparingTo("100.0");
                });
    }

    @Test
    void acumulacionCalculaPorcentajesConUnDecimal() {
        PerfilAuditor perfil = perfil();
        preparar(perfil, List.of(
                solicitud(SectorIndustrial.AGROINDUSTRIA, 1),
                solicitud(SectorIndustrial.MANUFACTURA, 3)));

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isEqualTo(2);
        assertThat(perfil.getTiempoPromedioRespuestaDias()).isEqualByComparingTo("2.0");
        assertThat(perfil.getDistribucionSectores())
                .extracting(DistribucionSectorAuditor::getPorcentaje)
                .containsExactly(new BigDecimal("50.0"), new BigDecimal("50.0"));
    }

    @Test
    void auditoriaSinFechaRespuestaCuentaPeroNoAfectaPromedio(CapturedOutput output) {
        PerfilAuditor perfil = perfil();
        SolicitudAuditoria sinRespuesta = solicitud(SectorIndustrial.SERVICIOS, 1);
        sinRespuesta.setFechaAceptacion(null);
        preparar(perfil, List.of(solicitud(SectorIndustrial.SERVICIOS, 2), sinRespuesta));

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isEqualTo(2);
        assertThat(perfil.getTiempoPromedioRespuestaDias()).isEqualByComparingTo("2.0");
        assertThat(perfil.getDistribucionSectores()).singleElement()
                .satisfies(distribucion -> assertThat(distribucion.getCantidad()).isEqualTo(2));
        assertThat(output).contains("falta fechaPrimeraRespuesta");
    }

    @Test
    void sectorNuloSeExcluyeDeDistribucionSinRomperCalculo(CapturedOutput output) {
        PerfilAuditor perfil = perfil();
        preparar(perfil, List.of(
                solicitud(SectorIndustrial.AGROINDUSTRIA, 1),
                solicitud(null, 1)));

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isEqualTo(2);
        assertThat(perfil.getDistribucionSectores()).singleElement()
                .satisfies(distribucion -> {
                    assertThat(distribucion.getSector()).isEqualTo("AGROINDUSTRIA");
                    assertThat(distribucion.getCantidad()).isEqualTo(1);
                    assertThat(distribucion.getPorcentaje()).isEqualByComparingTo("50.0");
                });
        assertThat(output).contains("falta sectorEmpresa");
    }

    @Test
    void sinAuditoriasCompletadasDejaMetricasSinDatos() {
        PerfilAuditor perfil = perfil();
        perfil.setAuditoriasCompletadas(9);
        perfil.setTiempoPromedioRespuestaDias(new BigDecimal("3.5"));
        perfil.getDistribucionSectores()
                .add(new DistribucionSectorAuditor("SERVICIOS", 9, new BigDecimal("100.0")));
        preparar(perfil, List.of());

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isNull();
        assertThat(perfil.getTiempoPromedioRespuestaDias()).isNull();
        assertThat(perfil.getDistribucionSectores()).isEmpty();
    }

    @Test
    void soloSolicitaEstadosCompletadosAlRepositorio() {
        PerfilAuditor perfil = perfil();
        preparar(perfil, List.of());

        service.recalcular(AUDITOR_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EstadoSolicitudAuditoria>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(solicitudAuditoriaRepository).listarCompletadasPorAuditor(eq(AUDITOR_ID), captor.capture());
        assertThat(captor.getValue()).containsExactly(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
    }

    private void preparar(PerfilAuditor perfil, List<SolicitudAuditoria> solicitudes) {
        when(perfilAuditorRepository.findByAuditorIdConDistribucion(AUDITOR_ID))
                .thenReturn(Optional.of(perfil));
        when(solicitudAuditoriaRepository.listarCompletadasPorAuditor(eq(AUDITOR_ID), any()))
                .thenReturn(solicitudes);
    }

    private PerfilAuditor perfil() {
        return PerfilAuditor.builder()
                .auditor(Usuario.builder().id(AUDITOR_ID).build())
                .build();
    }

    private SolicitudAuditoria solicitud(SectorIndustrial sector, int diasRespuesta) {
        return SolicitudAuditoria.builder()
                .id(UUID.randomUUID())
                .estado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA)
                .empresa(Empresa.builder().sectorIndustrial(sector).build())
                .fechaAsignacion(Instant.parse("2026-08-01T10:00:00Z"))
                .fechaAceptacion(Instant.parse("2026-08-01T10:00:00Z")
                        .plusSeconds(diasRespuesta * 86_400L))
                .build();
    }
}
