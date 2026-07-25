package com.piedpiper.carbonhub.emision.service;

final class ReporteHuellaPdfNombreArchivo {

    private ReporteHuellaPdfNombreArchivo() {
    }

    static String generar(Integer anio, Integer mes) {
        return "reporte-huella-" + anio
                + (mes == null ? "" : "-" + String.format("%02d", mes))
                + ".pdf";
    }
}
