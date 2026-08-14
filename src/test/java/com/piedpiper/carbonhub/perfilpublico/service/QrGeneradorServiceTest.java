package com.piedpiper.carbonhub.perfilpublico.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class QrGeneradorServiceTest {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private QrGeneradorService service;

    @BeforeEach
    void prepararServicio() {
        service = new QrGeneradorService();
    }

    @Test
    void generarQrBase64ProducePngValidoYDecodificable() throws Exception {
        String url = "https://carbonhub.app/empresa/empresa-verde/reputacion";

        String resultado = service.generarQrBase64(url);

        // Extraer bytes del PNG
        assertThat(resultado).startsWith(DATA_URI_PREFIX);
        String base64Data = resultado.substring(DATA_URI_PREFIX.length());
        byte[] pngBytes = Base64.getDecoder().decode(base64Data);

        // Verificar que es una imagen PNG válida
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);

        // Round-trip: decodificar el QR y verificar que contiene la URL original
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result qrResult = new MultiFormatReader().decode(bitmap);
        assertThat(qrResult.getText()).isEqualTo(url);
    }

    @Test
    void generarQrBase64RetornaCadenaConPrefijoDataUri() {
        String contenido = "https://carbonhub.app/empresa/test-slug/reputacion";

        String resultado = service.generarQrBase64(contenido);

        assertThat(resultado).startsWith(DATA_URI_PREFIX);
        // Verificar que después del prefijo hay contenido Base64 válido (no vacío)
        String base64Data = resultado.substring(DATA_URI_PREFIX.length());
        assertThat(base64Data).isNotBlank();
        // No debe lanzar excepción al decodificar
        byte[] decoded = Base64.getDecoder().decode(base64Data);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void generarQrBase64RetornaCadenaVaciaAnteInputNull() {
        String resultado = service.generarQrBase64(null);

        assertThat(resultado).isEmpty();
    }

    @Test
    void generarQrBase64RetornaCadenaVaciaAnteInputVacio() {
        String resultado = service.generarQrBase64("");

        assertThat(resultado).isEmpty();
    }

    @Test
    void generarQrBase64RetornaCadenaVaciaAnteInputEnBlanco() {
        String resultado = service.generarQrBase64("   ");

        assertThat(resultado).isEmpty();
    }
}
