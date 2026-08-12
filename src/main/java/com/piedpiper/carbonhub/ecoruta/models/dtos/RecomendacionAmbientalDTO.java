package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Una recomendación ambiental individual para mejorar el EcoScore de un itinerario (PP-93).
 * Por ahora el único tipo soportado es {@code ACTIVIDAD_ALTERNATIVA}: sustituir una actividad
 * mejorable por la mejor alternativa encontrada vía {@link com.piedpiper.carbonhub.ecoruta.service.AlternativasIaClienteService}
 * (mismo mecanismo que PP-92). Los campos {@code actividadId}, {@code alternativa}, {@code categoriaTuristica}
 * y {@code provincia} viajan junto a la recomendación para que el cliente pueda aplicarla
 * directamente contra {@code PUT /ecoruta/itinerarios/{id}/actividades/{actividadId}/sustituir}
 * sin una consulta adicional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionAmbientalDTO {

    private String tipo;
    private UUID actividadId;
    private String actividadNombre;
    private String descripcion;
    private BigDecimal incrementoEstimado;
    private AlternativaDTO alternativa;
    private String categoriaTuristica;
    private String provincia;
}
