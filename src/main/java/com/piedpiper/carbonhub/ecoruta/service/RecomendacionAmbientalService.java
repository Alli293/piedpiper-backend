package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionesResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Genera recomendaciones para mejorar el EcoScore de un itinerario ya calculado (PP-93).
 * Depende de PP-91 (EcoScore, {@link EcoScoreService}) para saber si vale la pena recomendar algo,
 * y de PP-92 ({@link AlternativasIaClienteService}, {@link ComparacionAlternativasService}) para
 * encontrar y aplicar las alternativas concretas: este servicio no llama a Gemini ni sustituye
 * actividades directamente, reutiliza esos colaboradores para no duplicar esa lógica.
 */
@Service
public class RecomendacionAmbientalService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionAmbientalService.class);

    /** Debajo de este puntaje una actividad se considera candidata a recomendación (rango BUENA de PP-91). */
    static final int UMBRAL_PUNTUACION_MEJORABLE = 60;
    static final int ECO_SCORE_DEFAULT = 50;
    static final int MAX_RECOMENDACIONES = 3;

    /** Mismo peso de "factor_actividad" usado por {@link EcoScoreService#calcular}, para estimar
     *  cuánto subiría el EcoScore del itinerario al aplicar una recomendación. */
    private static final BigDecimal W_ACTIVIDAD = new BigDecimal("0.20");

    static final String MENSAJE_ITINERARIO_OPTIMIZADO = "Tu itinerario ya presenta un excelente desempeño ambiental.";

    /**
     * Se usa cuando no hay actividades por debajo del umbral mejorable pero el itinerario NO está
     * en la banda EXCELENTE (p. ej. el EcoScore quedó en Moderada por el componente IMA/indicadores
     * a nivel de establecimiento, no por las actividades). {@code MENSAJE_ITINERARIO_OPTIMIZADO} es
     * una afirmación específica de esa banda y no debe reutilizarse para este caso: decirle a un
     * usuario con EcoScore Moderado que su itinerario es "excelente" es engañoso.
     */
    static final String MENSAJE_SIN_ACTIVIDADES_MEJORABLES =
            "No encontramos actividades específicas que sustituir para mejorar tu EcoScore en este momento.";

    private final ItinerarioRepository itinerarioRepository;
    private final ComparacionAlternativasService comparacionAlternativasService;
    private final AlternativasIaClienteService alternativasIaClienteService;

    public RecomendacionAmbientalService(ItinerarioRepository itinerarioRepository,
                                          ComparacionAlternativasService comparacionAlternativasService,
                                          AlternativasIaClienteService alternativasIaClienteService) {
        this.itinerarioRepository = itinerarioRepository;
        this.comparacionAlternativasService = comparacionAlternativasService;
        this.alternativasIaClienteService = alternativasIaClienteService;
    }

    /**
     * Analiza el EcoScore del itinerario (Req PP-91) e identifica oportunidades de mejora.
     * {@code mensaje} solo viene con la afirmación "excelente desempeño" cuando la clasificación
     * realmente es EXCELENTE; cualquier otro caso sin recomendaciones usa un texto neutro
     * ({@link #MENSAJE_SIN_ACTIVIDADES_MEJORABLES}) para no contradecir el EcoScore mostrado.
     *
     * @throws ApiException {@code accesoDenegado()} si el itinerario no existe o no pertenece a {@code usuarioId};
     *                      {@code ecoScoreNoDisponible()} si aún no se calculó un EcoScore para el itinerario.
     */
    @Transactional(readOnly = true)
    public RecomendacionesResponseDTO obtenerRecomendaciones(UUID itinerarioId, UUID usuarioId) {
        Itinerario itinerario = buscarItinerarioPropio(itinerarioId, usuarioId);

        if (itinerario.getEcoScore() == null) {
            throw ApiException.ecoScoreNoDisponible();
        }

        if (itinerario.getClasificacionAmbiental() == ClasificacionAmbiental.EXCELENTE) {
            return new RecomendacionesResponseDTO(List.of(), MENSAJE_ITINERARIO_OPTIMIZADO);
        }

        List<ItinerarioActividad> actividades = recopilarActividades(itinerario);
        List<String> nombresExcluidos = actividades.stream().map(ItinerarioActividad::getNombre).toList();

        List<RecomendacionAmbientalDTO> recomendaciones = actividades.stream()
                .filter(this::esMejorable)
                .sorted(Comparator.comparingInt(this::puntuacionOrDefault))
                .map(actividad -> generarRecomendacion(actividad, nombresExcluidos, actividades.size()))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(RecomendacionAmbientalDTO::getIncrementoEstimado).reversed())
                .limit(MAX_RECOMENDACIONES)
                .toList();

        if (recomendaciones.isEmpty()) {
            return new RecomendacionesResponseDTO(List.of(), MENSAJE_SIN_ACTIVIDADES_MEJORABLES);
        }

        return new RecomendacionesResponseDTO(new ArrayList<>(recomendaciones), null);
    }

    /**
     * Aplica una recomendación de tipo actividad alternativa. Delega íntegramente en
     * {@link ComparacionAlternativasService#sustituirActividad} (PP-92): la validación de
     * pertenencia y de equivalencia categoría/provincia ya vive ahí, no se duplica aquí.
     */
    @Transactional
    public ItinerarioResponseDTO aplicarRecomendacion(UUID itinerarioId, UUID actividadId,
                                                       SustitucionRequestDTO request, UUID usuarioId) {
        return comparacionAlternativasService.sustituirActividad(itinerarioId, actividadId, request, usuarioId);
    }

    private Itinerario buscarItinerarioPropio(UUID itinerarioId, UUID usuarioId) {
        return itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No tienes permiso para acceder a este itinerario."));
    }

    private List<ItinerarioActividad> recopilarActividades(Itinerario itinerario) {
        List<ItinerarioActividad> actividades = new ArrayList<>();
        for (ItinerarioDia dia : itinerario.getDias()) {
            actividades.addAll(dia.getActividades());
        }
        return actividades;
    }

    private boolean esMejorable(ItinerarioActividad actividad) {
        return puntuacionOrDefault(actividad) < UMBRAL_PUNTUACION_MEJORABLE;
    }

    private int puntuacionOrDefault(ItinerarioActividad actividad) {
        return actividad.getPuntuacionAmbientalEstimada() != null
                ? actividad.getPuntuacionAmbientalEstimada()
                : ECO_SCORE_DEFAULT;
    }

    /**
     * Busca la mejor alternativa disponible para {@code actividad} y arma la recomendación
     * correspondiente. Retorna {@code null} (sin lanzar) si la IA no devuelve alternativas o
     * falla: una recomendación individual que no se puede generar no debe tumbar el resto de
     * la lista (misma degradación graciosa que aplica el resto del dominio {@code ecoruta}).
     */
    private RecomendacionAmbientalDTO generarRecomendacion(ItinerarioActividad actividad,
                                                            List<String> nombresExcluidos,
                                                            int totalActividades) {
        List<AlternativaIaDTO> alternativas;
        try {
            alternativas = alternativasIaClienteService.buscarAlternativas(actividad, nombresExcluidos);
        } catch (ApiException e) {
            log.warn("No se pudo obtener alternativas para la actividad {} al generar recomendaciones: {}",
                    actividad.getId(), e.getMessage());
            return null;
        }

        if (alternativas == null || alternativas.isEmpty()) {
            return null;
        }

        int puntuacionActual = puntuacionOrDefault(actividad);
        AlternativaIaDTO mejor = alternativas.stream()
                .max(Comparator.comparingInt(this::puntuacionAlternativaOrDefault))
                .orElse(null);

        if (mejor == null) {
            return null;
        }

        int puntuacionMejor = puntuacionAlternativaOrDefault(mejor);
        int diferencia = puntuacionMejor - puntuacionActual;
        if (diferencia <= 0) {
            return null;
        }

        BigDecimal incrementoEstimado = W_ACTIVIDAD
                .multiply(BigDecimal.valueOf(diferencia))
                .divide(BigDecimal.valueOf(totalActividades), 1, RoundingMode.HALF_UP);

        AlternativaDTO alternativaDto = new AlternativaDTO();
        alternativaDto.setNombre(mejor.getNombre());
        alternativaDto.setDescripcion(mejor.getDescripcion());
        alternativaDto.setEcoScore(puntuacionMejor);
        alternativaDto.setCostoAproximado(mejor.getCostoAproximado());
        alternativaDto.setMoneda(mejor.getMoneda());
        alternativaDto.setEstablecimientoRecomendado(mejor.getEstablecimientoRecomendado());
        alternativaDto.setDiferenciaAmbiental(diferencia);
        alternativaDto.setMejorDesempeno(true);

        String categoriaTuristica = actividad.getCategoriaTuristica() != null
                ? actividad.getCategoriaTuristica().name() : null;
        String provincia = actividad.getProvincia() != null ? actividad.getProvincia().name() : null;

        String descripcion = "Sustituye \"" + actividad.getNombre() + "\" por \"" + mejor.getNombre()
                + "\" para mejorar el desempeño ambiental de tu itinerario.";

        return new RecomendacionAmbientalDTO(
                "ACTIVIDAD_ALTERNATIVA",
                actividad.getId(),
                actividad.getNombre(),
                descripcion,
                incrementoEstimado,
                alternativaDto,
                categoriaTuristica,
                provincia
        );
    }

    private int puntuacionAlternativaOrDefault(AlternativaIaDTO alternativa) {
        return alternativa.getPuntuacionAmbientalEstimada() != null
                ? alternativa.getPuntuacionAmbientalEstimada()
                : ECO_SCORE_DEFAULT;
    }
}
