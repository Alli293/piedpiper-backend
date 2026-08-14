package com.piedpiper.carbonhub.auditoria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ValidadorDocumentosPdf {

    public static final int MAXIMO_DOCUMENTOS = 10;
    public static final long TAMANIO_MAXIMO_BYTES = 15L * 1024 * 1024;

    private static final String TIPO_CONTENIDO_PDF = "application/pdf";

    private static final Logger log = LoggerFactory.getLogger(ValidadorDocumentosPdf.class);

    public void validar(List<MultipartFile> documentos) {
        validar(documentos, TipoDocumentoAdjunto.RESPALDO);
    }

    public void validar(List<MultipartFile> documentos, TipoDocumentoAdjunto tipo) {
        validarCantidad(documentos, tipo);
        documentos.forEach(documento -> validarDocumento(documento, tipo));
    }

    /**
     * Igual que {@link #validar(List, TipoDocumentoAdjunto)}, pero devuelve los bytes ya leidos de
     * cada documento (en el mismo orden) para que el llamador no vuelva a leer el stream del
     * multipart al persistirlos.
     */
    public List<byte[]> validarYLeer(List<MultipartFile> documentos, TipoDocumentoAdjunto tipo) {
        validarCantidad(documentos, tipo);
        List<byte[]> contenidos = new ArrayList<>(documentos.size());
        for (MultipartFile documento : documentos) {
            contenidos.add(validarDocumento(documento, tipo));
        }
        return contenidos;
    }

    private void validarCantidad(List<MultipartFile> documentos, TipoDocumentoAdjunto tipo) {
        if (documentos == null || documentos.isEmpty()) {
            throw tipo.documentosRequeridos();
        }
        if (documentos.size() > MAXIMO_DOCUMENTOS) {
            throw tipo.documentosExcedenMaximo();
        }
    }

    private byte[] validarDocumento(MultipartFile documento, TipoDocumentoAdjunto tipo) {
        if (documento == null || documento.isEmpty()) {
            throw tipo.documentosRequeridos();
        }
        if (documento.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw tipo.documentoExcedeTamanio();
        }
        byte[] contenido = leer(documento, tipo);
        // La regla es la estructura parseable y no el content type: ese ultimo lo manda el cliente y se
        // falsea trivialmente, asi que como defensa no aporta nada. Y exigirlo ademas de la estructura
        // rechazaba archivos legitimos: un PDF de verdad enviado como application/octet-stream, que
        // es lo que mandan varios clientes cuando no reconocen la extension, no pasaba.
        if (!esPdfEstructuralmenteValido(contenido, documento.getOriginalFilename())) {
            throw tipo.documentoNoEsPdf();
        }
        if (!TIPO_CONTENIDO_PDF.equalsIgnoreCase(documento.getContentType())) {
            log.warn("El documento {} tiene firma PDF valida pero llego con content type {}",
                    documento.getOriginalFilename(), documento.getContentType());
        }
        return contenido;
    }

    private byte[] leer(MultipartFile documento, TipoDocumentoAdjunto tipo) {
        try {
            return documento.getBytes();
        } catch (IOException e) {
            log.warn("No se pudo leer el documento {}", documento.getOriginalFilename());
            throw tipo.documentoNoEsPdf();
        }
    }

    private boolean esPdfEstructuralmenteValido(byte[] contenido, String nombreArchivo) {
        try (PDDocument pdf = Loader.loadPDF(contenido)) {
            return !pdf.isEncrypted() && pdf.getNumberOfPages() > 0;
        } catch (IOException e) {
            log.warn("Documento rechazado por estructura PDF invalida: {}", nombreArchivo);
            return false;
        }
    }
}
