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
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmisionConsultaService {

    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionElectricidadMapper emisionElectricidadMapper;
    private final EmisionVueloMapper emisionVueloMapper;
    private final EmisionEnvioMapper emisionEnvioMapper;
    private final EmisionFlotaMapper emisionFlotaMapper;

    public EmisionConsultaService(EmisionRepository emisionRepository,
                                  UsuarioRepository usuarioRepository,
                                  EmisionElectricidadMapper emisionElectricidadMapper,
                                  EmisionVueloMapper emisionVueloMapper,
                                  EmisionEnvioMapper emisionEnvioMapper,
                                  EmisionFlotaMapper emisionFlotaMapper) {
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionElectricidadMapper = emisionElectricidadMapper;
        this.emisionVueloMapper = emisionVueloMapper;
        this.emisionEnvioMapper = emisionEnvioMapper;
        this.emisionFlotaMapper = emisionFlotaMapper;
    }

    @Transactional(readOnly = true)
    public List<EmisionResponseDTO> listar(UUID usuarioId, CategoriaEmision categoria, Integer anio, Integer mes) {
        validarMes(mes);
        return emisionRepository.findAllByEmpresaIdWithFilters(
                        empresaId(usuarioId),
                        categoria == null ? null : categoria.name(),
                        anio,
                        mes)
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
        Emision emision = buscarPropia(id, usuarioId);
        emisionRepository.delete(emision);
    }

    private Emision buscarPropia(UUID id, UUID usuarioId) {
        UUID empresaId = empresaId(usuarioId);
        Emision emision = emisionRepository.findById(id)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontró la emisión solicitada."));
        if (!empresaId.equals(emision.getEmpresaId())) {
            throw ApiException.accesoDenegado("No tiene permiso para acceder a este registro.");
        }
        return emision;
    }

    private void validarMes(Integer mes) {
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El mes debe estar entre 1 y 12.");
        }
    }

    private UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
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
