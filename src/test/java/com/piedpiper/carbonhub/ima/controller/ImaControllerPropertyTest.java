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
import com.piedpiper.carbonhub.ima.service.ImaService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

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
 * Tests basados en propiedades para ImaController.
 *
 * Valida: Requisitos 3.3, 3.4, 3.5
 */
class ImaControllerPropertyTest {

    private ImaService imaService;
    private ImaBenchmarkService imaBenchmarkService;
    private ImaController controller;
    private Authentication authentication;

    // Configuración simple de mocks para Property 7 tests
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
        // Mock simple para tests de Property 7
        imaService = mock(ImaService.class);
        imaBenchmarkService = mock(ImaBenchmarkService.class);
        controller = new ImaController(imaService, imaBenchmarkService, mock(com.piedpiper.carbonhub.ima.service.ImaTendenciaService.class));
        authentication = new TestingAuthenticationToken(
                "41ce47ab-a46c-4306-8c46-2688dc97fa73", "password", "ROLE_ADMINISTRADOR_EMPRESA");

        // Configuración completa para tests de Property 8
        imaSnapshotRepository = mock(ImaSnapshotRepository.class);
        agregadoSectorialRepository = mock(AgregadoSectorialRepository.class);
        emisionRepository = mock(EmisionRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);

        imaSnapshotMapper = mock(ImaSnapshotMapper.class);
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
    // Valida: Requisitos 3.3
    // =========================================================================

    /**
     * Property 7a: Año menor a 2000 produce HTTP 400.
     *
     * Para cualquier anio < 2000, el controller lanza ApiException con BAD_REQUEST.
     *
     * **Valida: Requisitos 3.3**
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
     * Property 7b: Año mayor al actual produce HTTP 400.
     *
     * Para cualquier anio > año actual, el controller lanza ApiException con BAD_REQUEST.
     *
     * **Valida: Requisitos 3.3**
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
     * Property 7c: Mes menor a 1 produce HTTP 400.
     *
     * Para cualquier mes < 1, el controller lanza ApiException con BAD_REQUEST.
     *
     * **Valida: Requisitos 3.3**
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
     * Property 7d: Mes mayor a 12 produce HTTP 400.
     *
     * Para cualquier mes > 12, el controller lanza ApiException con BAD_REQUEST.
     *
     * **Valida: Requisitos 3.3**
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
     * Property 7e: Período futuro (mismo año, mes > mes actual) produce HTTP 400.
     *
     * Para el año actual con mes > mes actual, el controller lanza ApiException con BAD_REQUEST.
     *
     * **Valida: Requisitos 3.3**
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
    // Valida: Requisitos 3.4, 3.5
    // =========================================================================

    /**
     * Property 8: Para cualquier ImaSnapshot con interpretación persistida (valor real
     * o "No disponible"), la respuesta de obtenerIma contiene los campos
     * `interpretacion` y `siguientePaso` con exactamente los valores almacenados.
     *
     * **Valida: Requisitos 3.4, 3.5**
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
                .as("La interpretacion en la respuesta debe coincidir exactamente con el valor persistido")
                .isEqualTo(interpretacion);
        assertThat(result.getSiguientePaso())
                .as("El siguientePaso en la respuesta debe coincidir exactamente con el valor persistido")
                .isEqualTo(siguientePaso);
    }

    /**
     * Property 8b: Específicamente para valores "No disponible", la respuesta
     * contiene "No disponible" exactamente como está persistido.
     *
     * **Valida: Requisitos 3.4, 3.5**
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
                .as("Debe retornar 'No disponible' exactamente como está persistido")
                .isEqualTo("No disponible");
        assertThat(result.getSiguientePaso())
                .as("Debe retornar 'No disponible' exactamente como está persistido")
                .isEqualTo("No disponible");
    }

    // =========================================================================
    // Arbitraries personalizados
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
            // Si el mes actual es diciembre, no hay mes futuro válido en el mismo año dentro de 1-12
            // Genera mes 13+ que dispara validación de rango antes de la validación de futuro
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
