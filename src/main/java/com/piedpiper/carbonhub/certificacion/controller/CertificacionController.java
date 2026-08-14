package com.piedpiper.carbonhub.certificacion.controller;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificaciones")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class CertificacionController {

    private final ConsultaCertificacionService consultaCertificacionService;

    public CertificacionController(ConsultaCertificacionService consultaCertificacionService) {
        this.consultaCertificacionService = consultaCertificacionService;
    }

    @GetMapping
    public ResponseEntity<List<CertificacionResumenResponseDTO>> listar(Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(consultaCertificacionService.listar(usuarioId));
    }

    @GetMapping("/{certificacionId}")
    public ResponseEntity<CertificacionResponseDTO> detalle(
            Authentication authentication,
            @PathVariable UUID certificacionId) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(consultaCertificacionService.detalle(usuarioId, certificacionId));
    }

    @GetMapping("/{certificacionId}/jsonld")
    public ResponseEntity<Map<String, Object>> descargarJsonLd(
            Authentication authentication,
            @PathVariable UUID certificacionId) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Map<String, Object> credencial = consultaCertificacionService.descargarJsonLd(usuarioId, certificacionId);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vc+ld+json"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("certificacion-" + certificacionId + ".jsonld")
                        .build()
                        .toString())
                .body(credencial);
    }
}
