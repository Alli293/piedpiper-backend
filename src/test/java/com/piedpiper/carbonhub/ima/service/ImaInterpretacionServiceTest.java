package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ImaInterpretacionService.
 *
 * Valida: Requisitos 1.1, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 4.5
 */
@ExtendWith(MockitoExtension.class)
class ImaInterpretacionServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;

    private ImaInterpretacionService service;

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID SNAPSHOT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        service = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                "test-gemini-api-key"
        );
    }

    // --- Test 1: Respuesta válida se persiste correctamente ---

    @Test
    void respuestaValidaSePersisteCorrectamente() {
        configurarChatClientMockChain();
        InterpretacionIma resultado = new InterpretacionIma(
                "Su IMA de 75 está por encima del promedio sectorial.",
                "Implementar medición de Scope 3."
        );
        when(callResponseSpec.entity(any(Class.class))).thenReturn(resultado);
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Subió 5%");

        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());
        ImaSnapshot saved = captor.getValue();

        assertThat(saved.getInterpretacion()).isEqualTo("Su IMA de 75 está por encima del promedio sectorial.");
        assertThat(saved.getSiguientePaso()).isEqualTo("Implementar medición de Scope 3.");
    }

    // --- Test 2: Timeout no bloquea ---

    @Test
    void timeoutNoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new RuntimeException("Request timed out", new TimeoutException("timeout")));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 3: Rate limit 429 no bloquea ---

    @Test
    void rateLimit429NoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "429 Too Many Requests"));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 4: Unauthorized 401 no bloquea ---

    @Test
    void unauthorized401NoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "401 Unauthorized"));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 5: Forbidden 403 no bloquea ---

    @Test
    void forbidden403NoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "403 Forbidden"));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 6: Server error 5xx no bloquea ---

    @Test
    void serverError5xxNoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "500 Server Error"));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Bajó 2%");

        verificarNoDisponiblePersistido();
    }

    // --- Test 7: Error de red no bloquea ---

    @Test
    void errorDeRedNoBloquea() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new ResourceAccessException("I/O error on POST request"));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 8: Respuesta con campos vacíos ---

    @Test
    void respuestaConCamposVacios() {
        configurarChatClientMockChain();
        InterpretacionIma resultado = new InterpretacionIma("", "paso válido");
        when(callResponseSpec.entity(any(Class.class))).thenReturn(resultado);
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 9: Respuesta nula ---

    @Test
    void respuestaNula() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class))).thenReturn(null);
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 10: API key ausente no intenta la llamada ---

    @Test
    void apiKeyAusenteNoIntenta() {
        ImaInterpretacionService serviceNoKey = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                ""
        );
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        serviceNoKey.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        // ChatClient should never be called
        verify(chatClient, never()).prompt();

        // "No disponible" should be persisted
        verificarNoDisponiblePersistido();
    }

    // --- Helpers ---

    private void configurarChatClientMockChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private ImaSnapshot crearSnapshot() {
        return ImaSnapshot.builder()
                .id(SNAPSHOT_ID)
                .empresaId(EMPRESA_ID)
                .anio(2024)
                .mes(6)
                .cobertura(new BigDecimal("75.0"))
                .puntajeIntensidadSectorial(new BigDecimal("60.0"))
                .consistencia(new BigDecimal("80.0"))
                .ima(new BigDecimal("71.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();
    }

    private AgregadoSectorial crearAgregado() {
        return AgregadoSectorial.builder()
                .cantidadEmpresas(10)
                .intensidadPromedio(new BigDecimal("2.0"))
                .calculatedAt(Instant.now())
                .build();
    }

    private void verificarNoDisponiblePersistido() {
        ArgumentCaptor<ImaSnapshot> captor = ArgumentCaptor.forClass(ImaSnapshot.class);
        verify(imaSnapshotRepository).save(captor.capture());
        ImaSnapshot saved = captor.getValue();

        assertThat(saved.getInterpretacion()).isEqualTo("No disponible");
        assertThat(saved.getSiguientePaso()).isEqualTo("No disponible");
    }
}
