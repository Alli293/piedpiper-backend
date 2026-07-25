package com.piedpiper.carbonhub.limite.service;

import com.piedpiper.carbonhub.limite.mappers.LimiteEmisionesMapper;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LimiteEmisionesService {
    private final LimiteEmisionesRepository repository;
    private final LimiteEmisionesMapper limiteEmisionesMapper;

    public LimiteEmisionesService(LimiteEmisionesRepository repository, LimiteEmisionesMapper limiteEmisionesMapper) {
        this.repository = repository;
        this.limiteEmisionesMapper = limiteEmisionesMapper;
    }

    @Transactional
    public LimiteEmisionesResponseDTO guardarLimite(UUID empresaId, LimiteEmisionesRequestDTO request) {
        Optional<LimiteEmisiones> existente = repository.findByEmpresaIdAndAnio(empresaId, request.getAnio());
        boolean esNueva = existente.isEmpty();
        LimiteEmisiones limite = existente
                .map(existing -> {
                    existing.setLimiteMt(request.getLimiteMt());
                    existing.setJustificacion(normalizarJustificacion(request.getJustificacion()));
                    return existing;
                })
                .orElseGet(() -> new LimiteEmisiones(
                        empresaId,
                        request.getAnio(),
                        request.getLimiteMt(),
                        normalizarJustificacion(request.getJustificacion())
                ));

        try {
            LimiteEmisionesResponseDTO dto = toDto(repository.save(limite), true);
            dto.setRecienCreada(esNueva);
            return dto;
        } catch (DataIntegrityViolationException e) {
            throw ApiException.limiteConflicto();
        }
    }

    @Transactional(readOnly = true)
    public Optional<LimiteEmisionesResponseDTO> obtenerLimite(UUID empresaId, Integer anio) {
        return repository.findByEmpresaIdAndAnio(empresaId, anio)
                .map(limite -> toDto(limite, true));
    }

    @Transactional(readOnly = true)
    public List<LimiteEmisionesResponseDTO> listarLimites(UUID empresaId) {
        return repository.findAllByEmpresaIdOrderByAnioDesc(empresaId).stream()
                .map(limite -> toDto(limite, false))
                .toList();
    }

    @Transactional
    public void eliminarLimite(UUID empresaId, Integer anio) {
        LimiteEmisiones limite = repository.findByEmpresaIdAndAnio(empresaId, anio)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Limite no encontrado."));
        repository.delete(limite);
    }

    private LimiteEmisionesResponseDTO toDto(LimiteEmisiones limite, boolean incluirMensaje) {
        LimiteEmisionesResponseDTO dto = limiteEmisionesMapper.toDto(limite);
        if (incluirMensaje) {
            dto.setMensaje(mensajeLimite(limite));
        }
        return dto;
    }

    private String mensajeLimite(LimiteEmisiones limite) {
        return "Limite vigente del anio " + limite.getAnio() + ": " + limite.getLimiteMt() + " t CO2e.";
    }

    private String normalizarJustificacion(String justificacion) {
        if (justificacion == null || justificacion.isBlank()) {
            return null;
        }
        return justificacion.trim();
    }
}
