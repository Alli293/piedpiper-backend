package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class HttpCertificacionEventosClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> metodoRecibido = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> contentTypeRecibido = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void urlNoConfiguradaLanzaCertificacionNoDisponible() {
        HttpCertificacionEventosClient client = new HttpCertificacionEventosClient(
                "", "/api/certificacion/eventos", 2000);

        CertificacionNoDisponibleException exception = catchThrowableOfType(
                () -> client.enviar(requestValido()),
                CertificacionNoDisponibleException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(
                "El modulo de Certificacion no tiene una URL configurada.");
    }

    @Test
    void envioCorrectoPosteaEventoAlPathConfigurado() throws IOException {
        HttpCertificacionEventosClient client = clientApuntandoA(200);

        client.enviar(requestValido());

        assertThat(metodoRecibido.get()).isEqualTo("POST");
        assertThat(rutaRecibida.get()).isEqualTo("/api/certificacion/eventos");
        assertThat(contentTypeRecibido.get()).contains("application/json");
        assertThat(cuerpoRecibido.get()).contains("primer_itinerario_generado");
        assertThat(cuerpoRecibido.get()).contains("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    }

    @Test
    void errorHttpLanzaCertificacionNoDisponible() throws IOException {
        HttpCertificacionEventosClient client = clientApuntandoA(503);

        CertificacionNoDisponibleException exception = catchThrowableOfType(
                () -> client.enviar(requestValido()),
                CertificacionNoDisponibleException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(
                "No se pudo comunicar con el modulo de Certificacion.");
    }

    @Test
    void conexionNoDisponibleLanzaCertificacionNoDisponible() throws IOException {
        int puerto = puertoLocalLibre();
        HttpCertificacionEventosClient client = new HttpCertificacionEventosClient(
                "http://localhost:" + puerto,
                "/api/certificacion/eventos",
                200);

        CertificacionNoDisponibleException exception = catchThrowableOfType(
                () -> client.enviar(requestValido()),
                CertificacionNoDisponibleException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(
                "No se pudo comunicar con el modulo de Certificacion.");
    }

    private HttpCertificacionEventosClient clientApuntandoA(int statusCode) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/certificacion/eventos", exchange -> {
            metodoRecibido.set(exchange.getRequestMethod());
            rutaRecibida.set(exchange.getRequestURI().getPath());
            contentTypeRecibido.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            cuerpoRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidor.start();
        int puerto = servidor.getAddress().getPort();
        return new HttpCertificacionEventosClient(
                "http://localhost:" + puerto,
                "/api/certificacion/eventos",
                2000);
    }

    private static int puertoLocalLibre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static EventoCertificacionRequestDTO requestValido() {
        return new EventoCertificacionRequestDTO(
                UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73"),
                "primer_itinerario_generado",
                Instant.parse("2026-07-24T18:00:00Z"));
    }
}
