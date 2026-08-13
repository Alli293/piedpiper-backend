package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del endpoint de recomendación.
 *
 * <p>{@code iaDisponible} es lo que le dice al frontend si mostrar el aviso de "justificaciones no
 * disponibles". Va aparte de mirar si las justificaciones vienen nulas porque son dos cosas
 * distintas: puede haber candidatos sin justificación por un fallo de IA (iaDisponible = false) y no
 * hay forma de distinguir eso de "la IA respondió pero sin texto" solo mirando las tarjetas.</p>
 *
 * <p>Cuando no hay candidatos, {@code recomendaciones} viene vacío e {@code iaDisponible} es true:
 * no se llamó a la IA porque no había a quién justificar, no porque fallara.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionAuditoresResponseDTO {

    private List<AuditorRecomendadoResponseDTO> recomendaciones;
    private boolean iaDisponible;
}
