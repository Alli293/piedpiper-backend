package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.dtos.AsignarAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.CrearSolicitudAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class SolicitudAuditoriaController {

    private final SolicitudAuditoriaService solicitudAuditoriaService;

    public SolicitudAuditoriaController(SolicitudAuditoriaService solicitudAuditoriaService) {
        this.solicitudAuditoriaService = solicitudAuditoriaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitudAuditoriaResponseDTO> crear(
            @RequestPart("datos") @Valid CrearSolicitudAuditoriaRequestDTO datos,
            @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos,
            Authentication authentication) {
        SolicitudAuditoriaResponseDTO respuesta = solicitudAuditoriaService.crear(
                datos, documentos, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{idSolicitud}")
    public ResponseEntity<SolicitudAuditoriaResponseDTO> obtener(
            @PathVariable UUID idSolicitud,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudAuditoriaService.obtener(
                idSolicitud, Autenticaciones.usuarioId(authentication)));
    }

    @PostMapping("/{idSolicitud}/auditor")
    public ResponseEntity<SolicitudAuditoriaResponseDTO> asignarAuditor(
            @PathVariable UUID idSolicitud,
            @Valid @RequestBody AsignarAuditorRequestDTO datos,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudAuditoriaService.asignarAuditor(
                idSolicitud, datos, Autenticaciones.usuarioId(authentication)));
    }
}
