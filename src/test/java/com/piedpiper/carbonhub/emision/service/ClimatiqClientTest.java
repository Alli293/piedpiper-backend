package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Usa un {@link HttpServer} (JDK, sin dependencias extra) para simular Climatiq y verificar que
 * la clave de API se envía correctamente en la petición, pero nunca se filtra en los errores que
 * ClimatiqClient traduce hacia el resto de la aplicación.
 */
class ClimatiqClientTest {

    private static final String API_KEY = "sk-climatiq-secreta-no-debe-filtrarse";

    private HttpServer servidor;
    private final AtomicReference<String> authorizationRecibido = new AtomicReference<>();

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private ClimatiqClient clienteApuntandoA(int statusCode, String cuerpoRespuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/data/v1/estimate", exchange -> {
            authorizationRecibido.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidor.start();
        int puerto = servidor.getAddress().getPort();
        return new ClimatiqClient("http://localhost:" + puerto, API_KEY, 2000);
    }

    private ClimatiqEmissionFactorSelector selectorElectricidadCR() {
        return new ClimatiqEmissionFactorSelector(
                "electricity-supply_grid-source_supplier_mix-use_na", "^6", "CR");
    }

    @Test
    void enviaLaApiKeyAlServidorComoBearerToken() throws IOException {
        String cuerpo = "{\"co2e\": 27.85, \"co2e_unit\": \"kg\", \"emission_factor\": "
                + "{\"id\": \"factor-1\", \"activity_id\": \"electricity-supply_grid-source_supplier_mix-use_na\", "
                + "\"region\": \"CR\", \"year\": 2024}}";
        ClimatiqClient cliente = clienteApuntandoA(200, cuerpo);

        ClimatiqEstimateResponse respuesta = cliente.estimar(
                selectorElectricidadCR(),
                Map.of("energy", new BigDecimal("500"), "energy_unit", "kWh"));

        assertThat(respuesta.co2e()).isEqualByComparingTo("27.85");
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer " + API_KEY);
    }

    @Test
    void apiKeyInvalidaNoExponeLaClaveEnElMensajeDeError() throws IOException {
        String cuerpoError = "{\"error\":\"unauthorized\",\"error_code\":\"401\",\"message\":\"Invalid API key\"}";
        ClimatiqClient cliente = clienteApuntandoA(401, cuerpoError);

        ClimatiqEmissionFactorSelector selector = selectorElectricidadCR();
        Map<String, Object> parametros = Map.of("energy", new BigDecimal("500"), "energy_unit", "kWh");
        ApiException excepcion = catchThrowableOfType(() -> cliente.estimar(selector, parametros), ApiException.class);

        assertThat(excepcion).isNotNull();
        assertThat(excepcion.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(excepcion.getMessage()).doesNotContain(API_KEY);
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer " + API_KEY);
    }
}
