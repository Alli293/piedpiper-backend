package com.piedpiper.carbonhub.certificacion.models.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de certificacion que CarbonHub puede emitir, segun el tipo de auditoria
 * realizada. El catalogo esta inspirado en la escalera de reconocimientos del
 * Programa Pais (PPCN/PPLC) de Costa Rica, pero usa nomenclatura propia de CarbonHub
 */
public enum TipoCertificacion {
    INVENTARIO_GEI("inventario_gei"),
    REDUCCION_EMISIONES("reduccion_emisiones"),
    REDUCCION_PLUS("reduccion_plus"),
    CARBONO_NEUTRAL("carbono_neutral"),
    CARBONO_NEUTRAL_PLUS("carbono_neutral_plus"),
    ADAPTACION_CLIMATICA("adaptacion_climatica"),
    HUELLA_PRODUCTO("huella_producto");

    private final String codigo;

    TipoCertificacion(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
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
