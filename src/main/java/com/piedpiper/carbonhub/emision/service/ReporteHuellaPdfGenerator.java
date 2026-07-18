package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ReporteHuellaPdfGenerator {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", new Locale("es", "CR"));

    public byte[] generar(ReporteHuellaPdfDTO reporte) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Reporte de huella de carbono");
        lineas.add("Empresa: " + reporte.empresa());
        lineas.add("Periodo: " + periodo(reporte.anio(), reporte.mes()));
        lineas.add("");
        lineas.add("Total del periodo: " + formato(reporte.totalKg(), 3) + " kg CO2e");
        lineas.add("Total del periodo: " + formato(reporte.totalT(), 4) + " t CO2e");
        if (reporte.sinDatos()) {
            lineas.add("No hay emisiones registradas en el periodo.");
        }
        lineas.add("");
        lineas.add("Desglose por categoria");
        lineas.add("Categoria                         kg CO2e        %");
        for (ReporteHuellaCategoriaDTO categoria : reporte.categorias()) {
            lineas.add(String.format(
                    Locale.ROOT,
                    "%-30s %12s %8s",
                    etiquetaCategoria(categoria.categoria()),
                    formato(categoria.carbonKg(), 3),
                    formato(categoria.porcentaje(), 1) + "%"
            ));
        }
        lineas.add("");
        lineas.add("Comparacion contra el limite");
        ReporteHuellaComparacionDTO comparacion = reporte.comparacion();
        if (comparacion.tieneLimite()) {
            lineas.add("Acumulado: " + formato(comparacion.acumuladoT(), 4) + " t CO2e");
            lineas.add("Limite: " + formato(comparacion.limiteT(), 4) + " t CO2e");
            lineas.add("Consumido: " + formato(comparacion.porcentajeConsumido(), 1) + "%");
            lineas.add("Estado: " + comparacion.estado());
        } else {
            lineas.add("Sin limite declarado.");
        }
        lineas.add("");
        lineas.add("Generado: " + FECHA_HORA.format(reporte.generadoEn()));

        return escribirPdf(lineas);
    }

    private byte[] escribirPdf(List<String> lineas) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("BT\n/F2 18 Tf\n50 750 Td\n");
        for (int i = 0; i < lineas.size(); i++) {
            if (i == 1) {
                contenido.append("/F1 11 Tf\n14 TL\n");
            }
            contenido.append("(").append(escapar(lineas.get(i))).append(") Tj\nT*\n");
        }
        contenido.append("ET\n");

        byte[] stream = contenido.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objetos = List.of(
                bytes("<< /Type /Catalog /Pages 2 0 R >>"),
                bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                        + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"),
                bytes("<< /Length " + stream.length + " >>\nstream\n"
                        + new String(stream, StandardCharsets.ISO_8859_1) + "endstream")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        escribir(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(out.size());
            escribir(out, (i + 1) + " 0 obj\n");
            escribir(out, objetos.get(i));
            escribir(out, "\nendobj\n");
        }
        int xref = out.size();
        escribir(out, "xref\n0 " + (objetos.size() + 1) + "\n");
        escribir(out, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            escribir(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        escribir(out, "trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\n");
        escribir(out, "startxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    private String periodo(Integer anio, Integer mes) {
        return mes == null ? "Anual " + anio : String.format(Locale.ROOT, "%02d/%d", mes, anio);
    }

    private String etiquetaCategoria(Object categoria) {
        return String.valueOf(categoria).replace('_', ' ');
    }

    private String formato(BigDecimal valor, int escala) {
        return valor.setScale(escala, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escapar(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    private byte[] bytes(String valor) {
        return valor.getBytes(StandardCharsets.ISO_8859_1);
    }

    private void escribir(ByteArrayOutputStream out, String valor) {
        escribir(out, bytes(valor));
    }

    private void escribir(ByteArrayOutputStream out, byte[] bytes) {
        out.writeBytes(bytes);
    }
}
