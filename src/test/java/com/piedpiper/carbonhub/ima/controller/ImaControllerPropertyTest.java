package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.mappers.ImaSnapshotMapper;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.ima.service.ImaBenchmarkService;
import com.piedpiper.carbonhub.ima.service.ImaInterpretacionService;
import com.piedpiper.carbonhub.ima.service.ImaService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ImaController.
 *
 * Validates: Requirements 3.3, 3.4, 3.5
 */
class ImaControllerPropertyTest {

    private ImaService imaService;
    private ImaBenchmarkService imaBenchmarkService;
    private ImaController controller;
    private Authentication authentication;

    // For Property 8 tests
    private ImaSnapshotRepository imaSnapshotRepository;
    private AgregadoSectorialRepository agregadoSectorialRepository;
    private EmisionRepository emisionRepository;
    private EmpresaRepository empresaRepository;
    private UsuarioRepository usuarioRepository;
    private ImaSnapshotMapper imaSnapshotMapper;
    private ImaService imaServiceReal;

    private UUID empresaId;
    private UUID usuarioId;

    @BeforeTry
    void setUp() {
        // Simple mock for Property 7 tests
        imaService = Mockito.mock(ImaService.class);
        imaBenchmarkService = Mockito.mock(ImaBenchmarkService.class);
        controller = new ImaController(imaService, imaBenchmarkService, Mockito.mock(com.piedpiper.carbonhub.ima.service.ImaTendenciaService.class));
        authentication = new TestingAuthenticationToken(
                "41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA");

        // Full setup for Property 8 tests
        imaSnapshotRepository = Mockito.mock(ImaSnapshotRepository.class);
        agregadoSectorialRepository = Mockito.mock(AgregadoSectorialRepository.class);
        emisionRepository = Mockito.mock(EmisionRepository.class);
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);

        imaSnapshotMapper = Mockito.mock(ImaSnapshotMapper.class);
        when(imaSnapshotMapper.toDto(any(ImaSnapshot.class))).thenAnswer(invocation -> {
            ImaSnapshot s = invocation.getArgument(0);
            return ImaResponseDTO.builder()
                    .cobertura(s.getCobertura())
                    .puntajeIntensidadSectorial(s.getPuntajeIntensidadSectorial())
                    .consistencia(s.getConsistencia())
                    .ima(s.getIma())
                    .parcial(s.isParcial())
                    .motivoParcial(s.getMotivoParcial())
                    .intensidad(s.getIntensidad())
                    .calculatedAt(s.getCalculatedAt())
                    .interpretacion(s.getInterpretacion())
                    .siguientePaso(s.getSiguientePaso())
                    .build();
        });

        empresaId = UUID.randomUUID();
        usuarioId = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");

        Empresa empresa = Empresa.builder()
                .id(empresaId)
                .nombreEmpresa("TestCorp")
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .cantidadEmpleados(50)
                .build();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("test@test.com")
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .empresa(empresa)
                .build();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        imaServiceReal = new ImaService(
                imaSnapshotRepository,
                agregadoSectorialRepository,
                emisionRepository,
                empresaRepository,
                usuarioRepository,
                imaSnapshotMapper
        );
    }

    // =========================================================================
    // Property 7: Parámetros de período inválidos producen HTTP 400
    // Validates: Requirements 3.3
    // =========================================================================

    /**
     * Property 7a: Year below 2000 produces HTTP 400.
     *
     * For any anio < 2000, the controller SHALL throw ApiException with BAD_REQUEST.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("property-7")
    void anioMenorA2000ProduceHttp400(
            @ForAll @IntRange(min = -10000, max = 1999) int anioInvalido
    ) {
        int mesValido = 6;

        assertThatThrownBy(() -> controller.obtenerIma(authentication, anioInvalido, mesValido))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * Property 7b: Year above current year produces HTTP 400.
     *
     * For any anio > current year, the controller SHALL throw ApiException with BAD_REQUEST.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("property-7")
    void anioMayorAlActualProduceHttp400(
            @ForAll("anioFuturo") int anioInvalido
    ) {
        int mesValido = 1;

        assertThatThrownBy(() -> controller.obtenerIma(authentication, anioInvalido, mesValido))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * Property 7c: Month below 1 produces HTTP 400.
     *
     * For any mes < 1, the controller SHALL throw ApiException with BAD_REQUEST.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("property-7")
    void mesMenorA1ProduceHttp400(
            @ForAll @IntRange(min = -10000, max = 0) int mesInvalido
    ) {
        int anioValido = 2024;

        assertThatThrownBy(() -> controller.obtenerIma(authentication, anioValido, mesInvalido))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * Property 7d: Month above 12 produces HTTP 400.
     *
     * For any mes > 12, the controller SHALL throw ApiException with BAD_REQUEST.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("property-7")
    void mesMayorA12ProduceHttp400(
            @ForAll @IntRange(min = 13, max = 10000) int mesInvalido
    ) {
        int anioValido = 2024;

        assertThatThrownBy(() -> controller.obtenerIma(authentication, anioValido, mesInvalido))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    /**
     * Property 7e: Future period (same year, month > current month) produces HTTP 400.
     *
     * For the current year with mes > current month, the controller SHALL throw
     * ApiException with BAD_REQUEST.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("property-7")
    void periodoFuturoMismoAnioProduceHttp400(
            @ForAll("mesFuturoMismoAnio") int mesFuturo
    ) {
        int anioActual = LocalDate.now().getYear();

        assertThatThrownBy(() -> controller.obtenerIma(authentication, anioActual, mesFuturo))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // =========================================================================
    // Property 8: Interpretación persistida se retorna íntegramente en la respuesta
    // Validates: Requirements 3.4, 3.5
    // =========================================================================

    /**
     * Property 8: For any ImaSnapshot with interpretation persisted (real value
     * or "No disponible"), the response from obtenerIma SHALL contain the fields
     * `interpretacion` and `siguientePaso` with exactly the stored values.
     *
     * **Validates: Requirements 3.4, 3.5**
     */
    @Property(tries = 100)
    @Tag("property-8")
    void interpretacionPersistidaSeRetornaIntegramenteEnLaRespuesta(
            @ForAll("randomInterpretacion") String interpretacion,
            @ForAll("randomSiguientePaso") String siguientePaso,
            @ForAll @IntRange(min = 2020, max = 2025) int anio,
            @ForAll @IntRange(min = 1, max = 12) int mes
    ) {
        ImaSnapshot existingSnapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(empresaId)
                .anio(anio)
                .mes(mes)
                .cobertura(new BigDecimal("75.0"))
                .puntajeIntensidadSectorial(new BigDecimal("60.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .intensidad(new BigDecimal("1.5"))
                .calculatedAt(Instant.now())
                .interpretacion(interpretacion)
                .siguientePaso(siguientePaso)
                .build();

        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(empresaId, anio, mes))
                .thenReturn(Optional.of(existingSnapshot));

        ImaResponseDTO result = imaServiceReal.obtenerIma(anio, mes, usuarioId);

        assertThat(result.getInterpretacion())
                .as("Response interpretacion must match persisted value exactly")
                .isEqualTo(interpretacion);
        assertThat(result.getSiguientePaso())
                .as("Response siguientePaso must match persisted value exactly")
                .isEqualTo(siguientePaso);
    }

    /**
     * Property 8b: Specifically for "No disponible" values, the response
     * SHALL contain "No disponible" exactly as stored.
     *
     * **Validates: Requirements 3.4, 3.5**
     */
    @Property(tries = 100)
    @Tag("property-8")
    void noDisponibleSeRetornaExactamenteComoEstaPersistido(
            @ForAll @IntRange(min = 2020, max = 2025) int anio,
            @ForAll @IntRange(min = 1, max = 12) int mes
    ) {
        ImaSnapshot existingSnapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(empresaId)
                .anio(anio)
                .mes(mes)
                .cobertura(new BigDecimal("50.0"))
                .consistencia(new BigDecimal("40.0"))
                .ima(new BigDecimal("45.0"))
                .parcial(true)
                .motivoParcial("Sin datos suficientes")
                .calculatedAt(Instant.now())
                .interpretacion("No disponible")
                .siguientePaso("No disponible")
                .build();

        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(empresaId, anio, mes))
                .thenReturn(Optional.of(existingSnapshot));

        ImaResponseDTO result = imaServiceReal.obtenerIma(anio, mes, usuarioId);

        assertThat(result.getInterpretacion())
                .as("Response should return 'No disponible' exactly as persisted")
                .isEqualTo("No disponible");
        assertThat(result.getSiguientePaso())
                .as("Response should return 'No disponible' exactly as persisted")
                .isEqualTo("No disponible");
    }

    // =========================================================================
    // Custom Arbitraries
    // =========================================================================

    @Provide
    Arbitrary<Integer> anioFuturo() {
        int currentYear = LocalDate.now().getYear();
        return Arbitraries.integers().between(currentYear + 1, currentYear + 1000);
    }

    @Provide
    Arbitrary<Integer> mesFuturoMismoAnio() {
        int currentMonth = LocalDate.now().getMonthValue();
        if (currentMonth >= 12) {
            // If current month is December, no valid future month in same year within 1-12 range
            // Generate month 13+ which triggers month-out-of-range validation before future check
            return Arbitraries.integers().between(13, 13);
        }
        return Arbitraries.integers().between(currentMonth + 1, 12);
    }

    @Provide
    Arbitrary<String> randomInterpretacion() {
        return Arbitraries.oneOf(
                Arbitraries.just("No disponible"),
                Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .withChars(' ', ',', '.', ':', ';', '-')
                        .ofMinLength(10)
                        .ofMaxLength(300)
                        .filter(s -> !s.isBlank())
        );
    }

    @Provide
    Arbitrary<String> randomSiguientePaso() {
        return Arbitraries.oneOf(
                Arbitraries.just("No disponible"),
                Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .withChars(' ', ',', '.', ':', ';', '-')
                        .ofMinLength(5)
                        .ofMaxLength(200)
                        .filter(s -> !s.isBlank())
        );
    }
}
