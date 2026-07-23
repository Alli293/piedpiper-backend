package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ImaInterpretacionService.
 *
 * Validates: Requirements 1.1, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 4.5, 8.2, 8.3
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
    @Mock
    private EmpresaRepository empresaRepository;

    private ImaInterpretacionService service;

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID SNAPSHOT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        service = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                empresaRepository,
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
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
        configurarEmpresaSegura();
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        verificarNoDisponiblePersistido();
    }

    // --- Test 10: API key ausente no intenta la llamada ---

    @Test
    void apiKeyAusenteNoIntenta() {
        // Create service with empty API key
        ImaInterpretacionService serviceNoKey = new ImaInterpretacionService(
                chatClientBuilder,
                imaSnapshotRepository,
                empresaRepository,
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

    // --- Test 11: Privacidad fallida aborta la llamada ---

    @Test
    void privacidadFallidaAbortaLlamada() {
        // Configure empresa whose name WILL appear in the prompt
        Empresa empresa = Empresa.builder()
                .id(EMPRESA_ID)
                .nombreEmpresa("Tecnología")  // Same as sector name passed in prompt
                .cantidadEmpleados(100)
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .build();
        when(empresaRepository.findById(any(UUID.class))).thenReturn(Optional.of(empresa));
        when(imaSnapshotRepository.save(any(ImaSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        ImaSnapshot snapshot = crearSnapshot();

        service.generarInterpretacion(snapshot, "Tecnología", crearAgregado(), "Estable");

        // ChatClient prompt() should never be invoked (privacy check aborts before call)
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

    private void configurarEmpresaSegura() {
        Empresa empresa = Empresa.builder()
                .id(EMPRESA_ID)
                .nombreEmpresa("SafeTestCorp")
                .cantidadEmpleados(99999)
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .build();
        when(empresaRepository.findById(any(UUID.class))).thenReturn(Optional.of(empresa));
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
