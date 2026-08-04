package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.CertificacionActivaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PosicionBenchmark;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ResultadoPriorizacion;
import com.piedpiper.carbonhub.ecoruta.models.entities.RegistroPonderacion;
import com.piedpiper.carbonhub.ecoruta.repository.RegistroPonderacionRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeProperty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios y de propiedad para PriorizacionAmbientalService.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.5, 4.1, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 6.5
 */
class PriorizacionAmbientalServiceTest {

    private static final BigDecimal FACTOR_BOOST = new BigDecimal("0.15");

    // These are initialized per-property via @BeforeProperty for jqwik tests
    private PuntuacionAmbientalCalculator calculator;

    @BeforeProperty
    void setUpProperty() {
        calculator = new PuntuacionAmbientalCalculator();
    }

    // ========================================================================
    // Property Tests
    // ========================================================================

    /**
     * Property 3: No-exclusión de establecimientos.
     * La lista de salida contiene exactamente los mismos establecimientos que la entrada.
     *
     * Validates: Requirements 3.2, 3.3, 6.4
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 3: No-exclusión de establecimientos")
    void noExclusionDeEstablecimientos(@ForAll @IntRange(min = 1, max = 10) int cantidad) {
        // Create fresh mocks for each try
        IndicadorAmbientalClient localIndicador = Mockito.mock(IndicadorAmbientalClient.class);
        ImaClient localIma = Mockito.mock(ImaClient.class);
        BenchmarkClient localBench = Mockito.mock(BenchmarkClient.class);
        RegistroPonderacionRepository localRepo = Mockito.mock(RegistroPonderacionRepository.class);
        PriorizacionAmbientalService localService = new PriorizacionAmbientalService(
                localIndicador, localIma, localBench, calculator, localRepo);
        ReflectionTestUtils.setField(localService, "factorBoost", FACTOR_BOOST);

        Mockito.when(localIndicador.consultarIndicadores(anyList())).thenReturn(Map.of());
        Mockito.when(localIma.consultarIma(anyList())).thenReturn(Map.of());
        Mockito.when(localBench.consultarBenchmark(anyList())).thenReturn(Map.of());
        Mockito.when(localRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        List<EstablecimientoRankeado> input = crearEstablecimientos(cantidad);
        Set<UUID> idsInput = input.stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .collect(Collectors.toSet());

        ResultadoPriorizacion resultado = localService.aplicarPriorizacion(
                input, UUID.randomUUID(), UUID.randomUUID());

        Set<UUID> idsOutput = resultado.getEstablecimientosRankeados().stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .collect(Collectors.toSet());

        assertThat(idsOutput).isEqualTo(idsInput);
        assertThat(resultado.getEstablecimientosRankeados()).hasSameSizeAs(input);
    }

    /**
     * Property 2: Dominancia de afinidad de preferencias.
     * Si relevancia turística de A > B, entonces A aparece antes que B en el ranking final
     * independientemente de puntuaciones ambientales (dado FACTOR_BOOST=0.15 es pequeño).
     *
     * Validates: Requirements 3.1, 3.5
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 2: Dominancia de afinidad de preferencias")
    void dominanciaDePreferencias(@ForAll("diferenciaTuristicaSignificativa") BigDecimal[] scores) {
        BigDecimal scoreAlto = scores[0];
        BigDecimal scoreBajo = scores[1];

        // Create fresh mocks with max indicators for each try
        IndicadorAmbientalClient localIndicador = Mockito.mock(IndicadorAmbientalClient.class);
        ImaClient localIma = Mockito.mock(ImaClient.class);
        BenchmarkClient localBench = Mockito.mock(BenchmarkClient.class);
        RegistroPonderacionRepository localRepo = Mockito.mock(RegistroPonderacionRepository.class);
        PriorizacionAmbientalService localService = new PriorizacionAmbientalService(
                localIndicador, localIma, localBench, calculator, localRepo);
        ReflectionTestUtils.setField(localService, "factorBoost", FACTOR_BOOST);

        Mockito.when(localIndicador.consultarIndicadores(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            Map<UUID, IndicadorAmbientalDTO> result = new HashMap<>();
            for (UUID id : ids) {
                List<CertificacionActivaDTO> certs = IntStream.range(0, 5)
                        .mapToObj(i -> new CertificacionActivaDTO(UUID.randomUUID(), "Cert" + i,
                                "activa", Instant.now().minus(10, ChronoUnit.DAYS)))
                        .collect(Collectors.toList());
                result.put(id, new IndicadorAmbientalDTO(id, certs, Instant.now()));
            }
            return result;
        });
        Mockito.when(localIma.consultarIma(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            Map<UUID, IMADTO> result = new HashMap<>();
            for (UUID id : ids) {
                result.put(id, new IMADTO(id, new BigDecimal("100"), false, Instant.now()));
            }
            return result;
        });
        Mockito.when(localBench.consultarBenchmark(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            Map<UUID, BenchmarkDTO> result = new HashMap<>();
            for (UUID id : ids) {
                result.put(id, new BenchmarkDTO(id, PosicionBenchmark.LIDER,
                        BigDecimal.TEN, BigDecimal.TEN, Instant.now()));
            }
            return result;
        });
        Mockito.when(localRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();

        EstablecimientoRankeado estA = new EstablecimientoRankeado(
                empresaA, "Alto", scoreAlto, null, null, null);
        EstablecimientoRankeado estB = new EstablecimientoRankeado(
                empresaB, "Bajo", scoreBajo, null, null, null);

        List<EstablecimientoRankeado> input = new ArrayList<>(List.of(estA, estB));
        ResultadoPriorizacion resultado = localService.aplicarPriorizacion(
                input, UUID.randomUUID(), UUID.randomUUID());

        List<UUID> ordenFinal = resultado.getEstablecimientosRankeados().stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .toList();

        assertThat(ordenFinal.indexOf(empresaA)).isLessThan(ordenFinal.indexOf(empresaB));
    }

    /**
     * Property 5: Degradación graciosa.
     * Cualquier combinación de fallos en servicios no lanza excepciones y retorna lista válida.
     *
     * Validates: Requirements 6.1, 6.2, 6.3, 6.5
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 5: Degradación graciosa")
    void degradacionGraciosa(
            @ForAll @IntRange(min = 1, max = 5) int cantidadEstablecimientos,
            @ForAll("combinacionFallos") boolean[] fallos) {

        // Create fresh mocks for each try to avoid stacking stubs
        IndicadorAmbientalClient localIndicador = Mockito.mock(IndicadorAmbientalClient.class);
        ImaClient localIma = Mockito.mock(ImaClient.class);
        BenchmarkClient localBench = Mockito.mock(BenchmarkClient.class);
        RegistroPonderacionRepository localRepo = Mockito.mock(RegistroPonderacionRepository.class);
        PriorizacionAmbientalService localService = new PriorizacionAmbientalService(
                localIndicador, localIma, localBench, calculator, localRepo);
        ReflectionTestUtils.setField(localService, "factorBoost", FACTOR_BOOST);

        if (fallos[0]) {
            Mockito.when(localIndicador.consultarIndicadores(anyList()))
                    .thenThrow(new RuntimeException("service unavailable"));
        } else {
            Mockito.when(localIndicador.consultarIndicadores(anyList())).thenReturn(Map.of());
        }
        if (fallos[1]) {
            Mockito.when(localIma.consultarIma(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
        } else {
            Mockito.when(localIma.consultarIma(anyList())).thenReturn(Map.of());
        }
        if (fallos[2]) {
            Mockito.when(localBench.consultarBenchmark(anyList()))
                    .thenThrow(new RuntimeException("connection refused"));
        } else {
            Mockito.when(localBench.consultarBenchmark(anyList())).thenReturn(Map.of());
        }
        Mockito.when(localRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        List<EstablecimientoRankeado> input = crearEstablecimientos(cantidadEstablecimientos);

        assertThatNoException().isThrownBy(() -> {
            ResultadoPriorizacion resultado = localService.aplicarPriorizacion(
                    input, UUID.randomUUID(), UUID.randomUUID());

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEstablecimientosRankeados()).isNotNull();
            assertThat(resultado.getEstablecimientosRankeados()).hasSameSizeAs(input);
        });
    }

    /**
     * Property 6: Completitud del registro de auditoría.
     * Puntuación total = suma ponderada de componentes. itinerarioId y usuarioId no nulos.
     *
     * Validates: Requirements 5.1, 5.2, 5.3
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 6: Completitud del registro de auditoría")
    void completitudRegistroAuditoria(@ForAll @IntRange(min = 1, max = 5) int cantidadEstablecimientos) {
        // Create fresh mocks for each try to avoid invocation counter accumulation
        RegistroPonderacionRepository localRepo = Mockito.mock(RegistroPonderacionRepository.class);
        IndicadorAmbientalClient localIndicador = Mockito.mock(IndicadorAmbientalClient.class);
        ImaClient localIma = Mockito.mock(ImaClient.class);
        BenchmarkClient localBench = Mockito.mock(BenchmarkClient.class);
        PriorizacionAmbientalService localService = new PriorizacionAmbientalService(
                localIndicador, localIma, localBench, calculator, localRepo);
        ReflectionTestUtils.setField(localService, "factorBoost", FACTOR_BOOST);

        Mockito.when(localIndicador.consultarIndicadores(anyList())).thenReturn(Map.of());
        Mockito.when(localIma.consultarIma(anyList())).thenReturn(Map.of());
        Mockito.when(localBench.consultarBenchmark(anyList())).thenReturn(Map.of());
        Mockito.when(localRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<RegistroPonderacion> captor = ArgumentCaptor.forClass(RegistroPonderacion.class);

        List<EstablecimientoRankeado> input = crearEstablecimientos(cantidadEstablecimientos);
        UUID itinerarioId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        localService.aplicarPriorizacion(input, itinerarioId, usuarioId);

        Mockito.verify(localRepo, times(cantidadEstablecimientos)).save(captor.capture());

        for (RegistroPonderacion registro : captor.getAllValues()) {
            assertThat(registro.getItinerarioId()).isEqualTo(itinerarioId);
            assertThat(registro.getUsuarioId()).isEqualTo(usuarioId);
            assertThat(registro.getEmpresaId()).isNotNull();

            BigDecimal sumaComponentes = registro.getComponenteCertificaciones()
                    .add(registro.getComponenteIma())
                    .add(registro.getComponenteBenchmark());
            assertThat(registro.getPuntuacionTotal()).isEqualByComparingTo(sumaComponentes);
        }
    }

    // ========================================================================
    // Providers for jqwik
    // ========================================================================

    @Provide
    Arbitrary<BigDecimal[]> diferenciaTuristicaSignificativa() {
        // Generate pairs where the difference is > 15 (max possible environmental boost)
        return Arbitraries.integers().between(50, 100)
                .flatMap(alto -> Arbitraries.integers().between(0, alto - 16)
                        .map(bajo -> new BigDecimal[]{BigDecimal.valueOf(alto), BigDecimal.valueOf(bajo)}));
    }

    @Provide
    Arbitrary<boolean[]> combinacionFallos() {
        return Arbitraries.of(true, false)
                .tuple3()
                .map(t -> new boolean[]{t.get1(), t.get2(), t.get3()});
    }

    // ========================================================================
    // Unit Tests (Task 4.6)
    // ========================================================================

    @Nested
    @DisplayName("Unit Tests - PriorizacionAmbientalService")
    @ExtendWith(MockitoExtension.class)
    class UnitTests {

        @Mock
        private IndicadorAmbientalClient mockIndicadorClient;
        @Mock
        private ImaClient mockImaClient;
        @Mock
        private BenchmarkClient mockBenchmarkClient;
        @Mock
        private RegistroPonderacionRepository mockRepository;

        private PriorizacionAmbientalService unitService;

        @BeforeEach
        void setUp() {
            PuntuacionAmbientalCalculator calc = new PuntuacionAmbientalCalculator();
            unitService = new PriorizacionAmbientalService(
                    mockIndicadorClient, mockImaClient, mockBenchmarkClient,
                    calc, mockRepository);
            ReflectionTestUtils.setField(unitService, "factorBoost", FACTOR_BOOST);
        }

        @Test
        @DisplayName("Integración de componentes: consulta → cálculo → persistencia")
        void integracionComponentes() {
            UUID empresaId = UUID.randomUUID();
            UUID itinerarioId = UUID.randomUUID();
            UUID usuarioId = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaId, "Test", new BigDecimal("80"),
                            null, null, null)));

            Map<UUID, IndicadorAmbientalDTO> indicadores = Map.of(empresaId,
                    new IndicadorAmbientalDTO(empresaId, List.of(
                            new CertificacionActivaDTO(UUID.randomUUID(), "ISO14001", "activa",
                                    Instant.now().minus(30, ChronoUnit.DAYS))),
                            Instant.now()));
            Map<UUID, IMADTO> imaMap = Map.of(empresaId,
                    new IMADTO(empresaId, new BigDecimal("70"), false, Instant.now()));
            Map<UUID, BenchmarkDTO> benchMap = Map.of(empresaId,
                    new BenchmarkDTO(empresaId, PosicionBenchmark.ARRIBA_PROMEDIO,
                            BigDecimal.TEN, BigDecimal.TEN, Instant.now()));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(indicadores);
            when(mockImaClient.consultarIma(anyList())).thenReturn(imaMap);
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(benchMap);
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = unitService.aplicarPriorizacion(
                    input, itinerarioId, usuarioId);

            assertThat(resultado.getEstablecimientosRankeados()).hasSize(1);
            EstablecimientoRankeado est = resultado.getEstablecimientosRankeados().get(0);
            assertThat(est.getPuntuacionAmbiental()).isNotNull();
            assertThat(est.getPuntuacionAmbiental()).isGreaterThan(BigDecimal.ZERO);
            assertThat(est.getPuntuacionFinal()).isGreaterThan(est.getPuntuacionTuristica());

            verify(mockRepository).save(any(RegistroPonderacion.class));
        }

        @Test
        @DisplayName("Degradación: fallo de un servicio (indicadores)")
        void degradacionUnServicioFalla() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaId, "Test", new BigDecimal("50"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
            when(mockImaClient.consultarIma(anyList())).thenReturn(Map.of(empresaId,
                    new IMADTO(empresaId, new BigDecimal("60"), false, Instant.now())));
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of());
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = unitService.aplicarPriorizacion(
                    input, UUID.randomUUID(), UUID.randomUUID());

            assertThat(resultado.getEstablecimientosRankeados()).hasSize(1);
            assertThat(resultado.getIndicadoresNoDisponibles()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Degradación: fallo de dos servicios")
        void degradacionDosServiciosFallan() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaId, "Test", new BigDecimal("50"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
            when(mockImaClient.consultarIma(anyList()))
                    .thenThrow(new RuntimeException("connection refused"));
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of(empresaId,
                    new BenchmarkDTO(empresaId, PosicionBenchmark.PROMEDIO,
                            BigDecimal.TEN, BigDecimal.TEN, Instant.now())));
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = unitService.aplicarPriorizacion(
                    input, UUID.randomUUID(), UUID.randomUUID());

            assertThat(resultado.getEstablecimientosRankeados()).hasSize(1);
            assertThat(resultado.getIndicadoresNoDisponibles()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Degradación: fallo de todos los servicios")
        void degradacionTodosLosFallan() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaId, "Test", new BigDecimal("50"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
            when(mockImaClient.consultarIma(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
            when(mockBenchmarkClient.consultarBenchmark(anyList()))
                    .thenThrow(new RuntimeException("timeout"));
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = unitService.aplicarPriorizacion(
                    input, UUID.randomUUID(), UUID.randomUUID());

            assertThat(resultado.getEstablecimientosRankeados()).hasSize(1);
            assertThat(resultado.getIndicadoresNoDisponibles()).isEqualTo(3);
            EstablecimientoRankeado est = resultado.getEstablecimientosRankeados().get(0);
            assertThat(est.getPuntuacionFinal()).isEqualByComparingTo(new BigDecimal("50"));
        }

        @Test
        @DisplayName("Ranking: boost no invierte orden de relevancia turística mayor")
        void boostNoInvierteOrdenTuristico() {
            UUID empresaAlta = UUID.randomUUID();
            UUID empresaBaja = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaAlta, "Alta", new BigDecimal("90"),
                            null, null, null),
                    new EstablecimientoRankeado(empresaBaja, "Baja", new BigDecimal("30"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());
            when(mockImaClient.consultarIma(anyList())).thenReturn(Map.of(empresaBaja,
                    new IMADTO(empresaBaja, new BigDecimal("100"), false, Instant.now())));
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of(empresaBaja,
                    new BenchmarkDTO(empresaBaja, PosicionBenchmark.LIDER,
                            BigDecimal.TEN, BigDecimal.TEN, Instant.now())));
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = unitService.aplicarPriorizacion(
                    input, UUID.randomUUID(), UUID.randomUUID());

            assertThat(resultado.getEstablecimientosRankeados().get(0).getEmpresaId())
                    .isEqualTo(empresaAlta);
        }

        @Test
        @DisplayName("Persistencia transaccional: se persiste un registro por establecimiento")
        void persistenciaTransaccional() {
            UUID empresa1 = UUID.randomUUID();
            UUID empresa2 = UUID.randomUUID();
            UUID itinerarioId = UUID.randomUUID();
            UUID usuarioId = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresa1, "E1", new BigDecimal("60"),
                            null, null, null),
                    new EstablecimientoRankeado(empresa2, "E2", new BigDecimal("40"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());
            when(mockImaClient.consultarIma(anyList())).thenReturn(Map.of());
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of());
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            unitService.aplicarPriorizacion(input, itinerarioId, usuarioId);

            ArgumentCaptor<RegistroPonderacion> captor =
                    ArgumentCaptor.forClass(RegistroPonderacion.class);
            verify(mockRepository, times(2)).save(captor.capture());

            List<RegistroPonderacion> registros = captor.getAllValues();
            Set<UUID> empresaIds = registros.stream()
                    .map(RegistroPonderacion::getEmpresaId)
                    .collect(Collectors.toSet());

            assertThat(empresaIds).containsExactlyInAnyOrder(empresa1, empresa2);
            for (RegistroPonderacion reg : registros) {
                assertThat(reg.getItinerarioId()).isEqualTo(itinerarioId);
                assertThat(reg.getUsuarioId()).isEqualTo(usuarioId);
            }
        }
    }

    // ========================================================================
    // Integration Tests (Task 6.3)
    // ========================================================================

    @Nested
    @DisplayName("Integration Tests - Flujo completo")
    @ExtendWith(MockitoExtension.class)
    class IntegrationTests {

        @Mock
        private IndicadorAmbientalClient mockIndicadorClient;
        @Mock
        private ImaClient mockImaClient;
        @Mock
        private BenchmarkClient mockBenchmarkClient;
        @Mock
        private RegistroPonderacionRepository mockRepository;

        private PriorizacionAmbientalService intService;

        @BeforeEach
        void setUp() {
            PuntuacionAmbientalCalculator calc = new PuntuacionAmbientalCalculator();
            intService = new PriorizacionAmbientalService(
                    mockIndicadorClient, mockImaClient, mockBenchmarkClient,
                    calc, mockRepository);
            ReflectionTestUtils.setField(intService, "factorBoost", FACTOR_BOOST);
        }

        @Test
        @DisplayName("Flujo generar → priorización automática: resultado incluye puntuaciones")
        void flujoGenerarConPriorizacion() {
            UUID empresa1 = UUID.randomUUID();
            UUID empresa2 = UUID.randomUUID();
            UUID itinerarioId = UUID.randomUUID();
            UUID usuarioId = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresa1, "Hotel Verde", new BigDecimal("85"),
                            null, null, null),
                    new EstablecimientoRankeado(empresa2, "Lodge Eco", new BigDecimal("70"),
                            null, null, null)));

            Map<UUID, IndicadorAmbientalDTO> indicadores = Map.of(
                    empresa1, new IndicadorAmbientalDTO(empresa1, List.of(
                            new CertificacionActivaDTO(UUID.randomUUID(), "ISO14001", "activa",
                                    Instant.now().minus(60, ChronoUnit.DAYS)),
                            new CertificacionActivaDTO(UUID.randomUUID(), "CST", "activa",
                                    Instant.now().minus(90, ChronoUnit.DAYS))),
                            Instant.now()),
                    empresa2, new IndicadorAmbientalDTO(empresa2, List.of(
                            new CertificacionActivaDTO(UUID.randomUUID(), "Bandera Azul", "activa",
                                    Instant.now().minus(200, ChronoUnit.DAYS))),
                            Instant.now()));

            Map<UUID, IMADTO> imaMap = Map.of(
                    empresa1, new IMADTO(empresa1, new BigDecimal("75"), false, Instant.now()));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(indicadores);
            when(mockImaClient.consultarIma(anyList())).thenReturn(imaMap);
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of());
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = intService.aplicarPriorizacion(
                    input, itinerarioId, usuarioId);

            assertThat(resultado.getEstablecimientosRankeados()).hasSize(2);
            for (EstablecimientoRankeado est : resultado.getEstablecimientosRankeados()) {
                assertThat(est.getPuntuacionAmbiental()).isNotNull();
                assertThat(est.getPuntuacionFinal()).isNotNull();
                assertThat(est.getDetalleAmbiental()).isNotNull();
            }
            verify(mockRepository, times(2)).save(any(RegistroPonderacion.class));
        }

        @Test
        @DisplayName("Transaccionalidad: fallo de persistencia propaga excepción (rollback)")
        void transaccionalidadFalloPersistencia() {
            UUID empresa1 = UUID.randomUUID();
            UUID empresa2 = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresa1, "E1", new BigDecimal("80"),
                            null, null, null),
                    new EstablecimientoRankeado(empresa2, "E2", new BigDecimal("60"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());
            when(mockImaClient.consultarIma(anyList())).thenReturn(Map.of());
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of());
            when(mockRepository.save(any()))
                    .thenAnswer(i -> i.getArgument(0))
                    .thenThrow(new RuntimeException("DB constraint violation"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                    intService.aplicarPriorizacion(input, UUID.randomUUID(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("Autorización: servicio procesa cualquier UUID (auth en controller)")
        void autorizacionEnControllerLevel() {
            UUID empresaId = UUID.randomUUID();
            UUID itinerarioId = UUID.randomUUID();
            UUID usuarioId = UUID.randomUUID();

            List<EstablecimientoRankeado> input = new ArrayList<>(List.of(
                    new EstablecimientoRankeado(empresaId, "Test", new BigDecimal("50"),
                            null, null, null)));

            when(mockIndicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());
            when(mockImaClient.consultarIma(anyList())).thenReturn(Map.of());
            when(mockBenchmarkClient.consultarBenchmark(anyList())).thenReturn(Map.of());
            when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ResultadoPriorizacion resultado = intService.aplicarPriorizacion(
                    input, itinerarioId, usuarioId);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEstablecimientosRankeados()).hasSize(1);
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private List<EstablecimientoRankeado> crearEstablecimientos(int cantidad) {
        List<EstablecimientoRankeado> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(new EstablecimientoRankeado(
                    UUID.randomUUID(),
                    "Establecimiento-" + i,
                    BigDecimal.valueOf(50 + i * 5),
                    null, null, null));
        }
        return lista;
    }
}
