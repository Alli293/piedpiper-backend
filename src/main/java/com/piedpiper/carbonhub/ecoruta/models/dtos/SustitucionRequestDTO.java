package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SustitucionRequestDTO {

    @NotBlank(message = "El nombre de la alternativa es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;
    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;
    private BigDecimal costoAproximado;
    private String moneda;
    @Size(max = 200, message = "El establecimiento no puede exceder 200 caracteres")
    private String establecimientoRecomendado;
    @NotNull(message = "El puntaje ecológico (ecoScore) es obligatorio")
    @Min(value = 0, message = "El puntaje ecológico debe ser al menos 0")
    @Max(value = 100, message = "El puntaje ecológico no puede exceder 100")
    private Integer ecoScore;
    @NotBlank(message = "La categoría turística es obligatoria")
    private String categoriaTuristica;
    @NotBlank(message = "La provincia es obligatoria")
    private String provincia;
}
