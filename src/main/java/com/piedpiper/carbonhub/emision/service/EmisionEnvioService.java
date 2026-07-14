package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarEnvioRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;
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
public class EmisionEnvioService {

    private static final String ACTIVITY_ID =
            "freight_vehicle-vehicle_type_hgv_all_diesel-fuel_source_na-distance_na-weight_na";
    private static final String DATA_VERSION = "^1";
    private static final String REGION = "CR";

    private final ClimatiqClient climatiqClient;
    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionEnvioMapper emisionEnvioMapper;

    public EmisionEnvioService(ClimatiqClient climatiqClient,
                               EmisionRepository emisionRepository,
                               UsuarioRepository usuarioRepository,
                               EmisionEnvioMapper emisionEnvioMapper) {
        this.climatiqClient = climatiqClient;
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionEnvioMapper = emisionEnvioMapper;
    }

    public EmisionEnvioResponseDTO registrar(RegistrarEnvioRequestDTO request, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        ClimatiqEstimateResponse estimacion = climatiqClient.estimar(
                new ClimatiqEmissionFactorSelector(ACTIVITY_ID, DATA_VERSION, REGION),
                Map.of(
                        "weight", request.getWeightValue(),
                        "weight_unit", climatiqWeightUnit(request.getWeightUnit()),
                        "distance", request.getDistanceValue(),
                        "distance_unit", climatiqDistanceUnit(request.getDistanceUnit())));

        BigDecimal carbonKg = estimacion.co2e();
        if (carbonKg == null) {
            throw ApiException.errorInterno("El servicio de cálculo no devolvió un resultado válido.");
        }
        BigDecimal carbonMt = carbonKg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        EmisionEnvio emision = EmisionEnvio.builder()
                .titulo(request.getTitulo())
                .fechaActividad(request.getFechaActividad())
                .weightValue(request.getWeightValue())
                .weightUnit(request.getWeightUnit())
                .distanceValue(request.getDistanceValue())
                .distanceUnit(request.getDistanceUnit())
                .transportMethod(request.getTransportMethod())
                // TODO: asignar empresaId desde usuario cuando la entidad Usuario tenga el campo (deuda técnica)
                .carbonKg(carbonKg)
                .carbonMt(carbonMt)
                .factorEmisionId(estimacion.emissionFactor().id())
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .createdByUserId(usuarioId)
                .build();
        emision = emisionRepository.save(emision);

        return emisionEnvioMapper.toDto(emision);
    }

    private String climatiqWeightUnit(UnidadPeso unidad) {
        return switch (unidad) {
            case G -> "g";
            case LB -> "lb";
            case KG -> "kg";
            case MT -> "t";
        };
    }

    private String climatiqDistanceUnit(UnidadDistancia unidad) {
        return switch (unidad) {
            case KM -> "km";
            case MI -> "mi";
        };
    }
}
