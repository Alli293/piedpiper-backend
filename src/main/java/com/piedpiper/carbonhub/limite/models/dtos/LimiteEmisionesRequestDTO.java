package com.piedpiper.carbonhub.limite.models.dtos;

import com.piedpiper.carbonhub.limite.validation.AnioLimiteValido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimiteEmisionesRequestDTO {
    @NotNull(message = "Seleccione un anio valido.")
    @AnioLimiteValido
    private Integer anio;

    @NotNull(message = "Ingrese un limite mayor que 0.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ingrese un limite mayor que 0.")
    @Digits(integer = 12, fraction = 4, message = "Ingrese un limite mayor que 0.")
    private BigDecimal limiteMt;

    @Size(max = 500, message = "La justificacion no puede superar 500 caracteres.")
    private String justificacion;
}
