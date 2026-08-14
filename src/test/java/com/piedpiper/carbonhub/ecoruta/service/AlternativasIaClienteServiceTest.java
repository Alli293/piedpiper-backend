package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.service.AlternativasIaClienteService.RespuestaAlternativasIA;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlternativasIaClienteServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AlternativasIaClienteService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new AlternativasIaClienteService(chatClientBuilder, "test-gemini-api-key");
    }

    private void configurarChatClientMockChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private ItinerarioActividad actividadDePrueba() {
        return ItinerarioActividad.builder()
                .nombre("Canopy Tour")
                .categoriaTuristica(InteresTuristico.AVENTURA)
                .provincia(Provincia.PUNTARENAS)
                .build();
    }

    private RespuestaAlternativasIA respuestaValida() {
        AlternativaIaDTO alt1 = new AlternativaIaDTO(
                "Kayak en manglar", "Recorrido eco-guiado por manglares",
                new BigDecimal("15000"), "CRC", "EcoTours CR", 85);
        AlternativaIaDTO alt2 = new AlternativaIaDTO(
                "Senderismo volcánico", "Caminata guiada por senderos certificados",
                new BigDecimal("12000"), "CRC", "Green Trails", 78);
        return new RespuestaAlternativasIA(List.of(alt1, alt2));
    }

    // --- Tests de parsing exitoso de respuesta válida de Gemini ---

    @Test
    void respuestaValidaAlPrimerIntentoRetornaAlternativas() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class))).thenReturn(respuestaValida());

        List<AlternativaIaDTO> resultado = service.buscarAlternativas(
                actividadDePrueba(), List.of("Canopy Tour"));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Kayak en manglar");
        assertThat(resultado.get(0).getPuntuacionAmbientalEstimada()).isEqualTo(85);
        assertThat(resultado.get(1).getNombre()).isEqualTo("Senderismo volcánico");
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void respuestaValidaConservaMonedaYEstablecimiento() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class))).thenReturn(respuestaValida());

        List<AlternativaIaDTO> resultado = service.buscarAlternativas(
                actividadDePrueba(), List.of());

        assertThat(resultado.get(0).getMoneda()).isEqualTo("CRC");
        assertThat(resultado.get(0).getEstablecimientoRecomendado()).isEqualTo("EcoTours CR");
        assertThat(resultado.get(0).getCostoAproximado()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    // --- Tests de manejo de reintentos cuando Gemini retorna respuesta inválida ---

    @Test
    void respuestaInvalidaAlPrimerIntentoReintentaYRetornaEnSegundoIntento() {
        configurarChatClientMockChain();
        RespuestaAlternativasIA invalida = new RespuestaAlternativasIA(List.of());
        when(callResponseSpec.entity(any(Class.class)))
                .thenReturn(invalida)
                .thenReturn(respuestaValida());

        List<AlternativaIaDTO> resultado = service.buscarAlternativas(
                actividadDePrueba(), List.of());

        assertThat(resultado).hasSize(2);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void respuestaNullReintentaYRetornaEnSegundoIntento() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenReturn(null)
                .thenReturn(respuestaValida());

        List<AlternativaIaDTO> resultado = service.buscarAlternativas(
                actividadDePrueba(), List.of());

        assertThat(resultado).hasSize(2);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void respuestaConAlternativasNullReintenta() {
        configurarChatClientMockChain();
        RespuestaAlternativasIA conNull = new RespuestaAlternativasIA(null);
        when(callResponseSpec.entity(any(Class.class)))
                .thenReturn(conNull)
                .thenReturn(respuestaValida());

        List<AlternativaIaDTO> resultado = service.buscarAlternativas(
                actividadDePrueba(), List.of());

        assertThat(resultado).hasSize(2);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void dosRespuestasInvalidasAgotaReintentosYLanzaRespuestaInvalida() {
        configurarChatClientMockChain();
        RespuestaAlternativasIA invalida = new RespuestaAlternativasIA(List.of());
        when(callResponseSpec.entity(any(Class.class))).thenReturn(invalida);

        assertThatThrownBy(() -> service.buscarAlternativas(actividadDePrueba(), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
        verify(chatClient, times(2)).prompt();
    }

    // --- Tests de manejo de timeout y error de red ---

    @Test
    void timeoutFallaDeInmediatoSinReintentar() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new RuntimeException("Connection timed out"));

        assertThatThrownBy(() -> service.buscarAlternativas(actividadDePrueba(), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void errorDeRedFallaDeInmediatoSinReintentar() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(any(Class.class)))
                .thenThrow(new RuntimeException("Network unreachable"));

        assertThatThrownBy(() -> service.buscarAlternativas(actividadDePrueba(), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void apiKeyAusenteNoIntentaLaLlamada() {
        AlternativasIaClienteService serviceSinKey = new AlternativasIaClienteService(
                chatClientBuilder, "");

        assertThatThrownBy(() -> serviceSinKey.buscarAlternativas(actividadDePrueba(), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, never()).prompt();
    }

    @Test
    void apiKeyNullNoIntentaLaLlamada() {
        AlternativasIaClienteService serviceSinKey = new AlternativasIaClienteService(
                chatClientBuilder, null);

        assertThatThrownBy(() -> serviceSinKey.buscarAlternativas(actividadDePrueba(), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verify(chatClient, never()).prompt();
    }
}
