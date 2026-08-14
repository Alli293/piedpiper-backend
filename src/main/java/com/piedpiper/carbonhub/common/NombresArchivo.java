package com.piedpiper.carbonhub.common;

import java.util.regex.Pattern;

/**
 * Saca del nombre de archivo lo que podria romper una cabecera HTTP de descarga. El nombre lo
 * eligio quien subio el archivo, y uno con salto de linea o retorno de carro permite inyectar
 * cabeceras adicionales en la respuesta; las comillas cierran el valor antes de tiempo y las
 * barras arrastran rutas.
 *
 * <p>Centralizado aca porque mas de un controlador de descarga de documentos (auditoria,
 * credenciales de auditor) necesita la misma regla, y una regla de seguridad escrita dos veces
 * tarde o temprano se actualiza en una sola.</p>
 */
public final class NombresArchivo {

    public static final String NOMBRE_POR_DEFECTO = "documento.pdf";

    private static final Pattern CARACTERES_PELIGROSOS = Pattern.compile("[\\r\\n\"\\\\/]");

    private NombresArchivo() {
    }

    public static String seguro(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return NOMBRE_POR_DEFECTO;
        }
        String limpio = CARACTERES_PELIGROSOS.matcher(nombreArchivo).replaceAll("_").trim();
        return limpio.isEmpty() ? NOMBRE_POR_DEFECTO : limpio;
    }
}
