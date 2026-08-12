package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un turno del historial conversacional del chat de refinamiento (PP-88). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeConversacionDTO {

    private String rol;
    private String contenido;
}
