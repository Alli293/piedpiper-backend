package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reintento manual de la emision, para cuando la emision automatica falla.
 *
 * <p>Hoy recibe los datos de la auditoria en el cuerpo porque el dominio de
 * auditorias todavia no existe. Cuando exista, esto deberia pasar a ser
 * {@code POST /api/certificaciones/auditorias/{idAuditoria}/reintentos} sin
 * cuerpo, leyendo los datos de la auditoria aprobada.
 */
@RestController
@RequestMapping("/api/certificaciones/reintentos")
@PreAuthorize("hasRole('ADMINISTRADOR_PLATAFORMA')")
public class CertificacionAdminController {

    private final EmisionCertificacionPort emisionCertificacionPort;

    public CertificacionAdminController(EmisionCertificacionPort emisionCertificacionPort) {
        this.emisionCertificacionPort = emisionCertificacionPort;
    }

    @PostMapping
    public ResponseEntity<CertificacionResponseDTO> reintentar(
            @Valid @RequestBody EmitirCertificacionRequestDTO request) {
        CertificacionResponseDTO response =
                emisionCertificacionPort.emitirPorAuditoriaAprobada(request);
        HttpStatus status = response.isRecienEmitida() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
