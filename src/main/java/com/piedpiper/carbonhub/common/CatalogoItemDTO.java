package com.piedpiper.carbonhub.common;

/**
 * DTO estándar para ítems de catálogo.
 * Contrato acordado con PP-51/52: cada ítem expone su valor técnico (enum name)
 * y una etiqueta legible para el usuario.
 */
public record CatalogoItemDTO(String valor, String etiqueta) {

    /**
     * Genera una etiqueta legible a partir del nombre de un enum.
     * Ejemplo: HUELLA_CARBONO → "Huella carbono"
     */
    public static String etiquetaDesdeEnum(String enumName) {
        return enumName
                .replace('_', ' ')
                .toLowerCase()
                .replaceFirst("^\\w", String.valueOf(Character.toUpperCase(enumName.charAt(0))));
    }
}
