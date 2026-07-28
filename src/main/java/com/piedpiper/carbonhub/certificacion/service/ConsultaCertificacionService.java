package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultaCertificacionService {

    private final CertificacionRepository certificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final CertificacionMapper certificacionMapper;

    public ConsultaCertificacionService(CertificacionRepository certificacionRepository,
                                        UsuarioRepository usuarioRepository,
                                        CatalogoTiposCertificacion catalogoTiposCertificacion,
                                        CertificacionMapper certificacionMapper) {
        this.certificacionRepository = certificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.certificacionMapper = certificacionMapper;
    }

    @Transactional(readOnly = true)
    public List<CertificacionResumenResponseDTO> listar(UUID usuarioId) {
        UUID empresaId = empresaDelUsuario(usuarioId);
        return certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(empresaId).stream()
                .map(this::aResumenDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificacionResponseDTO detalle(UUID usuarioId, UUID certificacionId) {
        UUID empresaId = empresaDelUsuario(usuarioId);
        // Se filtra por empresa en la consulta para no revelar la existencia de
        // certificaciones de otras empresas con un 403 en vez de un 404.
        return certificacionRepository.findByIdAndEmpresaId(certificacionId, empresaId)
                .map(this::aDto)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "La certificacion no existe."));
    }

    /**
     * Certificaciones activas de una empresa, para el perfil publico. Sin
     * resolucion de usuario ni auth a proposito: lo llama {@code
     * perfilpublico}, que ya resolvio el {@code empresaId} a partir de un
     * slug sin sesion. Vive aqui y no en {@code perfilpublico} para que ese
     * dominio no dependa directamente de {@link CertificacionRepository} ni
     * de {@link CertificacionMapper}.
     */
    @Transactional(readOnly = true)
    public List<CertificacionPublicaResponseDTO> listarActivasPublicasPorEmpresa(UUID empresaId) {
        return certificacionRepository
                .findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(empresaId, EstadoCertificacion.ACTIVA)
                .stream()
                .map(this::aPublicaDto)
                .toList();
    }

    private UUID empresaDelUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "Solo el administrador de la empresa puede consultar certificaciones."));
        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
    }

    private CertificacionResponseDTO aDto(Certificacion certificacion) {
        CertificacionResponseDTO dto = certificacionMapper.toDto(certificacion);
        dto.setRecienEmitida(false);
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }

    private CertificacionResumenResponseDTO aResumenDto(Certificacion certificacion) {
        CertificacionResumenResponseDTO dto = certificacionMapper.toResumenDto(certificacion);
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }

    private CertificacionPublicaResponseDTO aPublicaDto(Certificacion certificacion) {
        CertificacionPublicaResponseDTO dto = certificacionMapper.toPublicaDto(certificacion);
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }
}
