package com.piedpiper.carbonhub.certificacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos ya resueltos de una alerta de vencimiento, listos para armar el correo (PP-71). Es un DTO
 * interno: no se expone por ningun endpoint, solo viaja del servicio que lee la alerta al que envia.
 *
 * <p>Se arma una sola vez, dentro de la transaccion que lee la alerta, para que el envio y sus
 * reintentos no dependan de entidades JPA ya desasociadas de la sesion.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaVencimientoNotificacionDTO {

    /**
     * El ticket lo documenta como {@code Long}, pero las llaves de este repo son {@code UUID} y la
     * de {@code Alerta} tambien; se mantiene el tipo real.
     */
    private UUID idAlerta;
    private String correoDestinatario;
    private String nombreEmpresa;
    private String nombreCertificacion;
    private LocalDate fechaVencimiento;
    private long diasRestantes;
    /** Codigo del umbral en el formato acordado con PP-70: {@code 90_dias}, {@code 30_dias} o {@code 7_dias}. */
    private String tipoAlerta;
    private String urlCertificacion;
}
