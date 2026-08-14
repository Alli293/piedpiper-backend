package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.dashboard.models.enums.PeriodoDashboard;
import com.piedpiper.carbonhub.exceptions.ApiException;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Traduce un {@link PeriodoDashboard} + año a un rango de fechas
 * {@code [inicio, fin)}, y valida que ese año sea razonable. Compartido
 * entre el resumen de huella del dashboard ({@code DashboardHuellaService},
 * PP-19/PP-61) y el cálculo de progreso de metas de reducción
 * ({@code MetaService}, PP-78): los dos necesitan la misma noción de "el
 * período actual" para que el selector de período afecte a ambos de forma
 * consistente, tal como pide el criterio de aceptación de PP-78 ("El
 * selector de periodo afecta el cálculo de progreso de las metas junto con
 * el bloque de resumen de huella").
 *
 * <p>Solo se extrajo la parte pura (fecha → rango, validación de año); el
 * resto de {@code DashboardHuellaService} (variación porcentual contra el
 * período anterior) sigue siendo específico de ese servicio.</p>
 */
public final class RangosPeriodoDashboard {

    private static final int ANIO_MINIMO = 1900;

    private RangosPeriodoDashboard() {
    }

    public record Rango(LocalDate inicio, LocalDate fin) {
    }

    /**
     * @throws ApiException {@code anioInvalido()} si el año está fuera de
     *                       un rango razonable — evita que un valor extremo
     *                       (negativo, con muchos dígitos) llegue a
     *                       {@link Year#of(int)}/{@link LocalDate#of(int, int, int)}
     *                       y explote con {@link java.time.DateTimeException},
     *                       que no es una {@link ApiException} y por lo
     *                       tanto no sigue el manejo de errores esperado.
     */
    public static void validarAnio(int anio) {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();
        if (anio < ANIO_MINIMO || anio > anioActual + 1) {
            throw ApiException.anioInvalido();
        }
    }

    public static Rango actual(PeriodoDashboard periodo, int anio, LocalDate hoy) {
        if (PeriodoDashboard.TRIMESTRE == periodo) {
            int mesInicial = (((hoy.getMonthValue() - 1) / 3) * 3) + 1;
            LocalDate inicio = LocalDate.of(anio, mesInicial, 1);
            return new Rango(inicio, inicio.plusMonths(3));
        }
        if (PeriodoDashboard.ANIO == periodo) {
            LocalDate inicio = Year.of(anio).atDay(1);
            return new Rango(inicio, inicio.plusYears(1));
        }

        LocalDate inicio = YearMonth.of(anio, hoy.getMonth()).atDay(1);
        return new Rango(inicio, inicio.plusMonths(1));
    }
}

