package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Valida la respuesta cruda de Gemini antes de que se persista, per los criterios de aceptación de
 * PP-85: estructura correcta, destinos únicamente en Costa Rica, cada día con al menos una
 * actividad, campos obligatorios presentes. Si CUALQUIER regla dura falla, la respuesta completa se
 * descarta (INVALIDO) — no se mapean parcialmente días individualmente rotos. Solo se considera
 * PARCIAL cuando la respuesta es íntegramente válida pero cubre menos días de los solicitados (la
 * IA no encontró suficientes recomendaciones), nunca cuando algún día/actividad incluido es
 * inconsistente.
 */
@Component
public class ItinerarioValidador {

    public ResultadoValidacionItinerario validar(ItinerarioIaResponseDTO respuesta, int cantidadDiasSolicitados) {
        if (respuesta == null || respuesta.getDias() == null || respuesta.getDias().isEmpty()) {
            return ResultadoValidacionItinerario.INVALIDO;
        }
        Integer puntuacion = respuesta.getPuntuacionAmbientalPreliminar();
        if (puntuacion != null && (puntuacion < 0 || puntuacion > 100)) {
            return ResultadoValidacionItinerario.INVALIDO;
        }
        List<DiaIaDTO> dias = respuesta.getDias();
        if (dias.size() > cantidadDiasSolicitados) {
            return ResultadoValidacionItinerario.INVALIDO;
        }

        Set<Integer> numerosDeDiaVistos = new HashSet<>();
        for (DiaIaDTO dia : dias) {
            if (!diaEsValido(dia, cantidadDiasSolicitados, numerosDeDiaVistos)) {
                return ResultadoValidacionItinerario.INVALIDO;
            }
        }

        if (dias.size() < cantidadDiasSolicitados && !esPrefijoContiguo(numerosDeDiaVistos, dias.size())) {
            return ResultadoValidacionItinerario.INVALIDO;
        }

        return dias.size() == cantidadDiasSolicitados
                ? ResultadoValidacionItinerario.VALIDO_COMPLETO
                : ResultadoValidacionItinerario.VALIDO_PARCIAL;
    }

    /**
     * Un itinerario parcial solo tiene sentido como prefijo contiguo desde el día 1 (ej. días 1-2
     * de un viaje de 3) — de lo contrario quedarían huecos sin actividades en días intermedios
     * (ej. solo el día 3), lo cual el frontend no sabe representar y el usuario no puede completar.
     */
    private boolean esPrefijoContiguo(Set<Integer> numerosDeDiaVistos, int cantidadDias) {
        for (int dia = 1; dia <= cantidadDias; dia++) {
            if (!numerosDeDiaVistos.contains(dia)) {
                return false;
            }
        }
        return true;
    }

    private boolean diaEsValido(DiaIaDTO dia, int cantidadDiasSolicitados, Set<Integer> numerosDeDiaVistos) {
        if (dia == null || dia.getNumeroDia() == null
                || dia.getNumeroDia() < 1 || dia.getNumeroDia() > cantidadDiasSolicitados) {
            return false;
        }
        if (!numerosDeDiaVistos.add(dia.getNumeroDia())) {
            return false;
        }
        if (dia.getActividades() == null || dia.getActividades().isEmpty()) {
            return false;
        }
        return dia.getActividades().stream().allMatch(this::actividadEsValida);
    }

    // Deben coincidir con las columnas de ItinerarioActividad — una respuesta que las exceda
    // se descarta aquí como inválida, en vez de fallar más adelante con un error de persistencia.
    private static final int LONGITUD_MAXIMA_NOMBRE = 200;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;
    private static final int LONGITUD_MAXIMA_ESTABLECIMIENTO = 200;

    private boolean actividadEsValida(ActividadIaDTO actividad) {
        if (actividad == null
                || actividad.getNombre() == null || actividad.getNombre().isBlank()
                || actividad.getNombre().length() > LONGITUD_MAXIMA_NOMBRE
                || actividad.getDuracionMinutos() == null || actividad.getDuracionMinutos() <= 0) {
            return false;
        }
        if (actividad.getDescripcion() != null
                && actividad.getDescripcion().length() > LONGITUD_MAXIMA_DESCRIPCION) {
            return false;
        }
        if (actividad.getEstablecimientoRecomendado() != null
                && actividad.getEstablecimientoRecomendado().length() > LONGITUD_MAXIMA_ESTABLECIMIENTO) {
            return false;
        }
        if (!horarioEsValido(actividad.getHorario())) {
            return false;
        }
        if (Catalogos.desde(Provincia.class, actividad.getProvincia()).isEmpty()) {
            return false;
        }
        if (actividad.getCostoAproximado() != null) {
            if (actividad.getCostoAproximado().signum() < 0) {
                return false;
            }
            if (Catalogos.desde(Moneda.class, actividad.getMoneda()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean horarioEsValido(String horario) {
        if (horario == null || horario.isBlank()) {
            return false;
        }
        try {
            LocalTime.parse(horario);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}
