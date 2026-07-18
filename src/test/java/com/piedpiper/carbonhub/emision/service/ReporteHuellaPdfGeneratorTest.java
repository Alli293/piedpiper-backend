package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteHuellaPdfGeneratorTest {

    private final ReporteHuellaPdfGenerator generator = new ReporteHuellaPdfGenerator();

    @Test
    void generaPdfNoVacioConDatos() {
        byte[] pdf = generator.generar(reporte(new BigDecimal("1500.000"), false));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).startsWith("%PDF-1.4");
    }

    @Test
    void generaPdfNoVacioConTotalesEnCero() {
        byte[] pdf = generator.generar(reporte(BigDecimal.ZERO, true));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("No hay emisiones registradas en el periodo.");
    }

    private ReporteHuellaPdfDTO reporte(BigDecimal totalKg, boolean sinDatos) {
        BigDecimal totalT = totalKg.divide(new BigDecimal("1000"), 4, java.math.RoundingMode.HALF_UP);
        return new ReporteHuellaPdfDTO(
                "CarbonHub Demo",
                2026,
                null,
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
