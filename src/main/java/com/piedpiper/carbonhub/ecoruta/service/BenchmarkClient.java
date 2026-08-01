package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BenchmarkClient {

    Map<UUID, BenchmarkDTO> consultarBenchmark(List<UUID> empresaIds);
}
