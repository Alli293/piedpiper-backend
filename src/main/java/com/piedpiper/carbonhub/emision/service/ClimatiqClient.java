package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqErrorResponse;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateRequest;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqTravelDistanceRequest;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqTravelDistanceResponse;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Cliente genérico para el endpoint de cálculo explícito de Climatiq (POST /data/v1/estimate).
 * Es intencionalmente independiente de categoría: cada servicio de emisión (electricidad, y más
 * adelante flota/vuelo/envío) arma su propio {@link ClimatiqEmissionFactorSelector} y su propio
 * mapa de {@code parameters}, y reutiliza este mismo cliente para llamar a Climatiq y traducir errores.
 */
@Component
public class ClimatiqClient {

    private static final Logger log = LoggerFactory.getLogger(ClimatiqClient.class);

    private final RestClient restClient;
    private final boolean apiKeyConfigurada;

    public ClimatiqClient(
            @Value("${climatiq.url-base}") String urlBase,
            @Value("${climatiq.clave-api}") String claveApi,
            @Value("${climatiq.tiempo-espera-ms}") int tiempoEsperaMs) {
        this.apiKeyConfigurada = claveApi != null && !claveApi.isBlank();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(tiempoEsperaMs))
                .withReadTimeout(Duration.ofMillis(tiempoEsperaMs));
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claveApi)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    public ClimatiqEstimateResponse estimar(ClimatiqEmissionFactorSelector emissionFactor,
                                            Map<String, Object> parameters) {
        ClimatiqEstimateRequest request = new ClimatiqEstimateRequest(emissionFactor, parameters);
        ClimatiqEstimateResponse response = post("/data/v1/estimate", request, ClimatiqEstimateResponse.class);
        validarRespuesta(response);
        return response;
    }

    public ClimatiqTravelDistanceResponse estimarDistanciaViaje(ClimatiqTravelDistanceRequest request) {
        return post("/travel/v1/distance", request, ClimatiqTravelDistanceResponse.class);
    }

    private <T> T post(String uri, Object request, Class<T> responseType) {
        if (!apiKeyConfigurada) {
            throw ApiException.calculoConfiguracion();
        }
        try {
            return restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.UnprocessableEntity e) {
            throw ApiException.calculoInvalido(extraerMensaje(e));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("Climatiq rechazó la solicitud (status {}): posible API key inválida o "
                    + "funcionalidad fuera del plan contratado", e.getStatusCode(), e);
            throw ApiException.calculoConfiguracion();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw ApiException.calculoSaturado();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw ApiException.calculoNoDisponible();
        }
    }

    private void validarRespuesta(ClimatiqEstimateResponse response) {
        if (response == null || response.co2e() == null || response.emissionFactor() == null
                || response.emissionFactor().id() == null) {
            throw ApiException.calculoRespuestaInvalida();
        }
    }

    private String extraerMensaje(HttpClientErrorException e) {
        try {
            ClimatiqErrorResponse body = e.getResponseBodyAs(ClimatiqErrorResponse.class);
            if (body != null && body.message() != null) {
                return body.message();
            }
        } catch (Exception parseFailure) {
            log.warn("No se pudo interpretar el cuerpo de error de Climatiq", parseFailure);
        }
        return "detalle no disponible";
    }
}
