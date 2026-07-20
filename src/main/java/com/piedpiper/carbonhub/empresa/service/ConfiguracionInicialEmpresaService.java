package com.piedpiper.carbonhub.empresa.service;

import com.piedpiper.carbonhub.empresa.mappers.EmpresaMapper;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaRequestDTO;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConfiguracionInicialEmpresaService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionInicialEmpresaService.class);

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaMapper empresaMapper;

    public ConfiguracionInicialEmpresaService(EmpresaRepository empresaRepository,
                                              UsuarioRepository usuarioRepository,
                                              EmpresaMapper empresaMapper) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaMapper = empresaMapper;
    }

    @Transactional
    public ConfiguracionInicialEmpresaResponseDTO completarConfiguracionEmpresa(
            UUID usuarioId, ConfiguracionInicialEmpresaRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno(
                        "No se pudo identificar al usuario autenticado."));

        if (usuario.getRol() != Rol.ADMINISTRADOR_EMPRESA) {
            throw ApiException.accesoDenegado(
                    "Solo el administrador de una empresa puede completar este paso.");
        }

        if (usuario.getEmpresa() != null) {
            ConfiguracionInicialEmpresaResponseDTO response = empresaMapper.toDto(usuario.getEmpresa());
            response.setDocumentosPendientes(true);
            response.setRecienCreada(false);
            return response;
        }

        if (empresaRepository.existsByCedulaJuridica(request.getCedulaJuridica())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con esta cédula jurídica.");
        }

        String correoCorporativo = usuario.getEmail();

        String slug = generarSlugUnico(request.getNombreEmpresa());

        Empresa empresa = Empresa.builder()
                .nombreEmpresa(request.getNombreEmpresa())
                .cedulaJuridica(request.getCedulaJuridica())
                .sectorIndustrial(request.getSectorIndustrial())
                .pais(request.getPais())
                .cantidadEmpleados(request.getCantidadEmpleados())
                .correoCorporativo(correoCorporativo)
                .slug(slug)
                .descripcion(request.getDescripcion())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        Empresa empresaGuardada;
        try {
            empresaGuardada = empresaRepository.saveAndFlush(empresa);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa con esa cédula jurídica o correo corporativo.");
        } catch (Exception e) {
            log.error("Error inesperado al registrar la configuración inicial de la empresa", e);
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar la empresa. Por favor, intenta nuevamente.");
        }

        usuario.setEmpresa(empresaGuardada);
        usuarioRepository.saveAndFlush(usuario);

        ConfiguracionInicialEmpresaResponseDTO response = empresaMapper.toDto(empresaGuardada);
        response.setDocumentosPendientes(true);
        response.setRecienCreada(true);
        return response;
    }

    private String generarSlugUnico(String nombreEmpresa) {
        String base = Empresa.generarSlug(nombreEmpresa);
        String slug = base;
        int sufijo = 2;
        while (empresaRepository.existsBySlug(slug)) {
            slug = base + "-" + sufijo;
            sufijo++;
        }
        return slug;
    }
}
