package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación stub del cliente de IMA.
 * Retorna un mapa vacío hasta que se implemente la integración real
 * con el módulo de benchmarking/IMA.
 */
@Component
public class ImaClientStub implements ImaClient {

    private static final Logger log = LoggerFactory.getLogger(ImaClientStub.class);

    @Override
    public Map<UUID, IMADTO> consultarIma(List<UUID> empresaIds) {
        log.debug("ImaClient stub: retornando mapa vacío para {} empresas", empresaIds.size());
        return Map.of();
    }
}
