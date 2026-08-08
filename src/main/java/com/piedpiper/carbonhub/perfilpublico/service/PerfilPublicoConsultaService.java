package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.BusquedaPerfilPublicoDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PerfilPublicoConsultaService {

    private final EmpresaRepository empresaRepository;
    private final CertificacionRepository certificacionRepository;
    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final SlugResolverService slugResolver;

    public PerfilPublicoConsultaService(EmpresaRepository empresaRepository,
                                        CertificacionRepository certificacionRepository,
                                        InsigniaEmpresaRepository insigniaEmpresaRepository,
                                        SlugResolverService slugResolver) {
        this.empresaRepository = empresaRepository;
        this.certificacionRepository = certificacionRepository;
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.slugResolver = slugResolver;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponseDTO obtenerPorSlug(String slugOriginal) {
        Empresa empresa = slugResolver.resolver(slugOriginal);

        // 4. Contar certificaciones vigentes (fecha expiración futura o null)
        int certificacionesVigentes = contarCertificacionesVigentes(empresa);

        // 5. Contar insignias activas
        int insigniasActivas = insigniaEmpresaRepository
                .findByEmpresaIdOrderByFechaObtencionDesc(empresa.getId()).size();

        // 6. Ensamblar DTO
        String nivelEcologico = resolverNivelEcologico(empresa.getNivelEcologico());

        return new PerfilPublicoResponseDTO(
                empresa.getNombreEmpresa(),
                empresa.getLogoUrl(),
                empresa.getSectorIndustrial() != null
                        ? empresa.getSectorIndustrial().name()
                        : null,
                empresa.getPais(),
                nivelEcologico,
                null, // fechaActualizacionNivel - no existe campo en entidad aún
                certificacionesVigentes,
                insigniasActivas
        );
    }

    private int contarCertificacionesVigentes(Empresa empresa) {
        try {
            return certificacionRepository
                    .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                            empresa.getId(), EstadoCertificacion.ACTIVA, LocalDate.now())
                    .size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public Page<BusquedaPerfilPublicoDTO> buscarPorNombre(String nombre, int page, int size) {
        int limitedSize = Math.min(size, 12);
        Pageable pageable = PageRequest.of(page, limitedSize);

        Page<Empresa> empresas;
        if (nombre == null || nombre.trim().length() < 3) {
            // Sin filtro: retorna las primeras empresas activas (catálogo)
            empresas = empresaRepository.findByEstado(EstadoEmpresa.ACTIVO, pageable);
        } else {
            empresas = empresaRepository.findByNombreEmpresaContainingIgnoreCaseAndEstado(
                    nombre.trim(), EstadoEmpresa.ACTIVO, pageable);
        }
        return empresas.map(this::mapToBusquedaDTO);
    }

    private BusquedaPerfilPublicoDTO mapToBusquedaDTO(Empresa empresa) {
        return new BusquedaPerfilPublicoDTO(
                empresa.getNombreEmpresa(),
                empresa.getSlug(),
                empresa.getSectorIndustrial() != null ? empresa.getSectorIndustrial().name() : null,
                resolverNivelEcologico(empresa.getNivelEcologico())
        );
    }

    private String resolverNivelEcologico(String nivelEcologico) {
        if (nivelEcologico == null || nivelEcologico.isBlank()) {
            return "Sin nivel";
        }
        return nivelEcologico;
    }
}
