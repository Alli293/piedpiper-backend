package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmisionConsultaService {

    private final EmisionRepository emisionRepository;
    private final EmisionElectricidadMapper emisionElectricidadMapper;
    private final EmisionVueloMapper emisionVueloMapper;

    public EmisionConsultaService(EmisionRepository emisionRepository,
                                  EmisionElectricidadMapper emisionElectricidadMapper,
                                  EmisionVueloMapper emisionVueloMapper) {
        this.emisionRepository = emisionRepository;
        this.emisionElectricidadMapper = emisionElectricidadMapper;
        this.emisionVueloMapper = emisionVueloMapper;
    }

    @Transactional(readOnly = true)
    public List<EmisionResponseDTO> listar(UUID usuarioId) {
        return emisionRepository.findAllByCreatedByUserIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmisionResponseDTO obtener(UUID id, UUID usuarioId) {
        return toDto(buscarPropia(id, usuarioId));
    }

    @Transactional
    public void eliminar(UUID id, UUID usuarioId) {
        Emision emision = buscarPropia(id, usuarioId);
        emisionRepository.delete(emision);
    }

    private Emision buscarPropia(UUID id, UUID usuarioId) {
        return emisionRepository.findByIdAndCreatedByUserId(id, usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontro la emision solicitada."));
    }

    private EmisionResponseDTO toDto(Emision emision) {
        if (emision instanceof EmisionVuelo vuelo) {
            return emisionVueloMapper.toDto(vuelo);
        }
        if (emision instanceof EmisionElectricidad electricidad) {
            return emisionElectricidadMapper.toDto(electricidad);
        }
        throw ApiException.errorInterno("Tipo de emision no soportado.");
    }
}
