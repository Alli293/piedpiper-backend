package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
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
 * Cada componente se promedia solo sobre los elementos (establecimientos o actividades) que
 * tienen dato, y ese promedio no representa por sí solo a los elementos sin dato. Por eso el
 * peso del componente se descuenta por su cobertura (elementos con dato / elementos totales)
 * antes de combinarlo con los demás: un componente disponible para 1 de 4 establecimientos pesa
 * un cuarto de lo que pesaría si los 4 tuvieran dato. Un componente sin cobertura (0 elementos
 * con dato) queda fuera y su peso se redistribuye entre los componentes restantes.
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
     * Promedio de un componente junto con su cobertura: la proporción de elementos
     * (establecimientos o actividades) que efectivamente tenían dato disponible para calcularlo.
     */
    private record ComponenteDisponible(BigDecimal promedio, BigDecimal cobertura) {
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

        ComponenteDisponible ima = promedioIma(empresaIds, imaMap);
        ComponenteDisponible indicadores = promedioIndicadores(empresaIds, indicadorMap);
        ComponenteDisponible actividad = promedioFactorActividad(itinerario);

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;
        int componentesDisponibles = 0;
        boolean coberturaCompleta = true;

        if (ima != null) {
            BigDecimal peso = W_IMA.multiply(ima.cobertura());
            sumaPonderada = sumaPonderada.add(ima.promedio().multiply(peso));
            sumaPesos = sumaPesos.add(peso);
            componentesDisponibles++;
            coberturaCompleta &= ima.cobertura().compareTo(BigDecimal.ONE) == 0;
        }
        if (indicadores != null) {
            BigDecimal peso = W_INDICADORES.multiply(indicadores.cobertura());
            sumaPonderada = sumaPonderada.add(indicadores.promedio().multiply(peso));
            sumaPesos = sumaPesos.add(peso);
            componentesDisponibles++;
            coberturaCompleta &= indicadores.cobertura().compareTo(BigDecimal.ONE) == 0;
        }
        if (actividad != null) {
            BigDecimal peso = W_ACTIVIDAD.multiply(actividad.cobertura());
            sumaPonderada = sumaPonderada.add(actividad.promedio().multiply(peso));
            sumaPesos = sumaPesos.add(peso);
            componentesDisponibles++;
            coberturaCompleta &= actividad.cobertura().compareTo(BigDecimal.ONE) == 0;
        }

        if (componentesDisponibles == 0) {
            return null;
        }

        BigDecimal ecoScore = sumaPonderada.divide(sumaPesos, 1, RoundingMode.HALF_UP);
        ClasificacionAmbiental clasificacion = ClasificacionAmbiental.porPuntaje(ecoScore);
        boolean parcial = componentesDisponibles < 3 || !coberturaCompleta;

        return new EcoScoreResultado(ecoScore, clasificacion, parcial);
    }

    private ComponenteDisponible promedioIma(List<UUID> empresaIds, Map<UUID, IMADTO> imaMap) {
        List<BigDecimal> scores = empresaIds.stream()
                .map(imaMap::get)
                .filter(java.util.Objects::nonNull)
                .map(calculator::calcularScoreIma)
                .toList();
        return componenteDisponible(scores, empresaIds.size());
    }

    private ComponenteDisponible promedioIndicadores(List<UUID> empresaIds, Map<UUID, IndicadorAmbientalDTO> indicadorMap) {
        List<BigDecimal> scores = empresaIds.stream()
                .map(indicadorMap::get)
                .filter(java.util.Objects::nonNull)
                .map(calculator::calcularScoreCertificaciones)
                .toList();
        return componenteDisponible(scores, empresaIds.size());
    }

    private ComponenteDisponible promedioFactorActividad(Itinerario itinerario) {
        List<ItinerarioActividad> actividades = itinerario.getDias().stream()
                .flatMap(dia -> dia.getActividades().stream())
                .toList();
        List<BigDecimal> puntuaciones = actividades.stream()
                .map(ItinerarioActividad::getPuntuacionAmbientalEstimada)
                .filter(java.util.Objects::nonNull)
                .map(BigDecimal::valueOf)
                .toList();
        return componenteDisponible(puntuaciones, actividades.size());
    }

    /**
     * Empaqueta el promedio de {@code valores} junto con su cobertura sobre {@code total}
     * elementos. Retorna {@code null} si no hay ningún valor disponible (cobertura 0 equivale a
     * componente ausente).
     */
    private ComponenteDisponible componenteDisponible(List<BigDecimal> valores, int total) {
        if (valores.isEmpty() || total == 0) {
            return null;
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(BigDecimal.valueOf(valores.size()), 10, RoundingMode.HALF_UP);
        BigDecimal cobertura = BigDecimal.valueOf(valores.size())
                .divide(BigDecimal.valueOf(total), 10, RoundingMode.HALF_UP);
        return new ComponenteDisponible(promedio, cobertura);
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
