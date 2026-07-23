package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.mappers.ImaSnapshotMapper;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ImaService.
 *
 * Validates: Requirements 1.2, 2.6
 */
class ImaServicePropertyTest {

    private ImaSnapshotRepository imaSnapshotRepository;
    private AgregadoSectorialRepository agregadoSectorialRepository;
    private EmisionRepository emisionRepository;
    private EmpresaRepository empresaRepository;
    private UsuarioRepository usuarioRepository;
    private ImaInterpretacionService interpretacionService;
    private ImaSnapshotMapper imaSnapshotMapper;
    private ImaService imaService;

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();

    @BeforeTry
    void setUp() {
        imaSnapshotRepository = Mockito.mock(ImaSnapshotRepository.class);
        agregadoSectorialRepository = Mockito.mock(AgregadoSectorialRepository.class);
        emisionRepository = Mockito.mock(EmisionRepository.class);
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        interpretacionService = Mockito.mock(ImaInterpretacionService.class);
        imaSnapshotMapper = Mockito.mock(ImaSnapshotMapper.class);

        imaService = new ImaService(
                imaSnapshotRepository,
                agregadoSectorialRepository,
                emisionRepository,
                empresaRepository,
                usuarioRepository,
                interpretacionService,
                imaSnapshotMapper
        );

        // Setup: usuario -> empresa
        Empresa empresa = Empresa.builder()
                .id(EMPRESA_ID)
                .nombreEmpresa("TestEmpresa")
                .cedulaJuridica("3101000000")
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .cantidadEmpleados(50)
                .pais("Costa Rica")
                .correoCorporativo("test@test.com")
                .slug("test-empresa")
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        Usuario usuario = Usuario.builder()
                .id(USUARIO_ID)
                .email("user@test.com")
                .empresa(empresa)
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));

        // Setup: AgregadoSectorial for sector lookups
        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .sector(SectorIndustrial.SERVICIOS)
                .anio(2024)
                .mes(6)
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.5"))
                .calculatedAt(Instant.now())
                .build();
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.of(agregado));

        // Setup: mapper returns DTO with interpretacion/siguientePaso from snapshot
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
    }

    // =========================================================================
    // Property 3: Interpretación existente se reutiliza sin re-invocar ChatClient
    // Validates: Requirements 1.2
    // =========================================================================

    /**
     * Property 3: For any ImaSnapshot that already has interpretacion and siguientePaso
     * distinct from "No disponible" and not null, a query to the same period SHALL return
     * those values without invoking interpretacionService.generarInterpretacion.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 3: Interpretación existente se reutiliza sin re-invocar ChatClient")
    void interpretacionExistenteSeReutilizaSinReInvocarChatClient(
            @ForAll("validInterpretacion") String interpretacion,
            @ForAll("validSiguientePaso") String siguientePaso,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 2020, max = 2025) int anio,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 12) int mes
    ) {
        // Arrange: snapshot with valid interpretation already set
        ImaSnapshot existingSnapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(EMPRESA_ID)
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

        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(EMPRESA_ID, anio, mes))
                .thenReturn(Optional.of(existingSnapshot));

        // Act
        ImaResponseDTO result = imaService.obtenerIma(anio, mes, USUARIO_ID);

        // Assert: interpretacionService.generarInterpretacion was NEVER called
        verify(interpretacionService, never()).generarInterpretacion(
                any(ImaSnapshot.class), any(String.class), any(), any(String.class));

        // Assert: the returned DTO has the same interpretation values (cached, not regenerated)
        assertThat(result.getInterpretacion()).isEqualTo(interpretacion);
        assertThat(result.getSiguientePaso()).isEqualTo(siguientePaso);
    }

    // =========================================================================
    // Property 6: Reintento cuando interpretación previa es "No disponible"
    // Validates: Requirements 2.6
    // =========================================================================

    /**
     * Property 6: When obtenerIma finds a snapshot with interpretacion = "No disponible" or null,
     * it SHOULD invoke interpretacionService.generarInterpretacion to retry.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 6: Reintento cuando interpretación previa es No disponible")
    void reintentoGeneraInterpretacionCuandoPreviaEsNoDisponible(
            @ForAll("interpretacionNoDisponible") String interpretacionPrevia
    ) throws InterruptedException {
        int anio = 2024;
        int mes = 6;

        ImaSnapshot snapshotExistente = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(EMPRESA_ID)
                .anio(anio)
                .mes(mes)
                .cobertura(new BigDecimal("75.0"))
                .puntajeIntensidadSectorial(new BigDecimal("60.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .interpretacion(interpretacionPrevia)
                .siguientePaso(interpretacionPrevia)
                .build();

        // The snapshot exists in the repo
        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(EMPRESA_ID, anio, mes))
                .thenReturn(Optional.of(snapshotExistente));

        // Also mock the previous month lookup for tendencia calculation (no previous snapshot)
        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(eq(EMPRESA_ID), eq(2024), eq(5)))
                .thenReturn(Optional.empty());

        // Act
        imaService.obtenerIma(anio, mes, USUARIO_ID);

        // Assert: interpretacionService.generarInterpretacion is called asynchronously
        // Wait briefly for the async CompletableFuture to execute
        Thread.sleep(200);
        verify(interpretacionService, times(1)).generarInterpretacion(
                eq(snapshotExistente),
                any(String.class),
                any(AgregadoSectorial.class),
                any(String.class)
        );
    }

    // =========================================================================
    // Custom Arbitraries
    // =========================================================================

    @Provide
    Arbitrary<String> validInterpretacion() {
        // Generate random non-null, non-blank strings that are NOT "No disponible"
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(200)
                .filter(s -> !s.isBlank() && !"No disponible".equals(s));
    }

    @Provide
    Arbitrary<String> validSiguientePaso() {
        // Generate random non-null, non-blank strings that are NOT "No disponible"
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(200)
                .filter(s -> !s.isBlank() && !"No disponible".equals(s));
    }

    @Provide
    Arbitrary<String> interpretacionNoDisponible() {
        return Arbitraries.of(null, "No disponible");
    }
}
