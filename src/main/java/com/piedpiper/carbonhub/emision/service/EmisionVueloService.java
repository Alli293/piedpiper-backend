package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarVueloRequestDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVueloLeg;
import com.piedpiper.carbonhub.emision.models.enums.CabinClass;
import com.piedpiper.carbonhub.emision.models.enums.DistanceUnit;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmisionVueloService {

    private static final BigDecimal KG_PER_METRIC_TON = BigDecimal.valueOf(1000);

    private final EmisionVueloLocalCalculator calculator;
    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionVueloMapper emisionVueloMapper;

    public EmisionVueloService(EmisionVueloLocalCalculator calculator,
                               EmisionRepository emisionRepository,
                               UsuarioRepository usuarioRepository,
                               EmisionVueloMapper emisionVueloMapper) {
        this.calculator = calculator;
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionVueloMapper = emisionVueloMapper;
    }

    @Transactional
    public EmisionResponseDTO registrar(RegistrarVueloRequestDTO request, UUID usuarioId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        Instant now = Instant.now();
        EmisionVuelo emision = EmisionVuelo.builder()
                .createdAt(now)
                .createdByUserId(usuarioId)
                .build();

        aplicarDatos(emision, request, now);
        emision = emisionRepository.save(emision);
        return emisionVueloMapper.toDto(emision);
    }

    @Transactional
    public EmisionResponseDTO actualizar(UUID id, RegistrarVueloRequestDTO request, UUID usuarioId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        EmisionVuelo emision = emisionRepository.findByIdAndCreatedByUserId(id, usuarioId)
                .filter(EmisionVuelo.class::isInstance)
                .map(EmisionVuelo.class::cast)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontro el vuelo solicitado."));

        aplicarDatos(emision, request, Instant.now());
        emision = emisionRepository.save(emision);
        return emisionVueloMapper.toDto(emision);
    }

    private void aplicarDatos(EmisionVuelo emision, RegistrarVueloRequestDTO request, Instant estimatedAt) {
        List<EmisionVueloLocalCalculator.Resultado> estimaciones = estimarLegs(request);

        BigDecimal carbonKg = estimaciones.stream()
                .map(EmisionVueloLocalCalculator.Resultado::carbonKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(request.getPassengers()))
                .setScale(3, RoundingMode.HALF_UP);
        BigDecimal distanceKm = estimaciones.stream()
                .map(EmisionVueloLocalCalculator.Resultado::distanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        emision.setTitulo(titulo(request.getLegs()));
        emision.setFechaActividad(request.getFechaActividad());
        emision.setPassengers(request.getPassengers());
        emision.setDistanceUnit(DistanceUnit.KM);
        emision.setDistanceValue(distanceKm);
        emision.setCarbonKg(carbonKg);
        emision.setCarbonMt(carbonKg.divide(KG_PER_METRIC_TON, 3, RoundingMode.HALF_UP));
        emision.setFactorEmisionId("local-flight-distance-v1");
        emision.setEstimatedAt(estimatedAt);
        emision.getLegs().clear();

        for (int i = 0; i < request.getLegs().size(); i++) {
            RegistrarVueloRequestDTO.LegDTO leg = request.getLegs().get(i);
            emision.addLeg(EmisionVueloLeg.builder()
                    .departureAirport(normalizarIata(leg.getDepartureAirport()))
                    .destinationAirport(normalizarIata(leg.getDestinationAirport()))
                    .cabinClass(leg.getCabinClass() == null ? CabinClass.ECONOMY : leg.getCabinClass())
                    .orden(i + 1)
                    .build());
        }
    }

    private String titulo(List<RegistrarVueloRequestDTO.LegDTO> legs) {
        RegistrarVueloRequestDTO.LegDTO first = legs.get(0);
        RegistrarVueloRequestDTO.LegDTO last = legs.get(legs.size() - 1);
        return "Viaje aereo " + normalizarIata(first.getDepartureAirport())
                + "-" + normalizarIata(last.getDestinationAirport());
    }

    private String normalizarIata(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private List<EmisionVueloLocalCalculator.Resultado> estimarLegs(RegistrarVueloRequestDTO request) {
        return request.getLegs().stream()
                .map(leg -> calculator.calcular(
                        normalizarIata(leg.getDepartureAirport()),
                        normalizarIata(leg.getDestinationAirport()),
                        leg.getCabinClass() == null ? CabinClass.ECONOMY : leg.getCabinClass()))
                .toList();
    }
}
