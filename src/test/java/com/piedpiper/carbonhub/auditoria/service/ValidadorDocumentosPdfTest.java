package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDocumentosPdfTest {

    private final ValidadorDocumentosPdf validador = new ValidadorDocumentosPdf();

    @Test
    void aceptaArchivoConTipoPdfYEstructuraValida() {
        assertThatNoException().isThrownBy(() -> validador.validar(List.of(pdf("respaldo.pdf"))));
    }

    @Test
    void rechazaArchivoConExtensionPdfPeroSinFirmaBinaria() {
        MultipartFile falso = new MockMultipartFile("documentos", "respaldo.pdf",
                MediaType.APPLICATION_PDF_VALUE, "PK esto es un zip".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validador.validar(List.of(falso)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aceptaArchivoConFirmaPdfAunqueElTipoDeContenidoNoLoSea() {
        MultipartFile pdfComoOctetStream = new MockMultipartFile("documentos", "respaldo.pdf",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, contenidoPdf());

        assertThatNoException().isThrownBy(() -> validador.validar(List.of(pdfComoOctetStream)));
    }

    @Test
    void rechazaArchivoSinFirmaPdfAunqueSeAnuncieComoPdf() {
        MultipartFile falso = new MockMultipartFile("documentos", "respaldo.pdf",
                MediaType.APPLICATION_PDF_VALUE, "GIF89a esto es una imagen".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validador.validar(List.of(falso)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.");
    }

    @Test
    void aceptaArchivoConFirmaPdfSinTipoDeContenido() {
        MultipartFile sinTipo = new MockMultipartFile("documentos", "respaldo.pdf", null, contenidoPdf());

        assertThatNoException().isThrownBy(() -> validador.validar(List.of(sinTipo)));
    }

    @Test
    void rechazaArchivoDemasiadoCortoParaContenerLaFirma() {
        MultipartFile corto = new MockMultipartFile("documentos", "respaldo.pdf",
                MediaType.APPLICATION_PDF_VALUE, "%PD".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validador.validar(List.of(corto)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se aceptan archivos en formato PDF.");
    }

    @Test
    void rechazaListaVaciaDeDocumentos() {
        assertThatThrownBy(() -> validador.validar(List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Debes adjuntar al menos un documento de respaldo.");
    }

    @Test
    void rechazaMasDeDiezDocumentos() {
        List<MultipartFile> once = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(indice -> pdf("respaldo-" + indice + ".pdf"))
                .map(MultipartFile.class::cast)
                .toList();

        assertThatThrownBy(() -> validador.validar(once))
                .isInstanceOf(ApiException.class)
                .hasMessage("Puedes adjuntar un máximo de 10 documentos.");
    }

    @Test
    void aceptaExactamenteDiezDocumentos() {
        List<MultipartFile> diez = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(indice -> pdf("respaldo-" + indice + ".pdf"))
                .map(MultipartFile.class::cast)
                .toList();

        assertThatNoException().isThrownBy(() -> validador.validar(diez));
    }

    @Test
    void elLimiteDeTamanioEsDeQuinceMegabytes() {
        assertThat(ValidadorDocumentosPdf.TAMANIO_MAXIMO_BYTES).isEqualTo(15L * 1024 * 1024);
    }

    static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("documentos", nombre, MediaType.APPLICATION_PDF_VALUE, contenidoPdf());
    }

    static byte[] contenidoPdf() {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
