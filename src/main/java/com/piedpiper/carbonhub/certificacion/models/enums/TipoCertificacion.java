package com.piedpiper.carbonhub.certificacion.models.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de certificacion que CarbonHub puede emitir, segun el tipo de auditoria
 * realizada. El catalogo esta inspirado en la escalera de reconocimientos del
 * Programa Pais (PPCN/PPLC) de Costa Rica, pero usa nomenclatura propia de CarbonHub
 */
public enum TipoCertificacion {
    INVENTARIO_GEI("inventario_gei", "Inventario de GEI"),
    REDUCCION_EMISIONES("reduccion_emisiones", "Reducción de Emisiones"),
    REDUCCION_PLUS("reduccion_plus", "Reducción Plus"),
    CARBONO_NEUTRAL("carbono_neutral", "Carbono Neutral"),
    CARBONO_NEUTRAL_PLUS("carbono_neutral_plus", "Carbono Neutral Plus"),
    ADAPTACION_CLIMATICA("adaptacion_climatica", "Adaptación Climática"),
    HUELLA_PRODUCTO("huella_producto", "Huella de Producto");

    private final String codigo;
    private final String etiqueta;

    TipoCertificacion(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Nombre legible para mostrar en UI (p. ej. el calendario de vencimientos de PP-77). */
    public String getEtiqueta() {
        return etiqueta;
    }

    public static Optional<TipoCertificacion> desde(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String normalizado = codigo.trim();
        return Arrays.stream(values())
                .filter(tipo -> tipo.codigo.equalsIgnoreCase(normalizado)
                        || tipo.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
