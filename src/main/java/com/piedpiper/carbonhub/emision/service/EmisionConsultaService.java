package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

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

    public EmisionConsultaService(EmisionRepository emisionRepository,
                                  UsuarioRepository usuarioRepository,
                                  EmisionElectricidadMapper emisionElectricidadMapper,
                                  EmisionVueloMapper emisionVueloMapper) {
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionElectricidadMapper = emisionElectricidadMapper;
        this.emisionVueloMapper = emisionVueloMapper;
    }

    @Transactional(readOnly = true)
    public List<EmisionResponseDTO> listar(UUID usuarioId) {
        return emisionRepository.findAllByEmpresaIdOrderByCreatedAtDesc(empresaId(usuarioId)).stream()
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
        return emisionRepository.findByIdAndEmpresaId(id, empresaId(usuarioId))
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontro la emision solicitada."));
    }

    private UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.accesoDenegado("El usuario autenticado no pertenece a una empresa.");
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
        throw ApiException.errorInterno("Tipo de emision no soportado.");
    }
}
