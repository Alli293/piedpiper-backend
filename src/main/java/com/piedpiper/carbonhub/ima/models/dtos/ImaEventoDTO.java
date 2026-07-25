package com.piedpiper.carbonhub.ima.models.dtos;

import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImaEventoDTO {

    /** Período del evento en formato ISO {@code YYYY-MM}. */
    private String mes;

    private TipoEventoIma tipo;

    /** Texto en español listo para mostrarse como tooltip del marcador. */
    private String texto;
}
