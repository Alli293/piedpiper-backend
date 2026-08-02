package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación stub del cliente de benchmarking.
 * Retorna un mapa vacío hasta que se implemente la integración real
 * con el módulo de benchmarking.
 */
@Component
public class BenchmarkClientStub implements BenchmarkClient {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkClientStub.class);

    @Override
    public Map<UUID, BenchmarkDTO> consultarBenchmark(List<UUID> empresaIds) {
        log.debug("BenchmarkClient stub: retornando mapa vacío para {} empresas", empresaIds.size());
        return Map.of();
    }
}
