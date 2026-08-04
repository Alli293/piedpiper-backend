package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shape crudo que se le pide a Gemini vía {@code ChatClient...call().entity(...)}. Nunca se expone
 * al frontend ni se persiste directamente — {@code ItinerarioValidador} la valida y el servicio la
 * traduce a entidades. Todos los campos son tipos simples (sin enums Java) porque no se puede
 * confiar en que la IA respete el catálogo; la resolución a enum (vía {@code Catalogos.desde(...)})
 * ocurre recién durante la validación/traducción, igual que {@code PreferenciasViajeRequestDTO}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioIaResponseDTO {

    private List<DiaIaDTO> dias;
    private Integer puntuacionAmbientalPreliminar;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaIaDTO {
        private Integer numeroDia;
        private String fecha;
        private List<ActividadIaDTO> actividades;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActividadIaDTO {
        private String nombre;
        private String descripcion;
        private String horario;
        private Integer duracionMinutos;
        private BigDecimal costoAproximado;
        private String moneda;
        private String establecimientoRecomendado;
        private String provincia;
        /** Score ambiental estimado por la IA (0-100), null si no pudo estimar. */
        private Integer puntuacionAmbientalEstimada;
    }
}
