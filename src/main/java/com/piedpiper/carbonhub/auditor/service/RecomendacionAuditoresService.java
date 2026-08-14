package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorRecomendadoResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendacionAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresConsultaService.Seleccion;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Recomendación de auditores (PP-57).
 *
 * <p>La selección y el orden son <b>deterministas</b> y los decide
 * {@link RecomendacionAuditoresConsultaService}; la IA solo redacta la justificación de una decisión
 * ya tomada. Esa separación es intencional: si el orden dependiera del modelo, la misma consulta
 * daría resultados distintos entre llamadas y no habría forma de probarla.</p>
 *
 * <p>El sector y la empresa salen del usuario autenticado, nunca del cuerpo de la petición, así que
 * nadie puede pedir recomendaciones haciéndose pasar por otra empresa.</p>
 */
@Service
public class RecomendacionAuditoresService {

    private final RecomendacionAuditoresConsultaService consultaService;
    private final RecomendacionAuditoresIaService recomendacionAuditoresIaService;

    public RecomendacionAuditoresService(
            RecomendacionAuditoresConsultaService consultaService,
            RecomendacionAuditoresIaService recomendacionAuditoresIaService) {
        this.consultaService = consultaService;
        this.recomendacionAuditoresIaService = recomendacionAuditoresIaService;
    }

    /**
     * <b>Sin {@code @Transactional} a propósito.</b> La llamada a Gemini tarda segundos y puede
     * colgarse hasta el timeout; hacerla dentro de una transacción retiene una conexión del pool
     * todo ese rato, y con varios usuarios a la vez el pool se agota y se cae todo lo que necesita
     * base, no solo esta pantalla. Es el incidente que el equipo ya vivió con el dashboard y que
     * documenta {@code docs/CONVENTIONS.md} §4.5.
     *
     * <p>Por eso la parte que toca la base vive en {@link RecomendacionAuditoresConsultaService},
     * que devuelve datos ya planos, y la IA se invoca después, con la transacción cerrada. Mismo
     * reparto que {@code DashboardRecomendacionService} entre consulta e IA.</p>
     */
    public RecomendacionAuditoresResponseDTO recomendar(RecomendarAuditoresRequestDTO filtros,
                                                        UUID usuarioId) {
        Seleccion seleccion = consultaService.seleccionar(filtros, usuarioId);

        // Sin candidatos no se llama a la IA: no hay a quién justificar. La bandera queda en true
        // porque el aviso de "IA no disponible" solo aplica cuando la IA falló, no cuando no se usó.
        if (seleccion.recomendados().isEmpty()) {
            return new RecomendacionAuditoresResponseDTO(List.of(), true);
        }

        Optional<Map<UUID, String>> justificaciones = recomendacionAuditoresIaService.generarJustificaciones(
                seleccion.empresaId(),
                seleccion.sector(),
                seleccion.tipoAuditoria(),
                seleccion.zona(),
                seleccion.paraIa());

        Map<UUID, String> porAuditor = justificaciones.orElse(Map.of());
        List<AuditorRecomendadoResponseDTO> recomendaciones = seleccion.recomendados().stream()
                .map(recomendado -> conJustificacion(recomendado, porAuditor.get(recomendado.getAuditorId())))
                .toList();

        return new RecomendacionAuditoresResponseDTO(recomendaciones, justificaciones.isPresent());
    }

    /** Copia la tarjeta agregándole la justificación, para no mutar lo que devolvió la consulta. */
    private AuditorRecomendadoResponseDTO conJustificacion(AuditorRecomendadoResponseDTO base,
                                                          String justificacion) {
        return new AuditorRecomendadoResponseDTO(
                base.getAuditorId(),
                base.getNombre(),
                base.getFotoPerfil(),
                base.getEspecialidades(),
                base.getCalificacionPromedio(),
                base.isDisponible(),
                base.getAuditoriasCompletadas(),
                justificacion);
    }
}
