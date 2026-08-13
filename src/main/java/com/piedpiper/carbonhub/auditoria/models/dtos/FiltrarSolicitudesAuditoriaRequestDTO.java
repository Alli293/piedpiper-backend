package com.piedpiper.carbonhub.auditoria.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Filtros del listado de solicitudes de auditoria.
 *
 * <p>{@code filtroEstado} llega como texto y no como enum a proposito: la historia pide que un
 * valor desconocido se ignore y se devuelva el listado completo, no que la peticion falle. Con el
 * enum, Jackson respondería 400 antes de que el servicio pudiera decidir nada.</p>
 *
 * <p>{@code idEmpresa} e {@code idAuditor} solo los honra el administrador de plataforma. Para
 * cualquier otro rol el servicio los ignora y resuelve el dueño del listado desde el token, asi que
 * enviarlos no abre ninguna puerta.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltrarSolicitudesAuditoriaRequestDTO {

    private List<String> filtroEstado;
    private Integer pagina;
    private UUID idEmpresa;
    private UUID idAuditor;
}
