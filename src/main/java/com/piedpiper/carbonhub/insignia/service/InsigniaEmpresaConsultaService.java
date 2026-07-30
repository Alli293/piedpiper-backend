package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.mappers.InsigniaEmpresaMapper;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InsigniaEmpresaConsultaService {

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final CatalogoInsigniaRepository catalogoInsigniaRepository;
    private final EmpresaRepository empresaRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final InsigniaEmpresaMapper insigniaEmpresaMapper;

    public InsigniaEmpresaConsultaService(InsigniaEmpresaRepository insigniaEmpresaRepository,
                                          CatalogoInsigniaRepository catalogoInsigniaRepository,
                                          EmpresaRepository empresaRepository,
                                          EmisionEmpresaService emisionEmpresaService,
                                          InsigniaEmpresaMapper insigniaEmpresaMapper) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
        this.empresaRepository = empresaRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.insigniaEmpresaMapper = insigniaEmpresaMapper;
    }

    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarParaEmpresaAutenticada(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        return listarPorEmpresa(empresaId);
    }

    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarPorSlug(String slug) {
        UUID empresaId = empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO)
                .map(Empresa::getId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La empresa no existe."));
        return listarPorEmpresa(empresaId);
    }

    private List<InsigniaEmpresaResponseDTO> listarPorEmpresa(UUID empresaId) {
        return insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(empresaId)
                .stream()
                .map(this::aDto)
                .toList();
    }

    private InsigniaEmpresaResponseDTO aDto(InsigniaEmpresa insigniaEmpresa) {
        InsigniaEmpresaResponseDTO dto = insigniaEmpresaMapper.toDto(insigniaEmpresa);
        catalogoInsigniaRepository.findByIdInsigniaAndNivelInsigniaAndActivaTrue(
                insigniaEmpresa.getIdInsignia(), insigniaEmpresa.getNivelInsignia())
                .ifPresent(catalogo -> {
                    dto.setNombre(catalogo.getNombre());
                    dto.setDescripcion(catalogo.getDescripcion());
                });
        return dto;
    }
}
