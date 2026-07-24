package com.piedpiper.carbonhub.reconocimiento.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum EventoReconocimientoCodigo {
    PRIMER_ITINERARIO_GENERADO("primer_itinerario_generado"),
    CINCO_ITINERARIOS_GENERADOS("cinco_itinerarios_generados"),
    DIEZ_ITINERARIOS_GENERADOS("diez_itinerarios_generados"),
    PRIMER_ITINERARIO_SOSTENIBLE("primer_itinerario_sostenible"),
    USUARIO_RECURRENTE("usuario_recurrente"),
    EXPLORADOR_DE_PROVINCIAS("explorador_de_provincias");

    private final String codigo;

    EventoReconocimientoCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Optional<EventoReconocimientoCodigo> desde(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String normalizado = codigo.trim();
        return Arrays.stream(values())
                .filter(evento -> evento.codigo.equals(normalizado))
                .findFirst();
    }
}
