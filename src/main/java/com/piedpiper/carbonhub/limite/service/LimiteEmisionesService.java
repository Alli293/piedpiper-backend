package com.piedpiper.carbonhub.limite.service;

import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LimiteEmisionesService {
    private final LimiteEmisionesRepository repository;

    public LimiteEmisionesService(LimiteEmisionesRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LimiteEmisionesResponseDTO guardarLimite(Long empresaId, LimiteEmisionesRequestDTO request) {
        LimiteEmisiones limite = repository
                .findByEmpresaIdAndAnio(empresaId, request.anio())
                .map(existing -> {
                    existing.setLimiteMt(request.limiteMt());
                    existing.setJustificacion(normalizarJustificacion(request.justificacion()));
                    return existing;
                })
                .orElseGet(() -> new LimiteEmisiones(
                        empresaId,
                        request.anio(),
                        request.limiteMt(),
                        normalizarJustificacion(request.justificacion())
                ));

        return toDto(repository.save(limite));
    }

    @Transactional(readOnly = true)
    public Optional<LimiteEmisionesResponseDTO> obtenerLimite(Long empresaId, Integer anio) {
        return repository.findByEmpresaIdAndAnio(empresaId, anio).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<LimiteEmisionesResponseDTO> listarLimites(Long empresaId) {
        return repository.findAllByEmpresaIdOrderByAnioDesc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void eliminarLimite(Long empresaId, Integer anio) {
        LimiteEmisiones limite = repository.findByEmpresaIdAndAnio(empresaId, anio)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Limite no encontrado."));
        repository.delete(limite);
    }

    private LimiteEmisionesResponseDTO toDto(LimiteEmisiones limite) {
        return new LimiteEmisionesResponseDTO(
                limite.getId(),
                limite.getEmpresaId(),
                limite.getAnio(),
                limite.getLimiteMt(),
                limite.getJustificacion(),
                "Limite vigente del anio " + limite.getAnio() + ": " + limite.getLimiteMt() + " t CO2e.",
                limite.getActualizadoEn()
        );
    }

    private String normalizarJustificacion(String justificacion) {
        if (justificacion == null || justificacion.isBlank()) {
            return null;
        }
        return justificacion.trim();
    }
}
