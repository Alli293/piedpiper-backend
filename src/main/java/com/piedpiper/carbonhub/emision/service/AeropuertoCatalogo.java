package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

import java.util.Map;

final class AeropuertoCatalogo {

    private static final Map<String, Aeropuerto> AEROPUERTOS = Map.ofEntries(
            Map.entry("SJO", new Aeropuerto(9.9939, -84.2088)),
            Map.entry("LIR", new Aeropuerto(10.5933, -85.5444)),
            Map.entry("PTY", new Aeropuerto(9.0714, -79.3835)),
            Map.entry("MGA", new Aeropuerto(12.1415, -86.1682)),
            Map.entry("SAL", new Aeropuerto(13.4409, -89.0557)),
            Map.entry("GUA", new Aeropuerto(14.5833, -90.5275)),
            Map.entry("BOG", new Aeropuerto(4.7016, -74.1469)),
            Map.entry("MEX", new Aeropuerto(19.4361, -99.0719)),
            Map.entry("CUN", new Aeropuerto(21.0365, -86.8771)),
            Map.entry("MIA", new Aeropuerto(25.7959, -80.2870)),
            Map.entry("FLL", new Aeropuerto(26.0726, -80.1527)),
            Map.entry("MCO", new Aeropuerto(28.4312, -81.3081)),
            Map.entry("ATL", new Aeropuerto(33.6407, -84.4277)),
            Map.entry("DFW", new Aeropuerto(32.8998, -97.0403)),
            Map.entry("IAH", new Aeropuerto(29.9902, -95.3368)),
            Map.entry("JFK", new Aeropuerto(40.6413, -73.7781)),
            Map.entry("EWR", new Aeropuerto(40.6895, -74.1745)),
            Map.entry("BOS", new Aeropuerto(42.3656, -71.0096)),
            Map.entry("IAD", new Aeropuerto(38.9531, -77.4565)),
            Map.entry("ORD", new Aeropuerto(41.9742, -87.9073)),
            Map.entry("DEN", new Aeropuerto(39.8561, -104.6737)),
            Map.entry("LAX", new Aeropuerto(33.9416, -118.4085)),
            Map.entry("SFO", new Aeropuerto(37.6213, -122.3790)),
            Map.entry("SEA", new Aeropuerto(47.4502, -122.3088)),
            Map.entry("YYZ", new Aeropuerto(43.6777, -79.6248)),
            Map.entry("YUL", new Aeropuerto(45.4706, -73.7408)),
            Map.entry("LHR", new Aeropuerto(51.4700, -0.4543)),
            Map.entry("MAD", new Aeropuerto(40.4983, -3.5676)),
            Map.entry("BCN", new Aeropuerto(41.2974, 2.0833)),
            Map.entry("CDG", new Aeropuerto(49.0097, 2.5479)),
            Map.entry("AMS", new Aeropuerto(52.3105, 4.7683)),
            Map.entry("FRA", new Aeropuerto(50.0379, 8.5622))
    );

    private AeropuertoCatalogo() {
    }

    static Aeropuerto obtener(String iata) {
        Aeropuerto aeropuerto = AEROPUERTOS.get(iata);
        if (aeropuerto == null) {
            throw ApiException.calculoVueloInvalido(
                    "No tenemos coordenadas locales para el aeropuerto " + iata
                            + ". El catálogo local cubre aeropuertos frecuentes del alcance actual; "
                            + "contacte al administrador si necesita registrar otro código IATA.");
        }
        return aeropuerto;
    }

    record Aeropuerto(double latitud, double longitud) {
    }
}
