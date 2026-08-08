package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calcula el EcoScore de un itinerario completo (PP-91): una estimación de impacto ambiental
 * agregada a nivel de viaje, distinta de la puntuación ambiental por establecimiento de PP-86.
 *
 * Fórmula: EcoScore = round(IMA_prom * 0.50 + indicadores_prom * 0.30 + factor_actividad * 0.20, 1)
 *
 * Si algún componente no está disponible, su peso se redistribuye proporcionalmente entre los
 * componentes restantes (equivalente a un promedio ponderado sobre los componentes disponibles).
 * Si ningún componente está disponible, no es posible calcular el EcoScore.
 */
@Service
public class EcoScoreService {

    private static final Logger log = LoggerFactory.getLogger(EcoScoreService.class);

    private static final BigDecimal W_IMA = new BigDecimal("0.50");
    private static final BigDecimal W_INDICADORES = new BigDecimal("0.30");
    private static final BigDecimal W_ACTIVIDAD = new BigDecimal("0.20");

    private final ImaClient imaClient;
    private final IndicadorAmbientalClient indicadorClient;
    private final PuntuacionAmbientalCalculator calculator;

    public EcoScoreService(ImaClient imaClient,
                            IndicadorAmbientalClient indicadorClient,
                            PuntuacionAmbientalCalculator calculator) {
        this.imaClient = imaClient;
        this.indicadorClient = indicadorClient;
        this.calculator = calculator;
    }

    /**
     * Calcula el EcoScore del itinerario a partir de los establecimientos ya vinculados a empresas
     * registradas y de las actividades del itinerario. Retorna {@code null} si no hay ningún
     * componente disponible (ni IMA, ni indicadores, ni estimación por actividad).
     */
    public EcoScoreResultado calcular(List<EstablecimientoRankeado> establecimientos, Itinerario itinerario) {
        List<UUID> empresaIds = establecimientos.stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, IMADTO> imaMap = consultarImaSafe(empresaIds);
        Map<UUID, IndicadorAmbientalDTO> indicadorMap = consultarIndicadoresSafe(empresaIds);

        BigDecimal imaProm = promedioIma(empresaIds, imaMap);
        BigDecimal indicadoresProm = promedioIndicadores(empresaIds, indicadorMap);
        BigDecimal factorActividad = promedioFactorActividad(itinerario);

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;
        int componentesDisponibles = 0;

        if (imaProm != null) {
            sumaPonderada = sumaPonderada.add(imaProm.multiply(W_IMA));
            sumaPesos = sumaPesos.add(W_IMA);
            componentesDisponibles++;
        }
        if (indicadoresProm != null) {
            sumaPonderada = sumaPonderada.add(indicadoresProm.multiply(W_INDICADORES));
            sumaPesos = sumaPesos.add(W_INDICADORES);
            componentesDisponibles++;
        }
        if (factorActividad != null) {
            sumaPonderada = sumaPonderada.add(factorActividad.multiply(W_ACTIVIDAD));
            sumaPesos = sumaPesos.add(W_ACTIVIDAD);
            componentesDisponibles++;
        }

        if (componentesDisponibles == 0) {
            return null;
        }

        BigDecimal ecoScore = sumaPonderada.divide(sumaPesos, 1, RoundingMode.HALF_UP);
        ClasificacionAmbiental clasificacion = ClasificacionAmbiental.porPuntaje(ecoScore);
        boolean parcial = componentesDisponibles < 3;

        return new EcoScoreResultado(ecoScore, clasificacion, parcial);
    }

    private BigDecimal promedioIma(List<UUID> empresaIds, Map<UUID, IMADTO> imaMap) {
        List<BigDecimal> scores = empresaIds.stream()
                .map(imaMap::get)
                .filter(java.util.Objects::nonNull)
                .map(calculator::calcularScoreIma)
                .toList();
        return promedio(scores);
    }

    private BigDecimal promedioIndicadores(List<UUID> empresaIds, Map<UUID, IndicadorAmbientalDTO> indicadorMap) {
        List<BigDecimal> scores = empresaIds.stream()
                .map(indicadorMap::get)
                .filter(java.util.Objects::nonNull)
                .map(calculator::calcularScoreCertificaciones)
                .toList();
        return promedio(scores);
    }

    private BigDecimal promedioFactorActividad(Itinerario itinerario) {
        List<BigDecimal> puntuaciones = itinerario.getDias().stream()
                .flatMap(dia -> dia.getActividades().stream())
                .map(actividad -> actividad.getPuntuacionAmbientalEstimada())
                .filter(java.util.Objects::nonNull)
                .map(BigDecimal::valueOf)
                .toList();
        return promedio(puntuaciones);
    }

    private BigDecimal promedio(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 10, RoundingMode.HALF_UP);
    }

    private Map<UUID, IMADTO> consultarImaSafe(List<UUID> empresaIds) {
        try {
            return imaClient.consultarIma(empresaIds);
        } catch (Exception e) {
            log.warn("Fallo en consulta de IMA para EcoScore. Continuando sin IMA.", e);
            return Map.of();
        }
    }

    private Map<UUID, IndicadorAmbientalDTO> consultarIndicadoresSafe(List<UUID> empresaIds) {
        try {
            return indicadorClient.consultarIndicadores(empresaIds);
        } catch (Exception e) {
            log.warn("Fallo en consulta de indicadores ambientales para EcoScore. Continuando sin indicadores.", e);
            return Map.of();
        }
    }
}
