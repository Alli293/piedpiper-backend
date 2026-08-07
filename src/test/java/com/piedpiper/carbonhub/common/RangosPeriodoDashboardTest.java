package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.dashboard.models.enums.PeriodoDashboard;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

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
}
