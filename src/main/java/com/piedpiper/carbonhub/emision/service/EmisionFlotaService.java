package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarFlotaRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO.CombustibleResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmisionFlotaService {
    // Climatiq para Alemania (DE, sí ofrece un desglose limpio de combustible -incluido eléctrico- consistente
    // entre las 4 categorías de vehículo, por lo que se fija como región para todo el catálogo
    private static final String DATA_VERSION = "^6";
    private static final String REGION_ALEMANIA = "DE";

    private final ClimatiqClient climatiqClient;
    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionFlotaMapper emisionFlotaMapper;

    public EmisionFlotaService(ClimatiqClient climatiqClient,
                               EmisionRepository emisionRepository,
                               UsuarioRepository usuarioRepository,
                               EmisionFlotaMapper emisionFlotaMapper) {
        this.climatiqClient = climatiqClient;
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionFlotaMapper = emisionFlotaMapper;
    }

    public List<TipoVehiculoResponseDTO> listarTiposVehiculo() {
        return Arrays.stream(TipoVehiculo.values())
                .map(tipo -> new TipoVehiculoResponseDTO(tipo.name(), tipo.getNombre(), combustibles(tipo)))
                .toList();
    }

    private List<CombustibleResponseDTO> combustibles(TipoVehiculo tipo) {
        return CatalogoVehiculoFlota.combustiblesValidos(tipo).stream()
                .map(combustible -> new CombustibleResponseDTO(combustible.name(), combustible.getNombre()))
                .toList();
    }

    public EmisionFlotaResponseDTO registrar(RegistrarFlotaRequestDTO request, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }

        TipoVehiculo tipoVehiculo = request.getTipoVehiculo();
        Combustible combustible = request.getCombustible();
        String activityId = CatalogoVehiculoFlota.activityId(tipoVehiculo, combustible)
                .orElseThrow(ApiException::combinacionVehiculoInvalida);

        ClimatiqEstimateResponse estimacion = climatiqClient.estimar(
                new ClimatiqEmissionFactorSelector(activityId, DATA_VERSION, REGION_ALEMANIA),
                Map.of(
                        "distance", request.getDistanceValue(),
                        "distance_unit", climatiqUnidad(request.getDistanceUnit())));

        if (!"kg".equalsIgnoreCase(estimacion.co2eUnit())) {
            throw ApiException.calculoUnidadNoSoportada(estimacion.co2eUnit());
        }

        BigDecimal carbonKg = estimacion.co2e();
        BigDecimal carbonMt = carbonKg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        EmisionFlota emision = EmisionFlota.builder()
                .empresaId(usuario.getEmpresa().getId())
                .titulo(request.getTitulo())
                .fechaActividad(request.getFechaActividad())
                .tipoVehiculo(tipoVehiculo)
                .combustible(combustible)
                .distanceValue(request.getDistanceValue())
                .distanceUnit(request.getDistanceUnit())
                .carbonKg(carbonKg)
                .carbonMt(carbonMt)
                .factorEmisionId(estimacion.emissionFactor().id())
                .estimatedAt(Instant.now())
                .createdAt(Instant.now())
                .createdByUserId(usuarioId)
                .build();
        emision = emisionRepository.save(emision);

        return emisionFlotaMapper.toDto(emision);
    }

    private String climatiqUnidad(UnidadDistancia unidad) {
        return unidad == UnidadDistancia.MI ? "mi" : "km";
    }
}
