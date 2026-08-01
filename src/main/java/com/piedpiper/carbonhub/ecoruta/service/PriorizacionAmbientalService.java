package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ResultadoPriorizacion;
import com.piedpiper.carbonhub.ecoruta.models.entities.RegistroPonderacion;
import com.piedpiper.carbonhub.ecoruta.repository.RegistroPonderacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio responsable de aplicar la priorización ambiental al ranking de establecimientos
 * generado por el Motor_Recomendacion. Consulta indicadores ambientales de forma independiente,
 * calcula la puntuación ambiental y aplica un boost aditivo al ranking turístico base.
 *
 * Garantiza degradación graciosa: si cualquier fuente de datos falla, se continúa
 * sin ese componente (puntuación neutral = 0).
 */
@Service
public class PriorizacionAmbientalService {

    private static final Logger log = LoggerFactory.getLogger(PriorizacionAmbientalService.class);

    private final IndicadorAmbientalClient indicadorClient;
    private final ImaClient imaClient;
    private final BenchmarkClient benchmarkClient;
    private final PuntuacionAmbientalCalculator calculator;
    private final RegistroPonderacionRepository registroPonderacionRepository;

    @Value("${ecoruta.priorizacion.factor-boost:0.15}")
    private BigDecimal factorBoost;

    public PriorizacionAmbientalService(IndicadorAmbientalClient indicadorClient,
                                        ImaClient imaClient,
                                        BenchmarkClient benchmarkClient,
                                        PuntuacionAmbientalCalculator calculator,
                                        RegistroPonderacionRepository registroPonderacionRepository) {
        this.indicadorClient = indicadorClient;
        this.imaClient = imaClient;
        this.benchmarkClient = benchmarkClient;
        this.calculator = calculator;
        this.registroPonderacionRepository = registroPonderacionRepository;
    }

    /**
     * Aplica la priorización ambiental a una lista de establecimientos ya rankeados por
     * criterios turísticos. Calcula la puntuación ambiental de cada uno y ajusta el ranking
     * final con un boost aditivo.
     *
     * @param establecimientosBase lista de establecimientos con su puntuación turística base
     * @param itinerarioId         identificador del itinerario en generación
     * @param usuarioId            identificador del usuario que solicita la generación
     * @return resultado con la lista re-ordenada y estadísticas de evaluación
     */
    @Transactional
    public ResultadoPriorizacion aplicarPriorizacion(List<EstablecimientoRankeado> establecimientosBase,
                                                     UUID itinerarioId,
                                                     UUID usuarioId) {
        List<UUID> empresaIds = establecimientosBase.stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .toList();

        // Consultar cada fuente de datos de forma independiente (degradación graciosa)
        Map<UUID, IndicadorAmbientalDTO> indicadores = consultarIndicadoresSafe(empresaIds);
        Map<UUID, IMADTO> imaMap = consultarImaSafe(empresaIds);
        Map<UUID, BenchmarkDTO> benchmarkMap = consultarBenchmarkSafe(empresaIds);

        int indicadoresNoDisponibles = 0;
        if (indicadores.isEmpty()) indicadoresNoDisponibles++;
        if (imaMap.isEmpty()) indicadoresNoDisponibles++;
        if (benchmarkMap.isEmpty()) indicadoresNoDisponibles++;

        // Calcular puntuación ambiental y aplicar boost para cada establecimiento
        for (EstablecimientoRankeado establecimiento : establecimientosBase) {
            UUID empresaId = establecimiento.getEmpresaId();

            IndicadorAmbientalDTO indicador = indicadores.get(empresaId);
            IMADTO ima = imaMap.get(empresaId);
            BenchmarkDTO benchmark = benchmarkMap.get(empresaId);

            PuntuacionAmbientalResponseDTO detalle = calculator.calcular(indicador, ima, benchmark);

            BigDecimal puntuacionAmbiental = detalle.getPuntuacionTotal();
            establecimiento.setPuntuacionAmbiental(puntuacionAmbiental);
            establecimiento.setDetalleAmbiental(detalle);

            // RankingFinal = RankingTuristicoBase + (PuntuacionAmbiental * FACTOR_BOOST)
            BigDecimal boost = puntuacionAmbiental.multiply(factorBoost);
            BigDecimal puntuacionFinal = establecimiento.getPuntuacionTuristica().add(boost);
            establecimiento.setPuntuacionFinal(puntuacionFinal);

            // Persistir registro de auditoría
            persistirRegistroPonderacion(itinerarioId, usuarioId, empresaId, detalle);
        }

        // Re-ordenar por puntuación final descendente
        establecimientosBase.sort(Comparator.comparing(EstablecimientoRankeado::getPuntuacionFinal).reversed());

        return new ResultadoPriorizacion(
                establecimientosBase,
                establecimientosBase.size(),
                indicadoresNoDisponibles
        );
    }

    private Map<UUID, IndicadorAmbientalDTO> consultarIndicadoresSafe(List<UUID> empresaIds) {
        try {
            return indicadorClient.consultarIndicadores(empresaIds);
        } catch (Exception e) {
            log.warn("Fallo en consulta de indicadores ambientales. Continuando sin indicadores.", e);
            return Map.of();
        }
    }

    private Map<UUID, IMADTO> consultarImaSafe(List<UUID> empresaIds) {
        try {
            return imaClient.consultarIma(empresaIds);
        } catch (Exception e) {
            log.warn("Fallo en consulta de IMA. Continuando sin IMA.", e);
            return Map.of();
        }
    }

    private Map<UUID, BenchmarkDTO> consultarBenchmarkSafe(List<UUID> empresaIds) {
        try {
            return benchmarkClient.consultarBenchmark(empresaIds);
        } catch (Exception e) {
            log.warn("Fallo en consulta de benchmarking. Continuando sin benchmark.", e);
            return Map.of();
        }
    }

    private void persistirRegistroPonderacion(UUID itinerarioId, UUID usuarioId,
                                              UUID empresaId, PuntuacionAmbientalResponseDTO detalle) {
        RegistroPonderacion registro = RegistroPonderacion.builder()
                .itinerarioId(itinerarioId)
                .usuarioId(usuarioId)
                .empresaId(empresaId)
                .puntuacionTotal(detalle.getPuntuacionTotal())
                .componenteCertificaciones(detalle.getComponenteCertificaciones())
                .componenteIma(detalle.getComponenteIma())
                .componenteBenchmark(detalle.getComponenteBenchmark())
                .cantidadCertificaciones(detalle.getCantidadCertificacionesActivas())
                .build();

        registroPonderacionRepository.save(registro);
    }
}
