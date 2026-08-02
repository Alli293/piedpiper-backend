package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.service.ImaCacheInvalidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmisionConsultaService {

    private final EmisionRepository emisionRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final EmisionElectricidadMapper emisionElectricidadMapper;
    private final EmisionVueloMapper emisionVueloMapper;
    private final EmisionEnvioMapper emisionEnvioMapper;
    private final EmisionFlotaMapper emisionFlotaMapper;
    private final ImaCacheInvalidator imaCacheInvalidator;

    public EmisionConsultaService(EmisionRepository emisionRepository,
                                  EmisionEmpresaService emisionEmpresaService,
                                  EmisionElectricidadMapper emisionElectricidadMapper,
                                  EmisionVueloMapper emisionVueloMapper,
                                  EmisionEnvioMapper emisionEnvioMapper,
                                  EmisionFlotaMapper emisionFlotaMapper,
                                  ImaCacheInvalidator imaCacheInvalidator) {
        this.emisionRepository = emisionRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.emisionElectricidadMapper = emisionElectricidadMapper;
        this.emisionVueloMapper = emisionVueloMapper;
        this.emisionEnvioMapper = emisionEnvioMapper;
        this.emisionFlotaMapper = emisionFlotaMapper;
        this.imaCacheInvalidator = imaCacheInvalidator;
    }

    @Transactional(readOnly = true)
    public List<EmisionResponseDTO> listar(UUID usuarioId, CategoriaEmision categoria, Integer anio, Integer mes) {
        validarMes(mes);
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        return listarPorCategoria(empresaId, categoria, anio, mes)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmisionResponseDTO obtener(UUID id, UUID usuarioId) {
        return toDto(buscarPropia(id, usuarioId));
    }

    @Transactional
    public void eliminar(UUID id, UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        Emision emision = emisionRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontró la emisión solicitada."));
        emisionRepository.delete(emision);
        imaCacheInvalidator.invalidar(empresaId);
    }

    private Emision buscarPropia(UUID id, UUID usuarioId) {
        return emisionRepository.findByIdAndEmpresaId(id, emisionEmpresaService.empresaId(usuarioId))
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontró la emisión solicitada."));
    }

    private void validarMes(Integer mes) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw ApiException.mesInvalido();
        }
    }

    private List<Emision> listarPorCategoria(
            UUID empresaId,
            CategoriaEmision categoria,
            Integer anio,
            Integer mes) {
        if (categoria == null) {
            return emisionRepository.findAllByEmpresaIdWithFilters(empresaId, anio, mes);
        }

        return switch (categoria) {
            case ELECTRICIDAD -> emisionRepository.findAllElectricidadByEmpresaIdWithFilters(empresaId, anio, mes);
            case FLOTA -> emisionRepository.findAllFlotaByEmpresaIdWithFilters(empresaId, anio, mes);
            case VUELO -> emisionRepository.findAllVueloByEmpresaIdWithFilters(empresaId, anio, mes);
            case ENVIO -> emisionRepository.findAllEnvioByEmpresaIdWithFilters(empresaId, anio, mes);
        };
    }

    private EmisionResponseDTO toDto(Emision emision) {
        if (emision instanceof EmisionVuelo vuelo) {
            return emisionVueloMapper.toDto(vuelo);
        }
        if (emision instanceof EmisionElectricidad electricidad) {
            return emisionElectricidadMapper.toDto(electricidad);
        }
        if (emision instanceof EmisionEnvio envio) {
            return emisionEnvioMapper.toDto(envio);
        }
        if (emision instanceof EmisionFlota flota) {
            return emisionFlotaMapper.toDto(flota);
        }
        throw ApiException.errorInterno("Tipo de emisión no soportado.");
    }
}
