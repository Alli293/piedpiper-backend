package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class ValidadorReporteAuditoriaPdf {

    public static final long TAMANIO_MAXIMO_BYTES = 25L * 1024 * 1024;

    private static final String TIPO_CONTENIDO_PDF = "application/pdf";
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private static final Logger log = LoggerFactory.getLogger(ValidadorReporteAuditoriaPdf.class);

    private final ProcesadorReportePdf procesadorReportePdf;

    public ValidadorReporteAuditoriaPdf(ProcesadorReportePdf procesadorReportePdf) {
        this.procesadorReportePdf = procesadorReportePdf;
    }

    /**
     * Devuelve los bytes ya validados para que el servicio de carga no vuelva a leer el stream del
     * multipart. El orden importa porque la historia pide cortar en la primera validacion fallida.
     */
    public byte[] validar(MultipartFile reporteAuditoria) {
        if (reporteAuditoria == null || reporteAuditoria.isEmpty()) {
            throw ApiException.reporteAuditoriaNoEsPdf();
        }
        if (reporteAuditoria.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw ApiException.reporteAuditoriaExcedeTamanio();
        }
        if (!TIPO_CONTENIDO_PDF.equals(reporteAuditoria.getContentType())) {
            throw ApiException.reporteAuditoriaNoEsPdf();
        }

        byte[] contenido = leer(reporteAuditoria);
        if (!tieneFirmaPdf(contenido)) {
            throw ApiException.reporteAuditoriaNoEsPdf();
        }
        validarProcesable(contenido, reporteAuditoria.getOriginalFilename());
        return contenido;
    }

    private byte[] leer(MultipartFile reporteAuditoria) {
        try {
            return reporteAuditoria.getBytes();
        } catch (IOException e) {
            log.error("No se pudo leer el reporte de auditoria {}", reporteAuditoria.getOriginalFilename(), e);
            throw ApiException.reporteAuditoriaNoProcesable();
        }
    }

    private boolean tieneFirmaPdf(byte[] contenido) {
        if (contenido.length < FIRMA_PDF.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(contenido, FIRMA_PDF.length), FIRMA_PDF);
    }

    private void validarProcesable(byte[] contenido, String nombreArchivo) {
        if (!procesadorReportePdf.puedeProcesar(contenido)) {
            log.warn("El reporte de auditoria {} no pudo procesarse como PDF", nombreArchivo);
            throw ApiException.reporteAuditoriaNoProcesable();
        }
    }
}
