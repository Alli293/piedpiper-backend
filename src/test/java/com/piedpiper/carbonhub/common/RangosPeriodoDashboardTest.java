package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.dashboard.models.enums.PeriodoDashboard;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangosPeriodoDashboardTest {

    @Test
    void mesActualDevuelveElMesDeHoyDelAnioSolicitado() {
        LocalDate hoy = LocalDate.of(2026, Month.JULY, 15);

        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(PeriodoDashboard.MES_ACTUAL, 2026, hoy);

        assertThat(rango.inicio()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(rango.fin()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void trimestreUbicaElInicioDelTrimestreQueContieneAHoy() {
        LocalDate hoy = LocalDate.of(2026, Month.AUGUST, 6);

        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(PeriodoDashboard.TRIMESTRE, 2026, hoy);

        assertThat(rango.inicio()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(rango.fin()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void anioDevuelveElAnioCompletoSolicitado() {
        LocalDate hoy = LocalDate.of(2026, Month.AUGUST, 6);

        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(PeriodoDashboard.ANIO, 2026, hoy);

        assertThat(rango.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(rango.fin()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void anioSolicitadoDistintoDeHoySeRespetaEnElMesActual() {
        LocalDate hoy = LocalDate.of(2026, Month.AUGUST, 6);

        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(PeriodoDashboard.MES_ACTUAL, 2024, hoy);

        assertThat(rango.inicio()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(rango.fin()).isEqualTo(LocalDate.of(2024, 9, 1));
    }

    @Test
    void validarAnioAceptaElAnioActual() {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();

        assertThatCode(() -> RangosPeriodoDashboard.validarAnio(anioActual)).doesNotThrowAnyException();
    }

    @Test
    void validarAnioAceptaElAnioSiguienteAlActual() {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();

        assertThatCode(() -> RangosPeriodoDashboard.validarAnio(anioActual + 1)).doesNotThrowAnyException();
    }

    @Test
    void validarAnioRechazaUnAnioMuyLejanoEnElFuturo() {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();

        assertThatThrownBy(() -> RangosPeriodoDashboard.validarAnio(anioActual + 5))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void validarAnioRechazaUnAnioNegativo() {
        assertThatThrownBy(() -> RangosPeriodoDashboard.validarAnio(-1))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void validarAnioRechazaUnAnioExtremoSinLanzarDateTimeException() {
        // El caso concreto que motivo esta validacion: sin ella, un valor asi
        // llega directo a Year.of(...)/LocalDate.of(...) en actual() y explota
        // con DateTimeException, que no es una ApiException.
        assertThatThrownBy(() -> RangosPeriodoDashboard.validarAnio(999999999))
                .isInstanceOf(ApiException.class);
    }
}
