package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class ValidadorDocumentosPdf {

    public static final int MAXIMO_DOCUMENTOS = 10;
    public static final long TAMANIO_MAXIMO_BYTES = 15L * 1024 * 1024;

    private static final String TIPO_CONTENIDO_PDF = "application/pdf";
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

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
        // La regla es la firma binaria y no el content type: ese ultimo lo manda el cliente y se
        // falsea trivialmente, asi que como defensa no aporta nada. Y exigirlo ademas de la firma
        // rechazaba archivos legitimos: un PDF de verdad enviado como application/octet-stream, que
        // es lo que mandan varios clientes cuando no reconocen la extension, no pasaba.
        if (!tieneFirmaPdf(documento)) {
            throw ApiException.documentoRespaldoNoEsPdf();
        }
        if (!TIPO_CONTENIDO_PDF.equalsIgnoreCase(documento.getContentType())) {
            log.warn("El documento {} tiene firma PDF valida pero llego con content type {}",
                    documento.getOriginalFilename(), documento.getContentType());
        }
    }

    private boolean tieneFirmaPdf(MultipartFile documento) {
        byte[] encabezado = new byte[FIRMA_PDF.length];
        try (InputStream entrada = documento.getInputStream()) {
            int leidos = entrada.readNBytes(encabezado, 0, encabezado.length);
            if (leidos < FIRMA_PDF.length) {
                return false;
            }
        } catch (IOException e) {
            log.error("No se pudo leer el documento de respaldo {}", documento.getOriginalFilename(), e);
            throw ApiException.errorInterno("No se pudo leer uno de los archivos adjuntos. Intenta nuevamente.");
        }
        return Arrays.equals(encabezado, FIRMA_PDF);
    }
}
