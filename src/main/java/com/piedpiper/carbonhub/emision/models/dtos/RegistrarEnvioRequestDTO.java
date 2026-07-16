package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEnvioRequestDTO {

    @NotBlank(message = "Ingrese un título para este registro.")
    @Size(max = 150, message = "El título no puede contener más de 150 caracteres")
    private String titulo;

    @NotNull(message = "Ingrese un valor de peso.")
    @DecimalMin(value = "0", inclusive = false, message = "Ingrese un peso mayor que 0.")
    @Digits(integer = 12, fraction = 3, message = "Ingrese un peso válido.")
    private BigDecimal weightValue;

    @NotNull(message = "Seleccione una unidad de peso válida.")
    private UnidadPeso weightUnit;

    @NotNull(message = "Ingrese un valor de distancia.")
    @DecimalMin(value = "0", inclusive = false, message = "Ingrese una distancia mayor que 0.")
    @Digits(integer = 12, fraction = 3, message = "Ingrese una distancia válida.")
    private BigDecimal distanceValue;

    @NotNull(message = "Seleccione una unidad de distancia válida.")
    private UnidadDistancia distanceUnit = UnidadDistancia.KM;

    @NotNull(message = "Seleccione un método de transporte válido.")
    private MetodoTransporte transportMethod;

    @NotNull(message = "Ingrese una fecha para este registro.")
    @PastOrPresent(message = "La fecha no puede ser posterior a hoy.")
    private LocalDate fechaActividad;
}
