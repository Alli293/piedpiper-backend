package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoGeneracionIA;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItinerarioIaClienteServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private final ItinerarioValidador validador = new ItinerarioValidador();

    private ItinerarioIaClienteService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new ItinerarioIaClienteService(chatClientBuilder, validador, "test-gemini-api-key");
    }

    private void configurarChatClientMockChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private ItinerarioIaResponseDTO respuestaValida() {
        ActividadIaDTO actividad = new ActividadIaDTO(
                "Caminata", "Recorrido guiado", "09:00", 120,
                new BigDecimal("10000"), "CRC", "Reserva Selvatura", "PUNTARENAS", 75);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividad));
        return new ItinerarioIaResponseDTO(List.of(dia), 82);
    }

    @Test
    void respuestaValidaAlPrimerIntentoNoReintenta() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class))).thenReturn(respuestaValida());

        ResultadoGeneracionIA resultado = service.generar("contexto", 1);

        assertThat(resultado.resultado()).isEqualTo(ResultadoValidacionItinerario.VALIDO_COMPLETO);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void respuestaInvalidaDosVecesYValidaALaTerceraReintenta() {
        configurarChatClientMockChain();
        ItinerarioIaResponseDTO invalida = new ItinerarioIaResponseDTO(List.of(), 0);
        when(callResponseSpec.entity(any(Class.class)))
                .thenReturn(invalida)
                .thenReturn(invalida)
                .thenReturn(respuestaValida());

        ResultadoGeneracionIA resultado = service.generar("contexto", 1);

        assertThat(resultado.resultado()).isEqualTo(ResultadoValidacionItinerario.VALIDO_COMPLETO);
        verify(chatClient, times(3)).prompt();
    }

    @Test
    void tresRespuestasInvalidasSeguidasLanzaRespuestaInvalida() {
        configurarChatClientMockChain();
        ItinerarioIaResponseDTO invalida = new ItinerarioIaResponseDTO(List.of(), 0);
        when(callResponseSpec.entity(any(Class.class))).thenReturn(invalida);

        assertThatThrownBy(() -> service.generar("contexto", 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(chatClient, times(3)).prompt();
    }

    @Test
    void timeoutFallaDeInmediatoSinReintentar() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new RuntimeException("Request timed out", new TimeoutException("timeout")));

        assertThatThrownBy(() -> service.generar("contexto", 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void apiKeyAusenteNoIntentaLaLlamada() {
        ItinerarioIaClienteService servicioSinKey = new ItinerarioIaClienteService(
                chatClientBuilder, validador, "");

        assertThatThrownBy(() -> servicioSinKey.generar("contexto", 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, never()).prompt();
    }
}
