package com.piedpiper.carbonhub.perfilpublico.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import net.jqwik.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for QrGeneradorService.
 * Validates: Requirements 3.1, 3.2
 */
class QrGeneradorServicePropertyTest {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private final QrGeneradorService service = new QrGeneradorService();

    /**
     * Property 3: Round-trip del código QR
     *
     * For any URL canónica generada por el servicio, si el QR se genera exitosamente,
     * decodificar la imagen QR resultante produce exactamente la misma URL que fue codificada.
     * Además, la cadena qrBase64 comienza con el prefijo "data:image/png;base64,".
     *
     * **Validates: Requirements 3.1, 3.2**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 3: Round-trip del código QR")
    void qrRoundTrip_codificarYDecodificarProduceUrlOriginal(
            @ForAll("urlsCanonicals") String url) throws Exception {

        // Act: generar QR
        String resultado = service.generarQrBase64(url);

        // Assert: prefijo correcto
        assertThat(resultado)
                .as("El QR Base64 debe comenzar con el prefijo data URI PNG")
                .startsWith(DATA_URI_PREFIX);

        // Decodificar Base64 a bytes PNG
        String base64Data = resultado.substring(DATA_URI_PREFIX.length());
        byte[] pngBytes = Base64.getDecoder().decode(base64Data);

        // Leer imagen PNG
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertThat(image)
                .as("Los bytes decodificados deben formar una imagen PNG válida")
                .isNotNull();

        // Verificar dimensiones 300x300
        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);

        // Decodificar QR desde la imagen con hints para mejorar detección
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        MultiFormatReader reader = new MultiFormatReader();
        Map<DecodeHintType, Object> decodeHints = Map.of(
                DecodeHintType.TRY_HARDER, Boolean.TRUE,
                DecodeHintType.PURE_BARCODE, Boolean.TRUE,
                DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(BarcodeFormat.QR_CODE)
        );
        Result qrResult = reader.decode(bitmap, decodeHints);

        // Round-trip: el texto decodificado del QR debe ser idéntico a la URL original
        assertThat(qrResult.getText())
                .as("El texto decodificado del QR debe ser igual a la URL original")
                .isEqualTo(url);
    }

    /**
     * Generador de URLs canónicas válidas con el patrón del perfil público.
     * Produce URLs como: https://{domain}/empresa/{slug}/reputacion
     */
    @Provide
    Arbitrary<String> urlsCanonicals() {
        Arbitrary<String> domains = Arbitraries.of(
                "carbonhub.app",
                "staging.carbonhub.app",
                "localhost:4200",
                "dev.carbonhub.io"
        );

        Arbitrary<String> slugs = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(3)
                .ofMaxLength(60)
                .filter(s -> !s.startsWith("-") && !s.endsWith("-") && !s.contains("--"));

        return Combinators.combine(domains, slugs)
                .as((domain, slug) -> "https://" + domain + "/empresa/" + slug + "/reputacion");
    }
}
