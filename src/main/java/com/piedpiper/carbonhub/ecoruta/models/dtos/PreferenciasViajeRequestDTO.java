package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * tipoViaje, provinciaPreferida y cada valor de intereses se reciben como String y se validan
 * contra sus catálogos de enums en el servicio (422 si no calzan), en vez de recibir el enum
 * directo — mismo motivo que PreferenciasUsuarioRequestDTO: controlar el mensaje en vez de que
 * Jackson falle la deserialización con un 400 genérico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasViajeRequestDTO {

    @NotNull(message = "La duración del viaje debe estar entre 1 y 30 días.")
    @Min(value = 1, message = "La duración del viaje debe estar entre 1 y 30 días.")
    @Max(value = 30, message = "La duración del viaje debe estar entre 1 y 30 días.")
    private Integer cantidadDias;

    @NotNull(message = "Selecciona una fecha válida.")
    @FutureOrPresent(message = "Selecciona una fecha válida.")
    private LocalDate fechaInicio;

    @NotBlank(message = "Selecciona el tipo de viaje.")
    private String tipoViaje;

    @Size(max = 100, message = "El presupuesto no puede superar 100 caracteres.")
    private String presupuesto;

    @NotEmpty(message = "Selecciona al menos una actividad de interés.")
    private List<String> intereses;

    private String provinciaPreferida;

    @Size(max = 200, message = "La ubicación no puede superar 200 caracteres.")
    private String ubicacionActual;

    private boolean buscarCercaDeMi;

    @Size(max = 500, message = "El campo no puede superar 500 caracteres.")
    private String limitacionesMovilidad;

    private boolean requiereHospedaje;

    @AssertTrue(message = "Ingresa tu ubicación actual para buscar actividades cercanas.")
    public boolean isUbicacionValidaSiBuscaCercania() {
        return !buscarCercaDeMi || (ubicacionActual != null && !ubicacionActual.isBlank());
    }
}
