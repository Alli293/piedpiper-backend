package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.insignia.mappers.InsigniaEmpresaMapper;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.perfilpublico.service.SlugResolverService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InsigniaEmpresaConsultaService {

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final CatalogoInsigniaRepository catalogoInsigniaRepository;
    private final EmpresaRepository empresaRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final InsigniaEmpresaMapper insigniaEmpresaMapper;
    private final InsigniaEmpresaOpenBadgesService insigniaEmpresaOpenBadgesService;
    private final SlugResolverService slugResolver;

    public InsigniaEmpresaConsultaService(InsigniaEmpresaRepository insigniaEmpresaRepository,
                                          CatalogoInsigniaRepository catalogoInsigniaRepository,
                                          EmpresaRepository empresaRepository,
                                          EmisionEmpresaService emisionEmpresaService,
                                          InsigniaEmpresaMapper insigniaEmpresaMapper,
                                          InsigniaEmpresaOpenBadgesService
                                                  insigniaEmpresaOpenBadgesService,
                                          SlugResolverService slugResolver) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
        this.empresaRepository = empresaRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.insigniaEmpresaMapper = insigniaEmpresaMapper;
        this.insigniaEmpresaOpenBadgesService = insigniaEmpresaOpenBadgesService;
        this.slugResolver = slugResolver;
    }

    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarParaEmpresaAutenticada(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        return listarPorEmpresa(empresaId);
    }

    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarPorSlug(String slug) {
        Empresa empresa = slugResolver.resolver(slug);
        return listarPorEmpresa(empresa.getId());
    }

    private List<InsigniaEmpresaResponseDTO> listarPorEmpresa(UUID empresaId) {
        Map<ClaveCatalogoInsignia, CatalogoInsignia> catalogoPorClave =
                catalogoInsigniaRepository.findByActivaTrue()
                        .stream()
                        .collect(Collectors.toMap(
                                catalogo -> new ClaveCatalogoInsignia(
                                        catalogo.getIdInsignia(), catalogo.getNivelInsignia()),
                                Function.identity(),
                                (actual, duplicada) -> actual));
        return insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(empresaId)
                .stream()
                .map(insignia -> aDto(insignia, catalogoPorClave))
                .toList();
    }

    private InsigniaEmpresaResponseDTO aDto(InsigniaEmpresa insigniaEmpresa,
                                            Map<ClaveCatalogoInsignia, CatalogoInsignia>
                                                    catalogoPorClave) {
        InsigniaEmpresaResponseDTO dto = insigniaEmpresaMapper.toDto(insigniaEmpresa);
        CatalogoInsignia catalogo = catalogoPorClave.get(new ClaveCatalogoInsignia(
                insigniaEmpresa.getIdInsignia(), insigniaEmpresa.getNivelInsignia()));
        if (catalogo != null) {
            dto.setNombre(catalogo.getNombre());
            dto.setDescripcion(catalogo.getDescripcion());
            dto.setCriteriosObtencion(insigniaEmpresaOpenBadgesService.criterios(catalogo));
            dto.setEmisor(insigniaEmpresaOpenBadgesService.emisorNombre());
            dto.setReceptor(insigniaEmpresa.getEmpresa().getNombreEmpresa());
            dto.setUrlVerificacionPublica(insigniaEmpresaOpenBadgesService
                    .urlVerificacionPublica(insigniaEmpresa.getId()));
            dto.setUrlVerificacionJwt(insigniaEmpresaOpenBadgesService
                    .urlVerificacionJwt(insigniaEmpresa.getId()));
            dto.setUrlLinkedIn(insigniaEmpresaOpenBadgesService.urlLinkedIn(
                    insigniaEmpresa, catalogo));
        }
        return dto;
    }

    private record ClaveCatalogoInsignia(Long idInsignia, String nivelInsignia) {
    }
}
