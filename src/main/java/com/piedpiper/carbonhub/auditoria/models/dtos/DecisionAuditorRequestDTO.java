package com.piedpiper.carbonhub.auditoria.models.dtos;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Decision del auditor sobre una solicitud que le fue asignada.
 *
 * <p>{@code decision} llega como {@code String} y no como enum, igual que
 * {@code PreferenciasUsuarioRequestDTO}: con el enum, un valor invalido lo rechaza Jackson al
 * deserializar y devuelve un 400 generico, cuando la historia pide 422 con un mensaje que nombre
 * los valores validos. La conversion la hace el servicio con {@code DecisionAuditor.desde}.</p>
 *
 * <p>El largo de {@code motivoRechazo} se valida aca, pero su obligatoriedad no: solo aplica cuando
 * la decision es un rechazo, y Bean Validation no puede expresar esa condicion entre dos campos sin
 * un validador propio. La comprueba el servicio.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionAuditorRequestDTO {

    @NotBlank(message = "Indica si aceptas o rechazas la solicitud.")
    private String decision;

    @Size(min = SolicitudAuditoria.MOTIVO_RECHAZO_MIN,
            max = SolicitudAuditoria.MOTIVO_RECHAZO_MAX,
            message = "El motivo del rechazo debe tener entre 10 y 300 caracteres.")
    private String motivoRechazo;
}
