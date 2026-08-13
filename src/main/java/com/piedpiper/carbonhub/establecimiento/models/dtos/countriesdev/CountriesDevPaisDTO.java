package com.piedpiper.carbonhub.establecimiento.models.dtos.countriesdev;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Subconjunto de campos de {@code GET https://countries.dev/alpha/{code}} que usa el banner de
 * origen (PP-95). {@code @JsonIgnoreProperties(ignoreUnknown = true)} porque la API expone más
 * campos de los que se necesitan acá.
 *
 * <p>Deliberadamente NO se pide/mapea {@code translations}: la documentación pública de
 * countries.dev no confirma su shape exacto (a diferencia de {@code name}, {@code flag} y
 * {@code flags}, verificados contra un ejemplo real de la doc), y pedir un campo cuyo tipo no
 * está confirmado es exactamente el error que ya causó una falla de deserialización con el
 * proveedor anterior (ver historial de PP-95: REST Countries devolvía un objeto donde se
 * esperaba un array). Los 8 países que puede tener registrados una {@code Empresa} en este
 * sistema (CR, GT, HN, SV, NI, PA, MX, CO) tienen nombre en inglés prácticamente idéntico al
 * español (acentos aparte), así que {@code name} solo alcanza sin necesidad de traducción. Si el
 * catálogo de países soportados crece más allá de Latinoamérica, esto debe revisarse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CountriesDevPaisDTO(
        String name,
        String flag,
        Banderas flags) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Banderas(String svg, String png) {
    }
}
