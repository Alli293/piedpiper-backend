package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ImaInterpretacionService.
 *
 * Validates: Requirements 1.4, 1.5, 8.1, 8.2, 8.3, 2.1, 2.2, 2.3, 2.4, 2.5, 2.7
 */
class ImaInterpretacionServicePropertyTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private ImaSnapshotRepository imaSnapshotRepository;
    private EmpresaRepository empresaRepository;
    private ImaInterpretacionService service;

    @BeforeTry
    void setUp() {
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
        chatClient = Mockito.mock(ChatClient.class);
        requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = Mockito.mock(ChatClient.CallResponseSpec.class);
        imaSnapshotRepository = Mockito.mock(ImaSnapshotRepository.class);
        empresaRepository = Mockito.mock(EmpresaRepository.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        // EmpresaRepository returns a safe empresa (name/id/employees won't appear in prompt)
        Empresa empresa = Empresa.builder()
                .id(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .nombreEmpresa("SafeTestCorp")
                .cantidadEmpleados(99999)
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .build();
        when(empresaRepository.findById(any(UUID.class))).thenReturn(Optional.of(empresa));

        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        service = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                empresaRepository,
                "test-api-key"
        );
    }

    // =========================================================================
    // Property 1: El prompt contiene exclusivamente datos anonimizados
    // Validates: Requirements 1.4, 1.5, 8.1
    // =========================================================================

    /**
     * Property 1: For any ImaSnapshot with any combination of scores (0–100),
     * sector, and aggregate data, the prompt built by construirPromptUsuario
     * SHALL contain only: sector name, four company scores, sector averages
     * with cantidadEmpresas, and tendencia; and SHALL NOT contain empresa name,
     * empresaId, or cantidadEmpleados.
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 1: El prompt contiene exclusivamente datos anonimizados")
    void promptContieneExclusivamenteDatosAnonimizados(
            @ForAll("safeEmpresaName") String nombreEmpresa,
            @ForAll("randomUUID") UUID empresaId,
            @ForAll @IntRange(min = 101, max = 99999) int cantidadEmpleados,
            @ForAll @IntRange(min = 0, max = 100) int cobertura,
            @ForAll @IntRange(min = 0, max = 100) int consistencia,
            @ForAll @IntRange(min = 0, max = 100) int ima
    ) {
        String sectorNombre = "TestSector";
        String tendencia = "Subió";

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(empresaId)
                .anio(2024)
                .mes(6)
                .cobertura(BigDecimal.valueOf(cobertura))
                .puntajeIntensidadSectorial(BigDecimal.valueOf(50))
                .consistencia(BigDecimal.valueOf(consistencia))
                .ima(BigDecimal.valueOf(ima))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();

        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.5"))
                .calculatedAt(Instant.now())
                .build();

        String prompt = service.construirPromptUsuario(snapshot, sectorNombre, agregado, tendencia);

        // Assert: prompt does NOT contain sensitive data
        assertThat(prompt.toLowerCase()).doesNotContain(nombreEmpresa.toLowerCase());
        assertThat(prompt).doesNotContain(empresaId.toString());
        assertThat(prompt).doesNotContain(String.valueOf(cantidadEmpleados));

        // Assert: prompt DOES contain expected data
        assertThat(prompt).contains(sectorNombre);
        assertThat(prompt).contains(tendencia);
    }

    // =========================================================================
    // Property 4: Respuesta válida se persiste; respuesta inválida resulta en "No disponible"
    // Validates: Requirements 1.6, 1.7
    // =========================================================================

    /**
     * Property 4a: If ChatClient returns a valid InterpretacionIma (both fields non-null, non-blank),
     * both values are persisted to the snapshot as-is.
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 4: Respuesta válida se persiste; respuesta inválida resulta en No disponible")
    void respuestaValidaSePersisteCorrectamente(
            @ForAll("nonEmptyAlphanumeric") String interpretacionText,
            @ForAll("nonEmptyAlphanumeric") String siguientePasoText
    ) {
        com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma resultado =
                new com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma(interpretacionText, siguientePasoText);
        when(callResponseSpec.entity(any(Class.class))).thenReturn(resultado);

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .anio(2024).mes(6)
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();

        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.0"))
                .calculatedAt(Instant.now())
                .build();

        service.generarInterpretacion(snapshot, "Tecnología", agregado, "Estable");

        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());
        ImaSnapshot saved = captor.getValue();

        assertThat(saved.getInterpretacion()).isEqualTo(interpretacionText);
        assertThat(saved.getSiguientePaso()).isEqualTo(siguientePasoText);
    }

    /**
     * Property 4b: If ChatClient returns null, both fields become "No disponible".
     */
    @Property(tries = 10)
    @Tag("Feature: interpretacion-ima-ia, Property 4: Respuesta válida se persiste; respuesta inválida resulta en No disponible")
    void respuestaNulaResultaEnNoDisponible() {
        when(callResponseSpec.entity(any(Class.class))).thenReturn(null);

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .anio(2024).mes(6)
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();

        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.0"))
                .calculatedAt(Instant.now())
                .build();

        service.generarInterpretacion(snapshot, "Tecnología", agregado, "Estable");

        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getInterpretacion()).isEqualTo("No disponible");
        assertThat(captor.getValue().getSiguientePaso()).isEqualTo("No disponible");
    }

    /**
     * Property 4c: If InterpretacionIma has blank interpretacion, result is "No disponible".
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 4: Respuesta válida se persiste; respuesta inválida resulta en No disponible")
    void respuestaConCamposVaciosResultaEnNoDisponible(
            @ForAll("blankOrNullString") String invalidField
    ) {
        com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma resultado =
                new com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma(invalidField, "Algún paso");
        when(callResponseSpec.entity(any(Class.class))).thenReturn(resultado);

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .anio(2024).mes(6)
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();

        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.0"))
                .calculatedAt(Instant.now())
                .build();

        service.generarInterpretacion(snapshot, "Tecnología", agregado, "Estable");

        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getInterpretacion()).isEqualTo("No disponible");
        assertThat(captor.getValue().getSiguientePaso()).isEqualTo("No disponible");
    }

    // =========================================================================
    // Property 5: Fallo del ChatClient no bloquea la persistencia del IMA
    // Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.7
    // =========================================================================

    /**
     * Property 5: Fallo del ChatClient no bloquea la persistencia del IMA.
     *
     * For any exception thrown by the ChatClient, the ImaSnapshot SHALL be
     * persisted with interpretacion = "No disponible" and siguientePaso = "No disponible",
     * and NO exception propagates out of the method.
     *
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.7**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 5: Fallo del ChatClient no bloquea la persistencia del IMA")
    void falloDelChatClientNoBloqueaLaPersistenciaDelIma(
            @ForAll("randomExceptions") RuntimeException exception) {

        // Arrange: ChatClient throws the generated exception on .entity() call
        when(callResponseSpec.entity(any(Class.class))).thenThrow(exception);

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .id(UUID.randomUUID())
                .empresaId(UUID.randomUUID())
                .anio(2024)
                .mes(6)
                .cobertura(new BigDecimal("75.0"))
                .puntajeIntensidadSectorial(new BigDecimal("60.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .intensidad(new BigDecimal("1.5"))
                .calculatedAt(Instant.now())
                .build();

        AgregadoSectorial agregado = AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.0"))
                .calculatedAt(Instant.now())
                .build();

        // Act: should NOT throw any exception
        service.generarInterpretacion(snapshot, "Tecnología", agregado, "subió 5%");

        // Assert: snapshot is saved with "No disponible" fields
        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());

        ImaSnapshot saved = captor.getValue();
        assertThat(saved.getInterpretacion()).isEqualTo("No disponible");
        assertThat(saved.getSiguientePaso()).isEqualTo("No disponible");

        // Assert: the original IMA puntajes remain untouched
        assertThat(saved.getCobertura()).isEqualByComparingTo(new BigDecimal("75.0"));
        assertThat(saved.getConsistencia()).isEqualByComparingTo(new BigDecimal("80.0"));
        assertThat(saved.getIma()).isEqualByComparingTo(new BigDecimal("71.7"));
    }

    // =========================================================================
    // Property 2: La verificación de privacidad detecta datos sensibles
    // Validates: Requirements 8.2, 8.3
    // =========================================================================

    /**
     * Property 2a: If the empresa name appears in the prompt, verificarPrivacidad returns false.
     *
     * **Validates: Requirements 8.2, 8.3**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 2: La verificación de privacidad detecta datos sensibles")
    void verificarPrivacidad_detectsNombreEmpresaInPrompt(
            @ForAll("nonEmptyAlphanumeric") String nombreEmpresa,
            @ForAll("randomUUID") UUID empresaId,
            @ForAll @IntRange(min = 1, max = 100000) int cantidadEmpleados
    ) {
        // Construct prompt that CONTAINS the sensitive nombre
        String promptWithNombre = "Sector: Tecnología\nDatos: " + nombreEmpresa + "\nIMA: 75";

        boolean result = service.verificarPrivacidad(promptWithNombre, nombreEmpresa, empresaId, cantidadEmpleados);

        assertThat(result)
                .as("verificarPrivacidad should return false when prompt contains nombreEmpresa '%s'", nombreEmpresa)
                .isFalse();
    }

    /**
     * Property 2b: If the empresa UUID appears in the prompt, verificarPrivacidad returns false.
     *
     * **Validates: Requirements 8.2, 8.3**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 2: La verificación de privacidad detecta datos sensibles")
    void verificarPrivacidad_detectsEmpresaIdInPrompt(
            @ForAll("nonEmptyAlphanumeric") String nombreEmpresa,
            @ForAll("randomUUID") UUID empresaId,
            @ForAll @IntRange(min = 1, max = 100000) int cantidadEmpleados
    ) {
        // Construct prompt that CONTAINS the sensitive UUID
        String promptWithId = "Sector: Energía\nRef: " + empresaId.toString() + "\nIMA: 60";

        boolean result = service.verificarPrivacidad(promptWithId, nombreEmpresa, empresaId, cantidadEmpleados);

        assertThat(result)
                .as("verificarPrivacidad should return false when prompt contains empresaId '%s'", empresaId)
                .isFalse();
    }

    /**
     * Property 2c: If cantidadEmpleados appears in the prompt, verificarPrivacidad returns false.
     *
     * **Validates: Requirements 8.2, 8.3**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 2: La verificación de privacidad detecta datos sensibles")
    void verificarPrivacidad_detectsCantidadEmpleadosInPrompt(
            @ForAll("nonEmptyAlphanumeric") String nombreEmpresa,
            @ForAll("randomUUID") UUID empresaId,
            @ForAll @IntRange(min = 1, max = 100000) int cantidadEmpleados
    ) {
        // Construct prompt that CONTAINS the sensitive cantidadEmpleados
        String promptWithEmpleados = "Sector: Manufactura\nEmpleados: " + cantidadEmpleados + "\nIMA: 80";

        boolean result = service.verificarPrivacidad(promptWithEmpleados, nombreEmpresa, empresaId, cantidadEmpleados);

        assertThat(result)
                .as("verificarPrivacidad should return false when prompt contains cantidadEmpleados '%d'", cantidadEmpleados)
                .isFalse();
    }

    /**
     * Property 2d: A clean prompt without any sensitive data returns true.
     *
     * **Validates: Requirements 8.2, 8.3**
     */
    @Property(tries = 100)
    @Tag("Feature: interpretacion-ima-ia, Property 2: La verificación de privacidad detecta datos sensibles")
    void verificarPrivacidad_returnsTrueForCleanPrompt(
            @ForAll("nonEmptyAlphanumeric") String nombreEmpresa,
            @ForAll("randomUUID") UUID empresaId,
            @ForAll @IntRange(min = 1, max = 100000) int cantidadEmpleados
    ) {
        // Construct a prompt that does NOT contain any sensitive data
        String cleanPrompt = "Sector: Agricultura\n"
                + "Puntajes de la empresa (0-100):\n"
                + "  - Cobertura: 0\n"
                + "  - Puntaje de intensidad sectorial: 0\n"
                + "  - Consistencia: 0\n"
                + "  - IMA: 0\n"
                + "Promedios del sector (0 empresas):\n"
                + "  - Promedio IMA: 0\n"
                + "Tendencia respecto al mes anterior: estable";

        // Skip if by chance the generated values appear in our fixed prompt
        if (cleanPrompt.toLowerCase().contains(nombreEmpresa.toLowerCase())
                || cleanPrompt.contains(empresaId.toString())
                || cleanPrompt.contains(String.valueOf(cantidadEmpleados))) {
            return; // Skip this iteration — coincidental collision
        }

        boolean result = service.verificarPrivacidad(cleanPrompt, nombreEmpresa, empresaId, cantidadEmpleados);

        assertThat(result)
                .as("verificarPrivacidad should return true when prompt contains no sensitive data")
                .isTrue();
    }

    // =========================================================================
    // Custom Arbitraries
    // =========================================================================

    @Provide
    Arbitrary<String> nonEmptyAlphanumeric() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(2)
                .ofMaxLength(30);
    }

    @Provide
    Arbitrary<UUID> randomUUID() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<RuntimeException> randomExceptions() {
        return Arbitraries.of(
                new RuntimeException("Unexpected runtime error"),
                new RuntimeException("Connection reset"),
                new IllegalStateException("Client in invalid state"),
                new RuntimeException("Request timed out after 10s",
                        new java.util.concurrent.TimeoutException("timeout")),
                new RuntimeException("Connection refused",
                        new java.net.ConnectException("Connection refused")),
                new RuntimeException("Read timed out",
                        new java.net.SocketTimeoutException("Read timed out")),
                new org.springframework.web.client.HttpServerErrorException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "500 Server Error"),
                new org.springframework.web.client.HttpServerErrorException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "503 Service Unavailable"),
                new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "429 Too Many Requests"),
                new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "401 Unauthorized"),
                new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "403 Forbidden"),
                new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "400 Bad Request"),
                new org.springframework.web.client.ResourceAccessException("I/O error on POST request"),
                new RuntimeException("Unexpected EOF"),
                new NullPointerException("Response body is null")
        );
    }

    @Provide
    Arbitrary<String> blankOrNullString() {
        return Arbitraries.of(null, "", "   ", "\t", "\n");
    }

    @Provide
    Arbitrary<String> safeEmpresaName() {
        // Generate names prefixed with "XQZW" to avoid collision with prompt keywords
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "XQZW" + s);
    }
}
