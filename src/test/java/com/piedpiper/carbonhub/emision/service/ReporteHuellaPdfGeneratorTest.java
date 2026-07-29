package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteHuellaPdfGeneratorTest {

    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

    private final ReporteHuellaPdfGenerator generator = new ReporteHuellaPdfGenerator();

    @Test
    void generaPdfNoVacioConDatos() {
        byte[] pdf = generator.generar(reporte(new BigDecimal("1500.000"), false));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, PDF_CHARSET)).startsWith("%PDF-1.4");
    }

    @Test
    void generaPdfNoVacioConTotalesEnCero() {
        byte[] pdf = generator.generar(reporte(BigDecimal.ZERO, true));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, PDF_CHARSET)).contains("No hay emisiones registradas en el período.");
    }

    @Test
    void conservaAcentosYNombreArchivoMensual() {
        byte[] pdf = generator.generar(reporte(new BigDecimal("100.000"), false, "Café del Valle S.A.", 7));

        String contenido = new String(pdf, PDF_CHARSET);
        assertThat(contenido)
                .contains("Café del Valle S.A.")
                .contains("Desglose por categoría")
                .contains("Archivo: reporte-huella-2026-07.pdf");
    }

    @Test
    void declaraWinAnsiEncodingParaMostrarTildesEnFuentesBase() {
        byte[] pdf = generator.generar(reporte(new BigDecimal("100.000"), false, "Café del Valle S.A.", 7));

        String contenido = new String(pdf, PDF_CHARSET);
        assertThat(contenido)
                .contains("/BaseFont /Helvetica /Encoding /WinAnsiEncoding")
                .contains("/BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding")
                .contains("/BaseFont /Courier-Bold /Encoding /WinAnsiEncoding");
    }

    private ReporteHuellaPdfDTO reporte(BigDecimal totalKg, boolean sinDatos) {
        return reporte(totalKg, sinDatos, "CarbonHub Demo", null);
    }

    private ReporteHuellaPdfDTO reporte(BigDecimal totalKg, boolean sinDatos, String empresa, Integer mes) {
        BigDecimal totalT = totalKg.divide(new BigDecimal("1000"), 4, java.math.RoundingMode.HALF_UP);
        return new ReporteHuellaPdfDTO(
                empresa,
                2026,
                mes,
                totalKg,
                totalT,
                Arrays.stream(CategoriaEmision.values())
                        .map(categoria -> new ReporteHuellaCategoriaDTO(categoria, BigDecimal.ZERO, BigDecimal.ZERO))
                        .toList(),
                new ReporteHuellaComparacionDTO(totalT, null, null, "sin_limite"),
                sinDatos,
                ZonedDateTime.of(2026, 7, 18, 10, 30, 0, 0, ZoneId.of("America/Costa_Rica"))
        );
    }
}
