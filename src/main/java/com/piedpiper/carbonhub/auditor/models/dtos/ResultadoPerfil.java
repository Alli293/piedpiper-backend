package com.piedpiper.carbonhub.auditor.models.dtos;

/**
 * Encapsula el resultado de un upsert de perfil de auditor.
 *
 * @param dto    DTO con los datos del perfil persistido.
 * @param creado {@code true} si se creó un nuevo perfil; {@code false} si se actualizó uno existente.
 */
public record ResultadoPerfil(PerfilAuditorResponseDTO dto, boolean creado) {
}
