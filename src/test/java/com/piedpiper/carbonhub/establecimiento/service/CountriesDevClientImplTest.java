package com.piedpiper.carbonhub.establecimiento.service;

import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usa un {@link HttpServer} (JDK, sin dependencias extra) para simular countries.dev, siguiendo
 * el mismo patrón que {@code ClimatiqClientTest}. A diferencia de ese cliente, acá se verifica
 * que NINGÚN escenario de falla propague una excepción (Req PP-95).
 *
 * <p>El cuerpo de respuesta usado acá está verificado contra un ejemplo real publicado en
 * {@code countries.dev/docs/api/alpha} (objeto plano, {@code flag} como emoji, {@code flags.svg}
 * como URL) — no es una suposición, a diferencia del incidente anterior con REST Countries donde
 * un cuerpo de test asumido incorrectamente ocultó una falla de deserialización real.
 */
class CountriesDevClientImplTest {

    private static final String CUERPO_COSTA_RICA = "{"
            + "\"name\":\"Costa Rica\","
            + "\"flag\":\"\\uD83C\\uDDE8\\uD83C\\uDDF7\","
            + "\"flags\":{\"svg\":\"https://countries.dev/flags/cr.svg\",\"png\":\"https://countries.dev/flags/cr.png\"}"
            + "}";

    private HttpServer servidor;
    private final AtomicInteger llamadasRecibidas = new AtomicInteger(0);

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private CountriesDevClientImpl clienteApuntandoA(int statusCode, String cuerpoRespuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/alpha/", exchange -> {
            llamadasRecibidas.incrementAndGet();
            byte[] bytes = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidor.start();
        int puerto = servidor.getAddress().getPort();
        return new CountriesDevClientImpl("http://localhost:" + puerto, 2000);
    }

    // --- Flujo exitoso ---

    @Test
    void consultarPaisRetornaElBannerConLosCamposCorrectos() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(200, CUERPO_COSTA_RICA);

        Optional<BannerOrigenDTO> resultado = cliente.consultarPais("CR");

        assertThat(resultado).isPresent();
        BannerOrigenDTO banner = resultado.get();
        assertThat(banner.getNombrePais()).isEqualTo("Costa Rica");
        assertThat(banner.getBanderaEmoji()).isEqualTo("\uD83C\uDDE8\uD83C\uDDF7");
        assertThat(banner.getBanderaUrlSvg()).isEqualTo("https://countries.dev/flags/cr.svg");
        assertThat(banner.getCodigoIso()).isEqualTo("CR");
    }

    // --- País no encontrado / servicio no disponible ---

    @Test
    void consultarPaisRetornaVacioSinLanzarCuandoCountriesDevDevuelve404() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(404, "\"Country not found\"");

        Optional<BannerOrigenDTO> resultado = cliente.consultarPais("ZZ");

        assertThat(resultado).isEmpty();
    }

    @Test
    void consultarPaisRetornaVacioSinLanzarCuandoCountriesDevDevuelve500() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(500, "{\"message\":\"Internal Server Error\"}");

        Optional<BannerOrigenDTO> resultado = cliente.consultarPais("CR");

        assertThat(resultado).isEmpty();
    }

    @Test
    void consultarPaisRetornaVacioSinLanzarCuandoElServicioNoResponde() {
        // Ningún servidor arrancado en este puerto: simula timeout/servicio caído.
        CountriesDevClientImpl cliente = new CountriesDevClientImpl("http://localhost:1", 500);

        Optional<BannerOrigenDTO> resultado = cliente.consultarPais("CR");

        assertThat(resultado).isEmpty();
    }

    // --- Validación de formato ISO ---

    @Test
    void consultarPaisRetornaVacioYNoLlamaAlServidorCuandoElCodigoNoEsIsoValido() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(200, CUERPO_COSTA_RICA);

        assertThat(cliente.consultarPais("Costa Rica")).isEmpty();
        assertThat(cliente.consultarPais("cr")).isEmpty();
        assertThat(cliente.consultarPais("CRI")).isEmpty();
        assertThat(cliente.consultarPais(null)).isEmpty();

        assertThat(llamadasRecibidas.get()).isZero();
    }

    // --- Caché con TTL ---

    @Test
    void consultarPaisNoRepiteLaLlamadaAlServidorEnUnaSegundaConsultaDelMismoCodigo() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(200, CUERPO_COSTA_RICA);

        cliente.consultarPais("CR");
        cliente.consultarPais("CR");
        Optional<BannerOrigenDTO> segundaRespuesta = cliente.consultarPais("CR");

        assertThat(llamadasRecibidas.get()).isEqualTo(1);
        assertThat(segundaRespuesta).isPresent();
        assertThat(segundaRespuesta.get().getNombrePais()).isEqualTo("Costa Rica");
    }

    @Test
    void consultarPaisConsultaPorSeparadoCodigosDistintos() throws IOException {
        CountriesDevClientImpl cliente = clienteApuntandoA(200, CUERPO_COSTA_RICA);

        cliente.consultarPais("CR");
        cliente.consultarPais("PA");

        assertThat(llamadasRecibidas.get()).isEqualTo(2);
    }
}
