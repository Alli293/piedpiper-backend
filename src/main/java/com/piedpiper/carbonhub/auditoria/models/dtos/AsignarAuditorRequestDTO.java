package com.piedpiper.carbonhub.auditoria.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * origenAsignacion se recibe como String y se valida contra el catálogo de enums
 * en el servicio, de forma que un valor fuera de catálogo responda 422
 * (con Jackson deserializando el enum directamente respondería 400).
 * Es la misma excepción documentada en CONVENTIONS §7.3 para PreferenciasUsuarioRequestDTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarAuditorRequestDTO {

    @NotNull(message = "Selecciona un auditor.")
    private UUID idAuditor;

    @NotBlank(message = "Selecciona el origen de la asignación.")
    @Size(max = 30, message = "Selecciona el origen de la asignación.")
    private String origenAsignacion;
}
