package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorReporteAuditoriaPdfTest {

    private final ValidadorReporteAuditoriaPdf validador =
            new ValidadorReporteAuditoriaPdf(new ProcesadorReportePdf());

    @Test
    void aceptaReportePdfConMimeFirmaYProcesamientoValido() {
        MockMultipartFile reporte = reporte(MediaType.APPLICATION_PDF_VALUE, pdfValido());

        byte[] contenido = validador.validar(reporte);

        assertThat(contenido).startsWith("%PDF-".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void rechazaMimeQueNoSeaExactamenteApplicationPdf() {
        MockMultipartFile reporte = reporte(MediaType.APPLICATION_OCTET_STREAM_VALUE, pdfValido());

        assertThatThrownBy(() -> validador.validar(reporte))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.");
    }

    @Test
    void rechazaFirmaIncorrectaAunqueElMimeSeaPdf() {
        MockMultipartFile reporte = reporte(MediaType.APPLICATION_PDF_VALUE,
                "PK\u0003\u0004 no es pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validador.validar(reporte))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.");
    }

    @Test
    void rechazaPdfQueNoPuedeProcesarse() {
        MockMultipartFile reporte = reporte(MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 incompleto".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validador.validar(reporte))
                .isInstanceOf(ApiException.class)
                .hasMessage("El archivo no pudo ser procesado. Verifica que no esté dañado y vuelve a intentarlo.");
    }

    @Test
    void rechazaArchivoQueSuperaVeinticincoMb() {
        MockMultipartFile reporte = new MockMultipartFile(
                "reporteAuditoria",
                "reporte.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[(int) ValidadorReporteAuditoriaPdf.TAMANIO_MAXIMO_BYTES + 1]);

        assertThatThrownBy(() -> validador.validar(reporte))
                .isInstanceOf(ApiException.class)
                .hasMessage("El archivo no puede superar 25 MB.");
    }

    private static MockMultipartFile reporte(String tipoContenido, byte[] contenido) {
        return new MockMultipartFile("reporteAuditoria", "reporte.pdf", tipoContenido, contenido);
    }

    private static byte[] pdfValido() {
        return """
                %PDF-1.7
                1 0 obj
                << /Type /Catalog /Pages 2 0 R >>
                endobj
                2 0 obj
                << /Type /Pages /Kids [3 0 R] /Count 1 >>
                endobj
                3 0 obj
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 1 1] >>
                endobj
                xref
                0 4
                0000000000 65535 f
                0000000009 00000 n
                0000000074 00000 n
                0000000131 00000 n
                trailer
                << /Root 1 0 R /Size 4 >>
                startxref
                207
                %%EOF
                """.getBytes(StandardCharsets.ISO_8859_1);
    }
}
