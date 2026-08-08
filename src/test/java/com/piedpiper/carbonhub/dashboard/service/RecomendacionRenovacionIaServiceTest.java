package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionIaTexto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecomendacionRenovacionIaServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private RecomendacionRenovacionIaService service;

    private static final CertAlertaDTO PRIORITARIA = new CertAlertaDTO(
            UUID.randomUUID(), "GHG Protocol — Corporate Standard",
            LocalDate.now().plusDays(5), 5, new BigDecimal("120.5000"));

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new RecomendacionRenovacionIaService(chatClientBuilder, "test-gemini-api-key");
    }

    @Test
    void generaLaRecomendacionConLosDatosCorrectos() {
        configurarChatClientMockChain();
        RecomendacionIaTexto resultado = new RecomendacionIaTexto(
                "Vence en 5 días y tiene un impacto de 120.5 t CO2e.",
                "Renovarla esta semana.");
        when(callResponseSpec.entity(RecomendacionIaTexto.class)).thenReturn(resultado);

        Optional<RecomendacionIaTexto> generado = service.generar(PRIORITARIA);

        assertThat(generado).contains(resultado);
    }

    @Test
    void anteExcepcionDevuelveVacioSinPropagarla() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(RecomendacionIaTexto.class))
                .thenThrow(new RuntimeException("timeout", new TimeoutException("timeout")));

        Optional<RecomendacionIaTexto> generado = service.generar(PRIORITARIA);

        assertThat(generado).isEmpty();
    }

    @Test
    void respuestaNulaDevuelveVacio() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(RecomendacionIaTexto.class)).thenReturn(null);

        assertThat(service.generar(PRIORITARIA)).isEmpty();
    }

    @Test
    void respuestaConCamposVaciosDevuelveVacio() {
        configurarChatClientMockChain();
        when(callResponseSpec.entity(RecomendacionIaTexto.class))
                .thenReturn(new RecomendacionIaTexto("", "paso válido"));

        assertThat(service.generar(PRIORITARIA)).isEmpty();
    }

    @Test
    void apiKeyAusenteNoIntentaLaLlamada() {
        RecomendacionRenovacionIaService sinKey =
                new RecomendacionRenovacionIaService(chatClientBuilder, "");

        Optional<RecomendacionIaTexto> generado = sinKey.generar(PRIORITARIA);

        assertThat(generado).isEmpty();
        verify(chatClient, never()).prompt();
    }

    private void configurarChatClientMockChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }
}
