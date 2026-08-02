package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación stub del cliente de indicadores ambientales.
 * Retorna un mapa vacío (sin indicadores disponibles) hasta que se implemente
 * la integración real con el módulo de certificaciones.
 */
@Component
public class IndicadorAmbientalClientStub implements IndicadorAmbientalClient {

    private static final Logger log = LoggerFactory.getLogger(IndicadorAmbientalClientStub.class);

    @Override
    public Map<UUID, IndicadorAmbientalDTO> consultarIndicadores(List<UUID> empresaIds) {
        log.debug("IndicadorAmbientalClient stub: retornando mapa vacío para {} empresas", empresaIds.size());
        return Map.of();
    }
}
