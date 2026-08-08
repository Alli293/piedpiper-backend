package com.piedpiper.carbonhub.auditoria.models.dtos;

import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransicionEstadoAuditoriaResponseDTO {

    private EstadoSolicitudAuditoria estadoAnterior;
    private EstadoSolicitudAuditoria estadoNuevo;
    private EventoTransicionAuditoria evento;
    private ActorTransicionAuditoria actor;

    /**
     * Nombre de quien origino el cambio, ya resuelto por el servidor: para las transiciones del
     * proceso automatico no hay persona y el valor es "Proceso automático". Se resuelve aca y no en
     * el cliente para que el correo y la pantalla nombren igual al responsable.
     */
    private String responsable;

    private Instant fecha;
}
