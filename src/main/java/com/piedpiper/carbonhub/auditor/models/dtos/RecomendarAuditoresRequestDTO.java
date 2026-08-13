package com.piedpiper.carbonhub.auditor.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros de la recomendación de auditores (PP-57).
 *
 * <p>Los tres campos de catálogo llegan como texto y no como enum: así un valor fuera del catálogo
 * se traduce a un 400 propio con mensaje claro, en vez del 400 genérico que produciría Jackson al
 * fallar la deserialización del enum. Es el mismo criterio de {@code PreferenciasUsuarioRequestDTO}.</p>
 *
 * <p>El {@code sector} y el {@code empresaId} no viajan aquí a propósito: se resuelven en el backend
 * desde el token del administrador, así que el cliente no los puede manipular.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendarAuditoresRequestDTO {

    @NotBlank(message = "Este campo es obligatorio.")
    private String tipoAuditoria;

    @NotBlank(message = "Este campo es obligatorio.")
    private String especialidadBuscada;

    @NotBlank(message = "Este campo es obligatorio.")
    private String zonaGeografica;

    /** Por defecto solo se consideran auditores disponibles; un nulo se trata como {@code true}. */
    private Boolean soloDisponibles;
}
