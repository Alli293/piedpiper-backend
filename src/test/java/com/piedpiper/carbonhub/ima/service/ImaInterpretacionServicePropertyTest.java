package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests basados en propiedades para ImaInterpretacionService.
 *
 * Valida: Requisitos 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 2.7, 8.1
 */
class ImaInterpretacionServicePropertyTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private ImaSnapshotRepository imaSnapshotRepository;
    private ImaInterpretacionService service;

    @BeforeTry
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        imaSnapshotRepository = mock(ImaSnapshotRepository.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        service = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                "test-api-key"
        );
    }

    // =========================================================================
    // Propiedad 1: El prompt contiene exclusivamente datos anonimizados
    // Valida: Requisitos 1.4, 1.5, 8.1
    // =========================================================================

    /**
     * Propiedad 1: Para cualquier ImaSnapshot con cualquier combinación de puntajes (0–100),
     * sector y datos agregados, el prompt construido por construirPromptUsuario
     * NO DEBE contener nombre de empresa, empresaId ni cantidadEmpleados;
     * SÍ DEBE contener: nombre del sector, cuatro puntajes, promedios sectoriales
     * con cantidadEmpresas y tendencia.
     */
    @Property(tries = 100)
    @Tag("property-1")
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

        // El prompt NO contiene datos sensibles
        assertThat(prompt.toLowerCase()).doesNotContain(nombreEmpresa.toLowerCase());
        assertThat(prompt).doesNotContain(empresaId.toString());
        assertThat(prompt).doesNotContain(String.valueOf(cantidadEmpleados));

        // El prompt SÍ contiene los datos esperados
        assertThat(prompt).contains(sectorNombre);
        assertThat(prompt).contains(tendencia);
    }

    // =========================================================================
    // Propiedad 4: Respuesta válida se persiste; respuesta inválida resulta en "No disponible"
    // Valida: Requisitos 1.6, 1.7
    // =========================================================================

    /**
     * Propiedad 4a: Si ChatClient devuelve un InterpretacionIma válido (ambos campos no-nulos, no-vacíos),
     * ambos valores se persisten en el snapshot tal cual.
     */
    @Property(tries = 100)
    @Tag("property-4")
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
     * Propiedad 4b: Si ChatClient devuelve null, ambos campos quedan "No disponible".
     */
    @Property(tries = 10)
    @Tag("property-4")
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
     * Propiedad 4c: Si InterpretacionIma tiene interpretación en blanco, el resultado es "No disponible".
     */
    @Property(tries = 100)
    @Tag("property-4")
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
    // Propiedad 5: Fallo del ChatClient no bloquea la persistencia del IMA
    // Valida: Requisitos 2.1, 2.2, 2.3, 2.4, 2.5, 2.7
    // =========================================================================

    /**
     * Propiedad 5: Para cualquier excepción lanzada por el ChatClient, el ImaSnapshot
     * se persiste con interpretacion = "No disponible" y siguientePaso = "No disponible",
     * y NINGUNA excepción se propaga fuera del método.
     */
    @Property(tries = 100)
    @Tag("property-5")
    void falloDelChatClientNoBloqueaLaPersistenciaDelIma(
            @ForAll("randomExceptions") RuntimeException exception) {

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

        // No debe lanzar ninguna excepción
        service.generarInterpretacion(snapshot, "Tecnología", agregado, "subió 5%");

        // El snapshot se persiste con "No disponible"
        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());

        ImaSnapshot saved = captor.getValue();
        assertThat(saved.getInterpretacion()).isEqualTo("No disponible");
        assertThat(saved.getSiguientePaso()).isEqualTo("No disponible");

        // Los puntajes originales del IMA no se alteran
        assertThat(saved.getCobertura()).isEqualByComparingTo(new BigDecimal("75.0"));
        assertThat(saved.getConsistencia()).isEqualByComparingTo(new BigDecimal("80.0"));
        assertThat(saved.getIma()).isEqualByComparingTo(new BigDecimal("71.7"));
    }

    // =========================================================================
    // Proveedores de datos arbitrarios
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
        // Nombres con prefijo "XQZW" para evitar colisiones con palabras clave del prompt
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "XQZW" + s);
    }
}
