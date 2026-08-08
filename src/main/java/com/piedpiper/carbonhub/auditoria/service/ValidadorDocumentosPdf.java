package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class ValidadorDocumentosPdf {

    public static final int MAXIMO_DOCUMENTOS = 10;
    public static final long TAMANIO_MAXIMO_BYTES = 15L * 1024 * 1024;

    private static final String TIPO_CONTENIDO_PDF = "application/pdf";

    private static final Logger log = LoggerFactory.getLogger(ValidadorDocumentosPdf.class);

    public void validar(List<MultipartFile> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            throw ApiException.documentosRespaldoRequeridos();
        }
        if (documentos.size() > MAXIMO_DOCUMENTOS) {
            throw ApiException.documentosRespaldoExcedenMaximo();
        }
        documentos.forEach(this::validarDocumento);
    }

    private void validarDocumento(MultipartFile documento) {
        if (documento == null || documento.isEmpty()) {
            throw ApiException.documentosRespaldoRequeridos();
        }
        if (documento.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw ApiException.documentoRespaldoExcedeTamanio();
        }
        // La regla es la estructura parseable y no el content type: ese ultimo lo manda el cliente y se
        // falsea trivialmente, asi que como defensa no aporta nada. Y exigirlo ademas de la estructura
        // rechazaba archivos legitimos: un PDF de verdad enviado como application/octet-stream, que
        // es lo que mandan varios clientes cuando no reconocen la extension, no pasaba.
        if (!esPdfEstructuralmenteValido(documento)) {
            throw ApiException.documentoRespaldoNoEsPdf();
        }
        if (!TIPO_CONTENIDO_PDF.equalsIgnoreCase(documento.getContentType())) {
            log.warn("El documento {} tiene firma PDF valida pero llego con content type {}",
                    documento.getOriginalFilename(), documento.getContentType());
        }
    }

    private boolean esPdfEstructuralmenteValido(MultipartFile documento) {
        try (PDDocument pdf = Loader.loadPDF(documento.getBytes())) {
            return !pdf.isEncrypted() && pdf.getNumberOfPages() > 0;
        } catch (IOException e) {
            log.warn("Documento de respaldo rechazado por estructura PDF invalida: {}",
                    documento.getOriginalFilename());
            return false;
        }
    }
}
