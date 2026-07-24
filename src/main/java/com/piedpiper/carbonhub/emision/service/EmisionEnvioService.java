package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarEnvioRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.service.ImaCacheInvalidator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmisionEnvioService {

    private static final Map<MetodoTransporte, String> ACTIVITY_IDS = Map.of(
            MetodoTransporte.TRUCK, "freight_vehicle-vehicle_type_commercial_truck-fuel_source_na-vehicle_weight_na-percentage_load_na",
            MetodoTransporte.SHIP, "sea_freight-vessel_type_bulk_and_general_cargo-route_type_coastal-vessel_length_na"
                    + "-tonnage_gt_10dwkt_lt_20_dwkt-fuel_source_na-load_type_na-distance_uplift_na",
            MetodoTransporte.TRAIN, "freight_train-route_type_domestic-fuel_type_diesel",
            MetodoTransporte.PLANE, "freight_flight-route_type_air_transport_freight_services-distance_na-weight_na"
                    + "-rf_na-method_na-aircraft_type_na-distance_uplift_na"
    );
    private static final String DATA_VERSION = "^6";

    private static final BigDecimal GRAMS_PER_TONNE = new BigDecimal("1000000");
    private static final BigDecimal LBS_PER_TONNE = new BigDecimal("2204.623");
    private static final BigDecimal KG_PER_TONNE = new BigDecimal("1000");

    private final ClimatiqClient climatiqClient;
    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionEnvioMapper emisionEnvioMapper;
    private final ImaCacheInvalidator imaCacheInvalidator;

    public EmisionEnvioService(ClimatiqClient climatiqClient,
                               EmisionRepository emisionRepository,
                               UsuarioRepository usuarioRepository,
                               EmisionEnvioMapper emisionEnvioMapper,
                               ImaCacheInvalidator imaCacheInvalidator) {
        this.climatiqClient = climatiqClient;
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionEnvioMapper = emisionEnvioMapper;
        this.imaCacheInvalidator = imaCacheInvalidator;
    }

    @Transactional
    public EmisionEnvioResponseDTO registrar(RegistrarEnvioRequestDTO request, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }

        String activityId = Optional.ofNullable(ACTIVITY_IDS.get(request.getTransportMethod()))
                .orElseThrow(ApiException::metodoTransporteNoSoportado);

        ClimatiqEstimateResponse estimacion = climatiqClient.estimar(
                new ClimatiqEmissionFactorSelector(activityId, DATA_VERSION, null),
                Map.of(
                        "weight", convertirPesoAToneladas(request.getWeightValue(), request.getWeightUnit()),
                        "weight_unit", "t",
                        "distance", request.getDistanceValue(),
                        "distance_unit", climatiqDistanceUnit(request.getDistanceUnit())));

        if (!"kg".equalsIgnoreCase(estimacion.co2eUnit())) {
            throw ApiException.calculoUnidadNoSoportada(estimacion.co2eUnit());
        }

        BigDecimal carbonKg = estimacion.co2e();
        BigDecimal carbonMt = carbonKg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        EmisionEnvio emision = EmisionEnvio.builder()
                .empresaId(usuario.getEmpresa().getId())
                .titulo(request.getTitulo())
                .fechaActividad(request.getFechaActividad())
                .weightValue(request.getWeightValue())
                .weightUnit(request.getWeightUnit())
                .distanceValue(request.getDistanceValue())
                .distanceUnit(request.getDistanceUnit())
                .transportMethod(request.getTransportMethod())
                .carbonKg(carbonKg)
                .carbonMt(carbonMt)
                .factorEmisionId(estimacion.emissionFactor().id())
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .createdByUserId(usuarioId)
                .build();
        emision = emisionRepository.save(emision);
        imaCacheInvalidator.invalidar(usuario.getEmpresa().getId());

        return emisionEnvioMapper.toDto(emision);
    }

    private BigDecimal convertirPesoAToneladas(BigDecimal valor, UnidadPeso unidad) {
        return switch (unidad) {
            case G -> valor.divide(GRAMS_PER_TONNE, 6, RoundingMode.HALF_UP);
            case LB -> valor.divide(LBS_PER_TONNE, 6, RoundingMode.HALF_UP);
            case KG -> valor.divide(KG_PER_TONNE, 6, RoundingMode.HALF_UP);
            case MT -> valor;
        };
    }

    private String climatiqDistanceUnit(UnidadDistancia unidad) {
        return switch (unidad) {
            case KM -> "km";
            case MI -> "mi";
        };
    }
}
