package com.piedpiper.carbonhub.auditoria.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudAuditoriaRequestDTO {

    @NotNull(message = "Seleccione la fecha de inicio del período a auditar.")
    private LocalDate periodoInicio;

    @NotNull(message = "Seleccione la fecha de fin del período a auditar.")
    private LocalDate periodoFin;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
    private String descripcionSolicitud;
}
