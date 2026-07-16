package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.enums.CabinClass;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EmisionVueloLocalCalculator {

    private static final BigDecimal ECONOMY_KG_CO2E_PER_PASSENGER_KM = new BigDecimal("0.115");
    private static final BigDecimal PREMIUM_MULTIPLIER = new BigDecimal("1.60");
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final BigDecimal ROUTE_FACTOR = new BigDecimal("1.09");

    public Resultado calcular(String origenIata, String destinoIata, CabinClass cabinClass) {
        AeropuertoCatalogo.Aeropuerto origen = AeropuertoCatalogo.obtener(origenIata);
        AeropuertoCatalogo.Aeropuerto destino = AeropuertoCatalogo.obtener(destinoIata);

        BigDecimal distanciaKm = BigDecimal.valueOf(distanciaGranCirculoKm(origen, destino))
                .multiply(ROUTE_FACTOR)
                .setScale(3, RoundingMode.HALF_UP);
        BigDecimal factor = ECONOMY_KG_CO2E_PER_PASSENGER_KM;
        if (cabinClass == CabinClass.PREMIUM) {
            factor = factor.multiply(PREMIUM_MULTIPLIER);
        }
        BigDecimal carbonKg = distanciaKm.multiply(factor).setScale(3, RoundingMode.HALF_UP);
        return new Resultado(carbonKg, distanciaKm);
    }

    private double distanciaGranCirculoKm(
            AeropuertoCatalogo.Aeropuerto origen,
            AeropuertoCatalogo.Aeropuerto destino) {
        double lat1 = Math.toRadians(origen.latitud());
        double lat2 = Math.toRadians(destino.latitud());
        double deltaLat = Math.toRadians(destino.latitud() - origen.latitud());
        double deltaLon = Math.toRadians(destino.longitud() - origen.longitud());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public record Resultado(BigDecimal carbonKg, BigDecimal distanceKm) {
    }
}
