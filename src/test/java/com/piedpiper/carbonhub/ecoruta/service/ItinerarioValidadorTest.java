package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItinerarioValidadorTest {

    private final ItinerarioValidador validador = new ItinerarioValidador();

    private ActividadIaDTO actividadValida() {
        return new ActividadIaDTO(
                "Caminata por puentes colgantes", "Recorrido guiado", "09:00", 150,
                new BigDecimal("13000"), "CRC", "Reserva Selvatura", "PUNTARENAS", 75);
    }

    private DiaIaDTO diaValido(int numeroDia) {
        return new DiaIaDTO(numeroDia, "2026-08-0" + numeroDia, List.of(actividadValida()));
    }

    @Test
    void respuestaConTodosLosDiasEsValidaCompleta() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(2), diaValido(3)), 82);

        assertThat(validador.validar(respuesta, 3)).isEqualTo(ResultadoValidacionItinerario.VALIDO_COMPLETO);
    }

    @Test
    void respuestaConMenosDiasDeLosSolicitadosEsParcial() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(2)), 82);

        assertThat(validador.validar(respuesta, 5)).isEqualTo(ResultadoValidacionItinerario.VALIDO_PARCIAL);
    }

    @Test
    void respuestaNulaEsInvalida() {
        assertThat(validador.validar(null, 3)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void respuestaSinDiasEsInvalida() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(), 82);

        assertThat(validador.validar(respuesta, 3)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void masDiasDeLosSolicitadosEsInvalida() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(2), diaValido(3)), 82);

        assertThat(validador.validar(respuesta, 2)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void diaSinActividadesEsInvalida() {
        DiaIaDTO diaVacio = new DiaIaDTO(1, "2026-08-01", List.of());
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(diaVacio), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void numeroDeDiaDuplicadoEsInvalida() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(1)), 82);

        assertThat(validador.validar(respuesta, 2)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void provinciaFueraDeCostaRicaEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", null, "09:00", 60, null, null, null, "FLORIDA", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void horarioMalFormadoEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", null, "no es una hora", 60, null, null, null, "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void costoSinMonedaEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", null, "09:00", 60, new BigDecimal("5000"), null, null, "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void puntuacionAmbientalFueraDeRangoEsInvalida() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(diaValido(1)), 9000);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void puntuacionAmbientalNulaEsValida() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(diaValido(1)), null);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.VALIDO_COMPLETO);
    }

    @Test
    void actividadSinCostoNiMonedaEsValida() {
        ActividadIaDTO actividadGratuita = new ActividadIaDTO(
                "Playa pública", null, "09:00", 120, null, null, null, "GUANACASTE", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadGratuita));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.VALIDO_COMPLETO);
    }

    @Test
    void nombreQueExcedeElLimiteDeColumnaEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "N".repeat(201), null, "09:00", 60, null, null, null, "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void descripcionQueExcedeElLimiteDeColumnaEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", "D".repeat(501), "09:00", 60, null, null, null, "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void establecimientoQueExcedeElLimiteDeColumnaEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", null, "09:00", 60, null, null, "E".repeat(201), "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void costoAproximadoNegativoEsInvalida() {
        ActividadIaDTO actividadInvalida = new ActividadIaDTO(
                "Tour", null, "09:00", 60, new BigDecimal("-1"), "CRC", null, "LIMON", null);
        DiaIaDTO dia = new DiaIaDTO(1, "2026-08-01", List.of(actividadInvalida));
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(dia), 82);

        assertThat(validador.validar(respuesta, 1)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void diasParcialesContiguosDesdeElDiaUnoSonValidos() {
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(2)), 82);

        assertThat(validador.validar(respuesta, 3)).isEqualTo(ResultadoValidacionItinerario.VALIDO_PARCIAL);
    }

    @Test
    void diasParcialesNoContiguosDesdeElDiaUnoSonInvalidos() {
        // Solo el día 3 de un viaje de 3 días dejaría huecos sin actividades en los días 1 y 2.
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(List.of(diaValido(3)), 82);

        assertThat(validador.validar(respuesta, 3)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }

    @Test
    void diasParcialesConHuecoIntermedioSonInvalidos() {
        // Días 1 y 3 de un viaje de 3 días dejarían el día 2 sin actividades.
        ItinerarioIaResponseDTO respuesta = new ItinerarioIaResponseDTO(
                List.of(diaValido(1), diaValido(3)), 82);

        assertThat(validador.validar(respuesta, 3)).isEqualTo(ResultadoValidacionItinerario.INVALIDO);
    }
}
