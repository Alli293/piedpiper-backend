package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ImaClient {

    Map<UUID, IMADTO> consultarIma(List<UUID> empresaIds);
}
