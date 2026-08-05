package com.piedpiper.carbonhub.auditoria.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Procesador local para la tercera capa de validacion del reporte. No reemplaza un renderizador
 * completo: verifica que el archivo tenga las piezas estructurales que un lector PDF necesita para
 * abrirlo como documento y permite separar "parece PDF" de "esta dañado".
 */
@Component
public class ProcesadorReportePdf {

    public boolean puedeProcesar(byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.ISO_8859_1);
        return texto.contains("/Root")
                && texto.contains("xref")
                && texto.contains("startxref")
                && texto.contains("%%EOF")
                && cantidadDe(texto, " obj") == cantidadDe(texto, "endobj");
    }

    private int cantidadDe(String texto, String fragmento) {
        int total = 0;
        int indice = texto.indexOf(fragmento);
        while (indice >= 0) {
            total++;
            indice = texto.indexOf(fragmento, indice + fragmento.length());
        }
        return total;
    }
}
