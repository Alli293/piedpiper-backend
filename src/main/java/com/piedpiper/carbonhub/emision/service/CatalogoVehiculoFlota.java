package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Matriz fija de tipo de vehículo x combustible -> activity_id de Climatiq
 * No todas las combinaciones existen en los datos de Climatiq: camión pesado, por ejemplo,
 * solo tiene un factor promedio
 */
final class CatalogoVehiculoFlota {

    private static final Map<TipoVehiculo, Map<Combustible, String>> ACTIVITY_IDS = construirCatalogo();

    private CatalogoVehiculoFlota() {
    }

    static List<Combustible> combustiblesValidos(TipoVehiculo tipoVehiculo) {
        return List.copyOf(ACTIVITY_IDS.get(tipoVehiculo).keySet());
    }

    static Optional<String> activityId(TipoVehiculo tipoVehiculo, Combustible combustible) {
        return Optional.ofNullable(ACTIVITY_IDS.get(tipoVehiculo))
                .map(porCombustible -> porCombustible.get(combustible));
    }

    private static Map<TipoVehiculo, Map<Combustible, String>> construirCatalogo() {
        Map<TipoVehiculo, Map<Combustible, String>> catalogo = new EnumMap<>(TipoVehiculo.class);

        Map<Combustible, String> automovil = new EnumMap<>(Combustible.class);
        automovil.put(Combustible.PROMEDIO,
                "passenger_vehicle-vehicle_type_car-fuel_source_na-engine_size_na-vehicle_age_na-vehicle_weight_na");
        automovil.put(Combustible.GASOLINA,
                "passenger_vehicle-vehicle_type_car-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        automovil.put(Combustible.DIESEL,
                "passenger_vehicle-vehicle_type_car-fuel_source_diesel-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        automovil.put(Combustible.PHEV,
                "passenger_vehicle-vehicle_type_car-fuel_source_phev-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        automovil.put(Combustible.BEV,
                "passenger_vehicle-vehicle_type_car-fuel_source_bev-engine_size_na-vehicle_age_na-vehicle_weight_na");
        catalogo.put(TipoVehiculo.AUTOMOVIL, Collections.unmodifiableMap(automovil));

        Map<Combustible, String> motocicleta = new EnumMap<>(Combustible.class);
        motocicleta.put(Combustible.PROMEDIO,
                "passenger_vehicle-vehicle_type_motorcycle-fuel_source_na-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        motocicleta.put(Combustible.GASOLINA,
                "passenger_vehicle-vehicle_type_motorcycle-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        motocicleta.put(Combustible.BEV,
                "passenger_vehicle-vehicle_type_motorcycle-fuel_source_bev-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        catalogo.put(TipoVehiculo.MOTOCICLETA, Collections.unmodifiableMap(motocicleta));

        Map<Combustible, String> furgonetaComercial = new EnumMap<>(Combustible.class);
        furgonetaComercial.put(Combustible.PROMEDIO,
                "commercial_vehicle-vehicle_type_lcv-fuel_source_na-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_lt_3.5t");
        furgonetaComercial.put(Combustible.GASOLINA,
                "commercial_vehicle-vehicle_type_lcv-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_lt_3.5t");
        furgonetaComercial.put(Combustible.DIESEL,
                "commercial_vehicle-vehicle_type_lcv-fuel_source_diesel-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_lt_3.5t");
        catalogo.put(TipoVehiculo.FURGONETA_COMERCIAL, Collections.unmodifiableMap(furgonetaComercial));

        Map<Combustible, String> camionPesado = new EnumMap<>(Combustible.class);
        camionPesado.put(Combustible.PROMEDIO,
                "commercial_vehicle-vehicle_type_hcv-fuel_source_na-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_gt_3.5t");
        catalogo.put(TipoVehiculo.CAMION_PESADO, Collections.unmodifiableMap(camionPesado));

        return Collections.unmodifiableMap(catalogo);
    }
}
