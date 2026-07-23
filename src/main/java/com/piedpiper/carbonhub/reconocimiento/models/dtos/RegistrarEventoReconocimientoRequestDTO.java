package com.piedpiper.carbonhub.reconocimiento.models.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEventoReconocimientoRequestDTO {

    @NotNull(message = "No se pudo identificar el usuario que genero el evento.")
    @JsonProperty("usuario_id")
    @JsonAlias("usuarioId")
    private UUID usuarioId;

    @NotBlank(message = "Indique el evento generado.")
    @Size(max = EventoReconocimiento.EVENTO_GENERADO_MAX,
            message = "El codigo del evento no puede contener mas de 80 caracteres.")
    @JsonProperty("evento_generado")
    @JsonAlias("eventoGenerado")
    private String eventoGenerado;
}
