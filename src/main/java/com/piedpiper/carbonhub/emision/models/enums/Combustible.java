package com.piedpiper.carbonhub.emision.models.enums;

public enum Combustible {

    PROMEDIO("Promedio"),
    GASOLINA("Gasolina"),
    DIESEL("Diésel"),
    PHEV("Híbrido enchufable (PHEV)"),
    BEV("Eléctrico (BEV)");

    private final String nombre;

    Combustible(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
