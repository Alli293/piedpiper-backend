package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.service.DocumentoRespaldoDescargaService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'AUDITOR_CERTIFICADO', 'ADMINISTRADOR_PLATAFORMA')")
public class DocumentoRespaldoController {

    private final DocumentoRespaldoDescargaService documentoRespaldoDescargaService;

    public DocumentoRespaldoController(DocumentoRespaldoDescargaService documentoRespaldoDescargaService) {
        this.documentoRespaldoDescargaService = documentoRespaldoDescargaService;
    }

    /**
     * Va {@code inline} y no {@code attachment} porque la historia pide previsualizar el documento
     * en una pestana nueva, no descargarlo. El nombre del archivo se manda igual para que, si el
     * navegador termina guardandolo, no quede con el identificador como nombre.
     */
    @GetMapping("/{idSolicitud}/documentos/{idDocumento}")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID idSolicitud,
                                            @PathVariable UUID idDocumento,
                                            Authentication authentication) {
        DocumentoRespaldo documento = documentoRespaldoDescargaService.obtener(
                idSolicitud, idDocumento, Autenticaciones.usuarioId(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(documento.getNombreArchivo())
                        .build()
                        .toString())
                .body(documento.getContenido());
    }
}
