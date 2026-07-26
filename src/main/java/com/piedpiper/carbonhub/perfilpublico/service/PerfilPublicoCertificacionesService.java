package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Perfil publico de una empresa: expone solo lo que un visitante sin sesion
 * puede ver. Hoy unicamente la lista de certificaciones activas; otros datos
 * del perfil (nombre, logo, etc.) se agregaran a este mismo dominio mas
 * adelante.
 */
@Service
public class PerfilPublicoCertificacionesService {

    private final EmpresaRepository empresaRepository;
    private final CertificacionRepository certificacionRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final CertificacionMapper certificacionMapper;

    public PerfilPublicoCertificacionesService(EmpresaRepository empresaRepository,
                                               CertificacionRepository certificacionRepository,
                                               CatalogoTiposCertificacion catalogoTiposCertificacion,
                                               CertificacionMapper certificacionMapper) {
        this.empresaRepository = empresaRepository;
        this.certificacionRepository = certificacionRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.certificacionMapper = certificacionMapper;
    }

    @Transactional(readOnly = true)
    public List<CertificacionPublicaResponseDTO> listarPorSlug(String slug) {
        UUID empresaId = empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO)
                .map(Empresa::getId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La empresa no existe."));
        return certificacionRepository
                .findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(empresaId, EstadoCertificacion.ACTIVA)
                .stream()
                .map(this::aDto)
                .toList();
    }

    private CertificacionPublicaResponseDTO aDto(Certificacion certificacion) {
        CertificacionPublicaResponseDTO dto = certificacionMapper.toPublicaDto(certificacion);
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }
}
