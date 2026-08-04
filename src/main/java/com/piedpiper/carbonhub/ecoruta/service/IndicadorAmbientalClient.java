package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IndicadorAmbientalClient {

    Map<UUID, IndicadorAmbientalDTO> consultarIndicadores(List<UUID> empresaIds);
}
