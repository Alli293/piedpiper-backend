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

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;
import net.jqwik.api.Tag;
import org.assertj.core.groups.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilPublicoAuditorMetricasPropertyTest {

    private static final UUID AUDITOR_ID =
            UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-10T12:00:00Z"), ZoneId.of("UTC"));

    @Property(tries = 100)
    @Tag("PP55")
    void recalculoMantieneConteoPromedioYDistribucionDeSolicitudesCompletadas(
            @ForAll("solicitudesCompletadas") List<SolicitudAuditoria> solicitudes) {
        PerfilAuditor perfil = PerfilAuditor.builder()
                .auditor(Usuario.builder().id(AUDITOR_ID).build())
                .build();
        PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        SolicitudAuditoriaRepository solicitudAuditoriaRepository =
                mock(SolicitudAuditoriaRepository.class);
        when(perfilAuditorRepository.findByAuditorIdConDistribucion(AUDITOR_ID))
                .thenReturn(Optional.of(perfil));
        when(solicitudAuditoriaRepository.listarCompletadasPorAuditor(eq(AUDITOR_ID), any()))
                .thenReturn(solicitudes);
        MetricasReputacionAuditorService service = new MetricasReputacionAuditorService(
                perfilAuditorRepository,
                solicitudAuditoriaRepository,
                CLOCK);

        service.recalcular(AUDITOR_ID);

        assertThat(perfil.getAuditoriasCompletadas()).isEqualTo(solicitudes.size());
        assertThat(perfil.getTiempoPromedioRespuestaDias())
                .isEqualByComparingTo(tiempoPromedioEsperado(solicitudes));
        assertThat(perfil.getDistribucionSectores())
                .extracting(
                        DistribucionSectorAuditor::getSector,
                        DistribucionSectorAuditor::getCantidad,
                        DistribucionSectorAuditor::getPorcentaje)
                .containsExactlyElementsOf(distribucionEsperada(solicitudes));
    }

    @Provide
    Arbitrary<List<SolicitudAuditoria>> solicitudesCompletadas() {
        return solicitudCompletada().list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<SolicitudAuditoria> solicitudCompletada() {
        Arbitrary<SectorIndustrial> sector = Arbitraries.of(SectorIndustrial.values());
        Arbitrary<Integer> horasRespuesta = Arbitraries.integers().between(1, 720);

        return Combinators.combine(sector, horasRespuesta)
                .as((sectorIndustrial, horas) -> {
                    Instant fechaAsignacion = Instant.parse("2026-08-01T10:00:00Z");
                    return SolicitudAuditoria.builder()
                            .id(UUID.randomUUID())
                            .estado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA)
                            .empresa(Empresa.builder().sectorIndustrial(sectorIndustrial).build())
                            .fechaAsignacion(fechaAsignacion)
                            .fechaAceptacion(fechaAsignacion.plus(Duration.ofHours(horas)))
                            .build();
                });
    }

    private BigDecimal tiempoPromedioEsperado(List<SolicitudAuditoria> solicitudes) {
        BigDecimal totalDias = solicitudes.stream()
                .map(solicitud -> BigDecimal.valueOf(Duration.between(
                                solicitud.getFechaAsignacion(),
                                solicitud.getFechaAceptacion()).toSeconds())
                        .divide(BigDecimal.valueOf(86_400), 10, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalDias.divide(BigDecimal.valueOf(solicitudes.size()), 1, RoundingMode.HALF_UP);
    }

    private List<Tuple> distribucionEsperada(List<SolicitudAuditoria> solicitudes) {
        Map<String, Long> conteos = solicitudes.stream()
                .map(solicitud -> solicitud.getEmpresa().getSectorIndustrial().name())
                .collect(Collectors.groupingBy(sector -> sector, Collectors.counting()));

        return conteos.entrySet().stream()
                .map(entry -> new DistribucionSectorAuditor(
                        entry.getKey(),
                        Math.toIntExact(entry.getValue()),
                        porcentaje(entry.getValue(), solicitudes.size())))
                .sorted(Comparator
                        .comparing(DistribucionSectorAuditor::getPorcentaje)
                        .reversed()
                        .thenComparing(DistribucionSectorAuditor::getSector))
                .map(distribucion -> tuple(
                        distribucion.getSector(),
                        distribucion.getCantidad(),
                        distribucion.getPorcentaje()))
                .toList();
    }

    private BigDecimal porcentaje(long cantidad, int total) {
        return BigDecimal.valueOf(cantidad * 100)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
