package com.piedpiper.carbonhub.validacion.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.validacion.models.dtos.MiSolicitudAuditorResponseDTO;
import com.piedpiper.carbonhub.validacion.service.MiSolicitudAuditorService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditor/mi-solicitud")
@PreAuthorize("hasRole('AUDITOR_CERTIFICADO')")
public class MiSolicitudAuditorController {

    private final MiSolicitudAuditorService miSolicitudAuditorService;

    public MiSolicitudAuditorController(MiSolicitudAuditorService miSolicitudAuditorService) {
        this.miSolicitudAuditorService = miSolicitudAuditorService;
    }

    @GetMapping
    public ResponseEntity<MiSolicitudAuditorResponseDTO> obtener(Authentication authentication) {
        return ResponseEntity.ok(
                miSolicitudAuditorService.obtener(Autenticaciones.usuarioId(authentication)));
    }
}
