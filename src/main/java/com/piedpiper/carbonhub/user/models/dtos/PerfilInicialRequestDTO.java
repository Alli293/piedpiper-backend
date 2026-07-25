package com.piedpiper.carbonhub.user.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilInicialRequestDTO {

    @NotBlank(message = "El nombre debe tener al menos 2 caracteres.")
    @Size(min = 2, max = 100, message = "El nombre debe tener al menos 2 caracteres.")
    private String nombreVisible;

    @NotNull(message = "Selecciona tus preferencias de interfaz.")
    @Valid
    private PreferenciasUsuarioRequestDTO preferencias;

    @Valid
    private DatosEmpresaPerfilDTO empresa;
}
