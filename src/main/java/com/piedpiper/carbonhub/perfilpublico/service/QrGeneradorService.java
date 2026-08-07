package com.piedpiper.carbonhub.perfilpublico.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Genera códigos QR como imágenes PNG codificadas en Base64.
 * En caso de error, retorna cadena vacía sin propagar la excepción (degradación graciosa).
 */
@Service
public class QrGeneradorService {

    private static final Logger log = LoggerFactory.getLogger(QrGeneradorService.class);
    private static final int QR_SIZE = 300;

    /**
     * Genera un código QR PNG 300x300 codificado en Base64 con prefijo data URI.
     *
     * @param contenido texto a codificar en el QR (típicamente una URL)
     * @return cadena con formato "data:image/png;base64,..." o cadena vacía si falla
     */
    public String generarQrBase64(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return "";
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            log.warn("No se pudo generar el código QR para: {}", contenido, e);
            return "";
        }
    }
}
