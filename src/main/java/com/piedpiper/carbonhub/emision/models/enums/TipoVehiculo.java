package com.piedpiper.carbonhub.emision.models.enums;

/**
 * Catálogo fijo de tipos de vehículo de flota. Los combustibles válidos para cada uno,
 * y el activity_id de Climatiq resultante de cada combinación, viven en
 * {@link com.piedpiper.carbonhub.emision.service.CatalogoVehiculoFlota}: no todas las
 * combinaciones de tipo y combustible existen en los datos de Climatiq.
 */
public enum TipoVehiculo {

    AUTOMOVIL("Automóvil / SUV"),
    MOTOCICLETA("Motocicleta"),
    FURGONETA_COMERCIAL("Furgoneta comercial (liviana, <3.5t)"),
    CAMION_PESADO("Camión pesado (>3.5t)");

    private final String nombre;

    TipoVehiculo(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
