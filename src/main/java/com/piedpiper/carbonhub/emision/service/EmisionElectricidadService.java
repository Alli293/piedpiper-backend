package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarElectricidadRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class EmisionElectricidadService {

    // Selector estable del factor de electricidad de Costa Rica en Climatiq. "^6" referencia la
    // última versión compatible con la serie de datos v6, siguiendo el esquema de versionado de
    // Climatiq (equivalente a un rango semver), en vez de anclarse a un id de factor puntual que
    // puede cambiar cuando Climatiq actualiza su base de datos.
    private static final String ACTIVITY_ID = "electricity-supply_grid-source_supplier_mix-use_na";
    private static final String DATA_VERSION = "^6";
    private static final String REGION_COSTA_RICA = "CR";

    private final ClimatiqClient climatiqClient;
    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionElectricidadMapper emisionElectricidadMapper;

    public EmisionElectricidadService(ClimatiqClient climatiqClient,
                                      EmisionRepository emisionRepository,
                                      UsuarioRepository usuarioRepository,
                                      EmisionElectricidadMapper emisionElectricidadMapper) {
        this.climatiqClient = climatiqClient;
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionElectricidadMapper = emisionElectricidadMapper;
    }

    public EmisionResponseDTO registrar(RegistrarElectricidadRequestDTO request, UUID usuarioId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        ClimatiqEstimateResponse estimacion = climatiqClient.estimar(
                new ClimatiqEmissionFactorSelector(ACTIVITY_ID, DATA_VERSION, REGION_COSTA_RICA),
                Map.of(
                        "energy", request.getElectricityValue(),
                        "energy_unit", climatiqUnidad(request.getElectricityUnit())));

        BigDecimal carbonKg = estimacion.co2e();
        BigDecimal carbonMt = carbonKg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        EmisionElectricidad emision = EmisionElectricidad.builder()
                .titulo(request.getTitulo())
                .fechaActividad(request.getFechaActividad())
                .electricityValue(request.getElectricityValue())
                .electricityUnit(request.getElectricityUnit())
                .carbonKg(carbonKg)
                .carbonMt(carbonMt)
                .factorEmisionId(estimacion.emissionFactor().id())
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .createdByUserId(usuarioId)
                .build();
        emision = emisionRepository.save(emision);

        return emisionElectricidadMapper.toDto(emision);
    }

    private String climatiqUnidad(UnidadElectricidad unidad) {
        return unidad == UnidadElectricidad.MWH ? "MWh" : "kWh";
    }
}
