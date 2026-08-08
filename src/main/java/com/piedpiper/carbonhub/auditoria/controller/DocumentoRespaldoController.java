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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'AUDITOR_CERTIFICADO', 'ADMINISTRADOR_PLATAFORMA')")
public class DocumentoRespaldoController {

    static final String NOMBRE_POR_DEFECTO = "documento.pdf";

    /** Saltos de linea, retornos de carro, comillas y barras: separadores de cabecera y de ruta. */
    private static final Pattern CARACTERES_PELIGROSOS = Pattern.compile("[\\r\\n\"\\\\/]");

    private final DocumentoRespaldoDescargaService documentoRespaldoDescargaService;

    public DocumentoRespaldoController(DocumentoRespaldoDescargaService documentoRespaldoDescargaService) {
        this.documentoRespaldoDescargaService = documentoRespaldoDescargaService;
    }

    /**
     * Va como {@code attachment} para que contenido activo dentro de un PDF no se ejecute
     * automaticamente en el contexto del navegador.
     *
     * <p>El tipo de contenido va fijo en {@code application/pdf} y no sale del {@code tipoContenido}
     * guardado a proposito: ese campo llega del cliente al subir el archivo y
     * {@code ValidadorDocumentosPdf} solo lo registra en el log cuando no coincide, no lo rechaza.
     * Lo que si valida es la estructura completa con PDFBox.
     * Devolver el valor declarado por el cliente le dejaria decidir como interpreta el navegador un
     * contenido que ya sabemos que es un PDF.</p>
     */
    @GetMapping("/{idSolicitud}/documentos/{idDocumento}")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID idSolicitud,
                                            @PathVariable UUID idDocumento,
                                            Authentication authentication) {
        DocumentoRespaldo documento = documentoRespaldoDescargaService.obtener(
                idSolicitud, idDocumento, Autenticaciones.usuarioId(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombreSeguro(documento.getNombreArchivo()), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(documento.getContenido());
    }

    /**
     * Saca del nombre lo que podria romper la cabecera. El nombre lo eligio quien subio el archivo,
     * y uno con salto de linea o retorno de carro permite inyectar cabeceras adicionales en la
     * respuesta; las comillas cierran el valor antes de tiempo y las barras arrastran rutas.
     *
     * <p>El {@code filename} se emite con codificacion UTF-8, que es lo que permite conservar los
     * acentos en vez de tener que descartarlos.</p>
     */
    static String nombreSeguro(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return NOMBRE_POR_DEFECTO;
        }
        String limpio = CARACTERES_PELIGROSOS.matcher(nombreArchivo).replaceAll("_").trim();
        return limpio.isEmpty() ? NOMBRE_POR_DEFECTO : limpio;
    }
}
