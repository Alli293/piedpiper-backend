package com.piedpiper.carbonhub.validacion.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.common.NombresArchivo;
import com.piedpiper.carbonhub.validacion.models.dtos.DecisionSolicitudRequestDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudDetalleResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;
import com.piedpiper.carbonhub.validacion.service.ValidacionAuditorService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/solicitudes-auditor")
@PreAuthorize("hasRole('ADMINISTRADOR_PLATAFORMA')")
public class ValidacionAuditorController {

    private final ValidacionAuditorService validacionAuditorService;

    public ValidacionAuditorController(ValidacionAuditorService validacionAuditorService) {
        this.validacionAuditorService = validacionAuditorService;
    }

    @GetMapping
    public ResponseEntity<PaginaSolicitudesResponseDTO> listarPendientes(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(validacionAuditorService.listarPendientes(usuarioId, pagina));
    }

    @GetMapping("/{solicitudId}")
    public ResponseEntity<SolicitudDetalleResponseDTO> obtenerDetalle(
            Authentication authentication,
            @PathVariable UUID solicitudId) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(validacionAuditorService.obtenerDetalle(usuarioId, solicitudId));
    }

    @GetMapping("/{solicitudId}/documentos/{documentoId}")
    public ResponseEntity<byte[]> descargarDocumento(
            Authentication authentication,
            @PathVariable UUID solicitudId,
            @PathVariable UUID documentoId) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        DocumentoCredencialAuditor documento = validacionAuditorService.obtenerDocumento(
                usuarioId, solicitudId, documentoId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(NombresArchivo.seguro(documento.getNombreArchivo()), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(documento.getContenido());
    }

    @PostMapping("/{solicitudId}/decision")
    public ResponseEntity<SolicitudResueltaResponseDTO> resolver(
            Authentication authentication,
            @PathVariable UUID solicitudId,
            @Valid @RequestBody DecisionSolicitudRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(validacionAuditorService.resolver(usuarioId, solicitudId, request));
    }
}
