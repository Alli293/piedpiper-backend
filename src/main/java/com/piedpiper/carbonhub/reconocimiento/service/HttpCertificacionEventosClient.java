package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class HttpCertificacionEventosClient implements CertificacionEventosClient {

    private static final Logger log = LoggerFactory.getLogger(HttpCertificacionEventosClient.class);

    private final RestClient restClient;
    private final boolean urlConfigurada;
    private final String eventosPath;

    public HttpCertificacionEventosClient(
            @Value("${certificacion.url-base:}") String urlBase,
            @Value("${certificacion.eventos-path:/api/certificacion/eventos}") String eventosPath,
            @Value("${certificacion.tiempo-espera-ms:10000}") int tiempoEsperaMs) {
        this.urlConfigurada = urlBase != null && !urlBase.isBlank();
        this.eventosPath = eventosPath;

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(tiempoEsperaMs))
                .withReadTimeout(Duration.ofMillis(tiempoEsperaMs));
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory);
        if (urlConfigurada) {
            builder.baseUrl(urlBase.trim());
        }
        this.restClient = builder.build();
    }

    @Override
    public void enviar(EventoCertificacionRequestDTO request) {
        if (!urlConfigurada) {
            throw new CertificacionNoDisponibleException(
                    "El modulo de Certificacion no tiene una URL configurada.");
        }
        try {
            restClient.post()
                    .uri(eventosPath)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException | ResourceAccessException e) {
            log.warn("No se pudo enviar el evento {} del usuario {} a Certificacion",
                    request.getEventoGenerado(), request.getUsuarioId(), e);
            throw new CertificacionNoDisponibleException(
                    "No se pudo comunicar con el modulo de Certificacion.", e);
        }
    }
}
