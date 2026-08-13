package com.piedpiper.carbonhub.establecimiento.service;

import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;
import com.piedpiper.carbonhub.establecimiento.models.dtos.countriesdev.CountriesDevPaisDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Cliente de <a href="https://countries.dev">countries.dev</a> (PP-95) para el banner de origen
 * de establecimientos. El ticket original especifica REST Countries
 * ({@code GET https://restcountries.com/v3.1/alpha/{code}}), pero ese servicio migró a un plan
 * pago con autenticación Bearer obligatoria (confirmado contra su documentación oficial actual:
 * {@code restcountries.com/docs/countries}) — la ruta {@code /v3.1/...} sigue respondiendo
 * {@code 200} pero sin datos reales. countries.dev es un reemplazo gratuito, sin autenticación,
 * construido explícitamente como sustituto de REST Countries v3.1 (mismo caso de uso: bandera +
 * nombre por código ISO, sin cuenta ni límite de requests).
 *
 * <p>A diferencia de {@link com.piedpiper.carbonhub.emision.service.ClimatiqClient}, este cliente
 * NUNCA propaga una excepción: cualquier falla (código inválido, país no encontrado, timeout,
 * servicio caído) se traduce en {@link Optional#empty()} porque el banner de origen es un
 * elemento decorativo — su ausencia nunca debe tumbar la carga del itinerario ni mostrar un error
 * al usuario (Req PP-95).
 *
 * <p>Cachea en memoria las respuestas exitosas por {@value #TTL_HORAS} horas para no repetir la
 * misma consulta en cada carga de itinerario (Req PP-95). Es un caché simple de proceso, no
 * distribuido: si el backend corre en varias instancias cada una mantiene el suyo. Aceptable acá
 * porque el universo de claves son códigos de país (~250 como máximo) y evita sumar una
 * dependencia nueva (Caffeine/Redis) solo para este caso.
 */
@Component
public class CountriesDevClientImpl implements CountriesDevClient {

    private static final Logger log = LoggerFactory.getLogger(CountriesDevClientImpl.class);
    private static final Pattern CODIGO_ISO_VALIDO = Pattern.compile("^[A-Z]{2}$");
    private static final long TTL_HORAS = 24;

    private final RestClient restClient;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(BannerOrigenDTO banner, Instant expiraEn) {
    }

    public CountriesDevClientImpl(
            @Value("${countriesdev.url-base}") String urlBase,
            @Value("${countriesdev.tiempo-espera-ms}") int tiempoEsperaMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(tiempoEsperaMs))
                .withReadTimeout(Duration.ofMillis(tiempoEsperaMs));
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<BannerOrigenDTO> consultarPais(String codigoIso) {
        if (codigoIso == null || !CODIGO_ISO_VALIDO.matcher(codigoIso).matches()) {
            log.warn("Código ISO de país inválido para banner de origen: '{}'", codigoIso);
            return Optional.empty();
        }

        CacheEntry cacheado = cache.get(codigoIso);
        if (cacheado != null && cacheado.expiraEn().isAfter(Instant.now())) {
            return Optional.of(cacheado.banner());
        }

        Optional<BannerOrigenDTO> resultado = consultarCountriesDev(codigoIso);
        resultado.ifPresent(banner ->
                cache.put(codigoIso, new CacheEntry(banner, Instant.now().plus(Duration.ofHours(TTL_HORAS)))));
        return resultado;
    }

    private Optional<BannerOrigenDTO> consultarCountriesDev(String codigoIso) {
        try {
            // fields= restringe explícitamente a lo que se necesita: nunca se pide `translations`
            // (shape no confirmado, ver CountriesDevPaisDTO) ni el resto del payload (capital,
            // currencies, borders, etc. — no aplican al banner).
            CountriesDevPaisDTO respuesta = restClient.get()
                    .uri("/alpha/{codigoIso}?fields=name,flag,flags", codigoIso)
                    .retrieve()
                    .body(CountriesDevPaisDTO.class);

            if (respuesta == null) {
                return Optional.empty();
            }
            return Optional.of(mapearBanner(respuesta, codigoIso));
        } catch (HttpClientErrorException.NotFound e) {
            log.info("País no encontrado en countries.dev: {}", codigoIso);
            return Optional.empty();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("countries.dev no disponible al consultar '{}': {}", codigoIso, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error inesperado al consultar countries.dev para '{}'", codigoIso, e);
            return Optional.empty();
        }
    }

    private BannerOrigenDTO mapearBanner(CountriesDevPaisDTO pais, String codigoIso) {
        String banderaSvg = pais.flags() != null ? pais.flags().svg() : null;
        return new BannerOrigenDTO(pais.name(), pais.flag(), banderaSvg, codigoIso);
    }
}
