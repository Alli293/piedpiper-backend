package com.piedpiper.carbonhub.auditoria.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Datos mínimos del auditor asignado a una solicitud. Se expone anidado en la respuesta para que
 * la pantalla de asignación pueda mostrar de quién se trata sin una consulta extra al directorio,
 * incluso al recargar cuando la asignación ya existía.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditorAsignadoResponseDTO {

    private UUID id;
    private String nombre;
}
