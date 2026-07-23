package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ReporteHuellaPdfGenerator {

    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", new Locale("es", "CR"));

    private static final Color GREEN = new Color(31, 138, 91);
    private static final Color GREEN_DARK = new Color(20, 96, 63);
    private static final Color GREEN_SOFT = new Color(230, 244, 236);
    private static final Color SKY = new Color(79, 193, 233);
    private static final Color BLUE = new Color(43, 166, 222);
    private static final Color WARNING = new Color(230, 168, 23);
    private static final Color WARNING_SOFT = new Color(253, 241, 214);
    private static final Color DANGER = new Color(204, 68, 68);
    private static final Color DANGER_SOFT = new Color(253, 232, 232);
    private static final Color INK = new Color(14, 42, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LIGHT_TEXT = new Color(138, 155, 174);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color PAGE_BG = new Color(240, 242, 245);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color DARK_CARD = new Color(17, 45, 66);

    public byte[] generar(ReporteHuellaPdfDTO reporte) {
        PdfCanvas canvas = new PdfCanvas();
        canvas.fill(PAGE_BG, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);

        dibujarHeader(canvas, reporte);
        dibujarMetricas(canvas, reporte);
        dibujarComparacion(canvas, reporte);
        dibujarDesglose(canvas, reporte);
        dibujarFooter(canvas, reporte);

        return escribirPdf(canvas.content());
    }

    private void dibujarHeader(PdfCanvas canvas, ReporteHuellaPdfDTO reporte) {
        canvas.fill(WHITE, 0, 730, PAGE_WIDTH, 62);
        canvas.stroke(BORDER, 0.8, 0, 730, PAGE_WIDTH, 0);

        canvas.circle(GREEN, 42, 761, 16);
        canvas.text("CH", "F2", 9, WHITE, 34, 758);
        canvas.text("Carbon", "F2", 17, INK, 66, 764);
        canvas.text("Hub", "F2", 17, GREEN, 124, 764);
        canvas.text("PANEL EMPRESARIAL", "F3", 8, LIGHT_TEXT, 66, 749);

        canvas.text("Reporte de huella de carbono", "F2", 20, INK, 42, 695);
        canvas.text(limpiar(reporte.empresa()), "F1", 11, MUTED, 42, 677);
        canvas.text("Período: " + periodo(reporte.anio(), reporte.mes()), "F3", 9, MUTED, 42, 660);

        canvas.text("Generado", "F3", 8, LIGHT_TEXT, 438, 695);
        canvas.text(FECHA_HORA.format(reporte.generadoEn()), "F1", 10, INK, 438, 679);
    }

    private void dibujarMetricas(PdfCanvas canvas, ReporteHuellaPdfDTO reporte) {
        card(canvas, 42, 560, 252, 86);
        boolean mensual = reporte.mes() != null;
        canvas.text(mensual ? "HUELLA DEL MES" : "HUELLA ACUMULADA", "F3", 8, LIGHT_TEXT, 58, 622);
        canvas.text(formato(reporte.totalT(), 4), "F2", 28, SKY, 58, 594);
        canvas.text(mensual ? "t CO2e del mes" : "t CO2e acumuladas", "F3", 9, MUTED, 58, 577);

        card(canvas, 318, 560, 252, 86);
        canvas.text("TOTAL DEL PERÍODO", "F3", 8, LIGHT_TEXT, 334, 622);
        canvas.text(formato(reporte.totalKg(), 3), "F2", 24, GREEN, 334, 596);
        canvas.text("kg CO2e registrados", "F3", 9, MUTED, 334, 577);

        if (reporte.sinDatos()) {
            canvas.fill(GREEN_SOFT, 42, 526, 528, 22);
            canvas.text("No hay emisiones registradas en el período.", "F2", 9, GREEN_DARK, 54, 533);
        }
    }

    private void dibujarComparacion(PdfCanvas canvas, ReporteHuellaPdfDTO reporte) {
        ReporteHuellaComparacionDTO comparacion = reporte.comparacion();
        card(canvas, 42, 368, 528, 132);
        canvas.text("Límite anual de emisiones", "F2", 14, INK, 58, 474);

        EstadoVisual estado = estadoVisual(comparacion);
        badge(canvas, estado, 430, 466, 118, 22);

        if (!comparacion.tieneLimite()) {
            canvas.fill(new Color(248, 250, 252), 58, 414, 496, 34);
            canvas.stroke(BORDER, 0.8, 58, 414, 496, 34);
            canvas.text("Sin límite declarado", "F2", 11, INK, 72, 435);
            canvas.text("Declara un límite anual para ver el porcentaje consumido.", "F1", 9, MUTED, 72, 421);
            return;
        }

        BigDecimal porcentaje = comparacion.porcentajeConsumido() == null
                ? BigDecimal.ZERO
                : comparacion.porcentajeConsumido();
        double progreso = porcentaje
                .min(new BigDecimal("100"))
                .max(BigDecimal.ZERO)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                .doubleValue();

        canvas.fill(new Color(240, 242, 245), 58, 436, 496, 14);
        canvas.fill(estado.color(), 58, 436, 496 * progreso, 14);
        canvas.stroke(BORDER, 0.6, 58, 436, 496, 14);

        canvas.text("0 t CO2e", "F3", 8, MUTED, 58, 418);
        canvas.text(
                formato(comparacion.acumuladoT(), 4) + " t CO2e  -  "
                        + formato(porcentaje, 1) + "%",
                "F2",
                10,
                estado.color(),
                236,
                418
        );
        canvas.text(formato(comparacion.limiteT(), 4) + " t CO2e", "F3", 8, MUTED, 494, 418);

        canvas.text("Acumulado", "F3", 8, LIGHT_TEXT, 58, 392);
        canvas.text(formato(comparacion.acumuladoT(), 4) + " t", "F2", 13, INK, 58, 377);
        canvas.text("Límite", "F3", 8, LIGHT_TEXT, 208, 392);
        canvas.text(formato(comparacion.limiteT(), 4) + " t", "F2", 13, INK, 208, 377);
        canvas.text("Consumido", "F3", 8, LIGHT_TEXT, 358, 392);
        canvas.text(formato(porcentaje, 1) + "%", "F2", 13, INK, 358, 377);
    }

    private void dibujarDesglose(PdfCanvas canvas, ReporteHuellaPdfDTO reporte) {
        card(canvas, 42, 138, 528, 204);
        canvas.text("Desglose por categoría", "F2", 14, INK, 58, 316);
        canvas.text("kg CO2e y participación sobre el total del período", "F1", 9, MUTED, 58, 301);

        double headerY = 276;
        canvas.fill(new Color(248, 250, 252), 58, headerY, 496, 24);
        canvas.text("Categoría", "F3", 8, MUTED, 72, headerY + 8);
        canvas.text("kg CO2e", "F3", 8, MUTED, 356, headerY + 8);
        canvas.text("%", "F3", 8, MUTED, 506, headerY + 8);

        double y = 246;
        for (ReporteHuellaCategoriaDTO categoria : reporte.categorias()) {
            canvas.stroke(BORDER, 0.5, 58, y - 6, 496, 0);
            canvas.fill(colorCategoria(categoria.categoria()), 72, y + 2, 8, 8);
            canvas.text(etiquetaCategoria(categoria.categoria()), "F1", 10, INK, 88, y);
            canvas.text(formato(categoria.carbonKg(), 3), "F3", 10, INK, 356, y);
            canvas.text(formato(categoria.porcentaje(), 1) + "%", "F3", 10, INK, 506, y);
            y -= 28;
        }
    }

    private void dibujarFooter(PdfCanvas canvas, ReporteHuellaPdfDTO reporte) {
        canvas.stroke(BORDER, 0.7, 42, 104, 528, 0);
        canvas.text("CarbonHub", "F2", 10, GREEN, 42, 82);
        canvas.text(
                "Reporte generado automáticamente para " + limpiar(reporte.empresa()) + ".",
                "F1",
                8,
                MUTED,
                42,
                68
        );
        canvas.text("Archivo: " + ReporteHuellaPdfNombreArchivo.generar(reporte.anio(), reporte.mes()),
                "F3", 8, LIGHT_TEXT, 406, 68);
    }

    private void card(PdfCanvas canvas, double x, double y, double width, double height) {
        canvas.fill(WHITE, x, y, width, height);
        canvas.stroke(BORDER, 0.8, x, y, width, height);
    }

    private void badge(PdfCanvas canvas, EstadoVisual estado, double x, double y, double width, double height) {
        canvas.fill(estado.background(), x, y, width, height);
        canvas.circle(estado.color(), x + 14, y + 11, 3);
        canvas.text(estado.label(), "F3", 8, estado.color(), x + 24, y + 8);
    }

    private EstadoVisual estadoVisual(ReporteHuellaComparacionDTO comparacion) {
        if (!comparacion.tieneLimite()) {
            return new EstadoVisual("SIN LÍMITE", BLUE, new Color(226, 243, 252));
        }
        return switch (comparacion.estado()) {
            case "superado" -> new EstadoVisual("LÍMITE SUPERADO", DANGER, DANGER_SOFT);
            case "cerca" -> new EstadoVisual("CERCA DEL LÍMITE", WARNING, WARNING_SOFT);
            default -> new EstadoVisual("EN META", GREEN, GREEN_SOFT);
        };
    }

    private Color colorCategoria(CategoriaEmision categoria) {
        return switch (categoria) {
            case ELECTRICIDAD -> GREEN;
            case FLOTA -> BLUE;
            case VUELO -> DARK_CARD;
            case ENVIO -> WARNING;
        };
    }

    private String periodo(Integer anio, Integer mes) {
        return mes == null ? "Anual " + anio : String.format(Locale.ROOT, "%02d/%d", mes, anio);
    }

    private String etiquetaCategoria(CategoriaEmision categoria) {
        return switch (categoria) {
            case ELECTRICIDAD -> "Electricidad";
            case FLOTA -> "Flota vehicular";
            case VUELO -> "Vuelos";
            case ENVIO -> "Envíos";
        };
    }

    private String formato(BigDecimal valor, int escala) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("es", "CR"));
        DecimalFormat format = new DecimalFormat("#,##0." + "0".repeat(escala), symbols);
        return format.format(valor.setScale(escala, RoundingMode.HALF_UP));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? "Empresa sin nombre" : valor;
    }

    private byte[] escribirPdf(String contenido) {
        byte[] stream = contenido.getBytes(PDF_CHARSET);
        List<byte[]> objetos = List.of(
                bytes("<< /Type /Catalog /Pages 2 0 R >>"),
                bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                        + "/Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>"),
                bytes("<< /Length " + stream.length + " >>\nstream\n"
                        + new String(stream, PDF_CHARSET) + "endstream")
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

    private byte[] bytes(String valor) {
        return valor.getBytes(PDF_CHARSET);
    }

    private void escribir(ByteArrayOutputStream out, String valor) {
        escribir(out, bytes(valor));
    }

    private void escribir(ByteArrayOutputStream out, byte[] bytes) {
        out.writeBytes(bytes);
    }

    private record Color(int r, int g, int b) {
        String pdf() {
            return String.format(Locale.ROOT, "%.4f %.4f %.4f", r / 255.0, g / 255.0, b / 255.0);
        }
    }

    private record EstadoVisual(String label, Color color, Color background) {
    }

    private static final class PdfCanvas {
        private final StringBuilder out = new StringBuilder();

        void fill(Color color, double x, double y, double width, double height) {
            out.append("q ")
                    .append(color.pdf())
                    .append(" rg ")
                    .append(number(x)).append(' ')
                    .append(number(y)).append(' ')
                    .append(number(width)).append(' ')
                    .append(number(height))
                    .append(" re f Q\n");
        }

        void stroke(Color color, double lineWidth, double x, double y, double width, double height) {
            out.append("q ")
                    .append(color.pdf())
                    .append(" RG ")
                    .append(number(lineWidth))
                    .append(" w ")
                    .append(number(x)).append(' ')
                    .append(number(y)).append(' ')
                    .append(number(width)).append(' ')
                    .append(number(height))
                    .append(" re S Q\n");
        }

        void circle(Color color, double centerX, double centerY, double radius) {
            double c = radius * 0.5522847498;
            out.append("q ")
                    .append(color.pdf())
                    .append(" rg ")
                    .append(number(centerX + radius)).append(' ').append(number(centerY)).append(" m ")
                    .append(number(centerX + radius)).append(' ').append(number(centerY + c)).append(' ')
                    .append(number(centerX + c)).append(' ').append(number(centerY + radius)).append(' ')
                    .append(number(centerX)).append(' ').append(number(centerY + radius)).append(" c ")
                    .append(number(centerX - c)).append(' ').append(number(centerY + radius)).append(' ')
                    .append(number(centerX - radius)).append(' ').append(number(centerY + c)).append(' ')
                    .append(number(centerX - radius)).append(' ').append(number(centerY)).append(" c ")
                    .append(number(centerX - radius)).append(' ').append(number(centerY - c)).append(' ')
                    .append(number(centerX - c)).append(' ').append(number(centerY - radius)).append(' ')
                    .append(number(centerX)).append(' ').append(number(centerY - radius)).append(" c ")
                    .append(number(centerX + c)).append(' ').append(number(centerY - radius)).append(' ')
                    .append(number(centerX + radius)).append(' ').append(number(centerY - c)).append(' ')
                    .append(number(centerX + radius)).append(' ').append(number(centerY)).append(" c f Q\n");
        }

        void text(String text, String font, int size, Color color, double x, double y) {
            out.append("BT /")
                    .append(font)
                    .append(' ')
                    .append(size)
                    .append(" Tf ")
                    .append(color.pdf())
                    .append(" rg ")
                    .append(number(x)).append(' ')
                    .append(number(y))
                    .append(" Td (")
                    .append(escape(text))
                    .append(") Tj ET\n");
        }

        String content() {
            return out.toString();
        }

        private String number(double value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private String escape(String value) {
            if (value != null) {
                String escaped = value
                        .replace("\\", "\\\\")
                        .replace("(", "\\(")
                        .replace(")", "\\)");

                StringBuilder seguro = new StringBuilder();
                for (int i = 0; i < escaped.length(); i++) {
                    char caracter = escaped.charAt(i);
                    if (caracter == '\n' || caracter == '\r' || caracter == '\t') {
                        seguro.append(' ');
                    } else if (caracter >= 0x20 && PDF_CHARSET.newEncoder().canEncode(caracter)) {
                        seguro.append(caracter);
                    }
                }
                return seguro.toString();
            }
            return "";
        }
    }
}
