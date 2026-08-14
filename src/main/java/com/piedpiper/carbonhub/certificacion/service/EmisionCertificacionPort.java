package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;

/**
 * Puerto de entrada para emitir certificaciones.
 *
 * <p>Existe para que el futuro dominio de auditorias dependa de esta interfaz y
 * no de la implementacion: cuando una auditoria se apruebe, su servicio inyecta
 * este puerto y lo invoca <em>despues del commit</em> de la aprobacion, con el
 * mismo patron de {@code TransactionSynchronizationManager} que ya usan
 * {@code InvitacionService.enviarTrasCommit} y
 * {@code ValidacionAuditorService.enviarCorreoTrasCommit}. Asi un fallo de
 * emision nunca revierte la aprobacion de la auditoria.
 *
 * <p>La operacion es <strong>idempotente por {@code idAuditoria}</strong>: si ya
 * existe una certificacion para esa auditoria, se devuelve la existente con
 * {@code recienEmitida = false} y no se crea un duplicado. Eso hace que el
 * reintento manual del administrador de plataforma sea seguro.
 */
public interface EmisionCertificacionPort {

    CertificacionResponseDTO emitirPorAuditoriaAprobada(EmitirCertificacionRequestDTO comando);
}
