package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.dtos.DecisionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.service.DecisionAuditorService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controlador aparte del de solicitudes porque el rol es otro: esto lo opera el auditor, no la
 * empresa. Meterlo en {@code SolicitudAuditoriaController} obligaria a bajar el
 * {@code @PreAuthorize} de la clase a nivel de metodo en todos los endpoints y dejaria el rol de
 * cada uno mas dificil de leer.
 */
@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasRole('AUDITOR_CERTIFICADO')")
public class DecisionAuditorController {

    private final DecisionAuditorService decisionAuditorService;

    public DecisionAuditorController(DecisionAuditorService decisionAuditorService) {
        this.decisionAuditorService = decisionAuditorService;
    }

    @PostMapping("/{idSolicitud}/decision")
    public ResponseEntity<Void> responder(@PathVariable UUID idSolicitud,
                                          @Valid @RequestBody DecisionAuditorRequestDTO datos,
                                          Authentication authentication) {
        decisionAuditorService.responder(idSolicitud, datos, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.ok().build();
    }
}
