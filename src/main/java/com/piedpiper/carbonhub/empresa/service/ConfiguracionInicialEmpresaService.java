package com.piedpiper.carbonhub.empresa.service;

import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaRequestDTO;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConfiguracionInicialEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public ConfiguracionInicialEmpresaService(EmpresaRepository empresaRepository,
                                              UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ConfiguracionInicialEmpresaResponseDTO completarPaso2(
            UUID usuarioId, ConfiguracionInicialEmpresaRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno(
                        "No se pudo identificar al usuario autenticado."));

        if (usuario.getRol() != Rol.ADMINISTRADOR_EMPRESA) {
            throw ApiException.accesoDenegado(
                    "Solo el administrador de una empresa puede completar este paso.");
        }

        if (usuario.getEmpresa() != null) {
            throw ApiException.cuentaDuplicada(
                    "Ya completaste la configuración inicial de tu empresa.");
        }

        if (empresaRepository.existsByCedulaJuridica(request.getCedulaJuridica())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con esta cédula jurídica.");
        }

        String correoCorporativo = usuario.getEmail();
        if (empresaRepository.existsByCorreoCorporativo(correoCorporativo)) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con este correo corporativo.");
        }

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
        Empresa empresaGuardada = empresaRepository.saveAndFlush(empresa);

        usuario.setEmpresa(empresaGuardada);
        // TODO: no marcar configuracionCompleta=true todavia -- falta el Paso 3
        // (subida de documentos: cedula juridica y personeria juridica) antes de
        // considerar la configuracion inicial 100% completa.
        usuarioRepository.saveAndFlush(usuario);

        return new ConfiguracionInicialEmpresaResponseDTO(
                empresaGuardada.getId(), empresaGuardada.getNombreEmpresa(), slug, true);
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
