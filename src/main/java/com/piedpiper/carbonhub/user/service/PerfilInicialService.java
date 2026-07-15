package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.auth.service.RedirectResolver;
import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.DatosEmpresaPerfilDTO;
import com.piedpiper.carbonhub.user.models.dtos.EmpresaPerfilResponseDTO;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialResponseDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.enums.UnidadesMedida;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PerfilInicialService {

    private static final Logger log = LoggerFactory.getLogger(PerfilInicialService.class);

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public PerfilInicialService(UsuarioRepository usuarioRepository,
                                EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public PerfilInicialResponseDTO obtener(UUID usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return construirRespuesta(usuario);
    }

    @Transactional
    public PerfilInicialResponseDTO completar(UUID usuarioId, PerfilInicialRequestDTO request) {
        Usuario usuario = buscarUsuario(usuarioId);

        if (request.getEmpresa() != null && usuario.getRol() != Rol.ADMINISTRADOR_EMPRESA) {
            throw ApiException.accesoDenegado(
                    "No puedes modificar los datos de la empresa con tu rol.");
        }
        if (request.getEmpresa() != null && usuario.getEmpresa() == null) {
            throw ApiException.accesoDenegado(
                    "Aún no tienes una empresa asociada. Completa primero el registro de la empresa.");
        }

        Optional<Idioma> idioma = Idioma.desde(request.getPreferencias().getIdioma());
        Optional<Moneda> moneda = Moneda.desde(request.getPreferencias().getMoneda());
        Optional<UnidadesMedida> unidades =
                UnidadesMedida.desde(request.getPreferencias().getUnidades());
        Optional<SectorIndustrial> sector = request.getEmpresa() != null
                ? Catalogos.desde(SectorIndustrial.class, request.getEmpresa().getSectorIndustrial())
                : Optional.empty();

        List<String> errores = new ArrayList<>();
        if (idioma.isEmpty()) {
            errores.add("El idioma seleccionado no está soportado.");
        }
        if (moneda.isEmpty()) {
            errores.add("La moneda seleccionada no está soportada.");
        }
        if (unidades.isEmpty()) {
            errores.add("El sistema de unidades seleccionado no está soportado.");
        }
        if (request.getEmpresa() != null && sector.isEmpty()) {
            errores.add("El sector industrial seleccionado no está soportado.");
        }
        if (!errores.isEmpty()) {
            throw ApiException.valorNoSoportado(String.join(" ", errores));
        }

        usuario.setNombreVisible(request.getNombreVisible().trim());
        usuario.setIdioma(idioma.get().name());
        usuario.setMoneda(moneda.get().name());
        usuario.setUnidades(unidades.get().name());
        usuario.setConfiguracionCompleta(true);

        Empresa empresa = null;
        if (request.getEmpresa() != null) {
            empresa = aplicarDatosEmpresa(usuario.getEmpresa(), request.getEmpresa(), sector.get());
        }

        try {
            if (empresa != null) {
                empresaRepository.saveAndFlush(empresa);
            }
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataAccessException e) {
            log.error("Error inesperado al guardar el perfil inicial del usuario {}", usuarioId, e);
            throw ApiException.errorInterno("No se pudo guardar tu perfil. Intenta nuevamente.");
        }

        return construirRespuesta(usuario);
    }

    private Empresa aplicarDatosEmpresa(Empresa empresa, DatosEmpresaPerfilDTO datos,
                                        SectorIndustrial sector) {
        empresa.setSectorIndustrial(sector);
        empresa.setPais(datos.getPais().trim());
        empresa.setCantidadEmpleados(datos.getCantidadEmpleados());
        return empresa;
    }

    private PerfilInicialResponseDTO construirRespuesta(Usuario usuario) {
        PreferenciasUsuarioResponseDTO preferencias = PreferenciasUsuarioResponseDTO.de(
                Idioma.desde(usuario.getIdioma()).orElse(Idioma.POR_DEFECTO),
                Moneda.desde(usuario.getMoneda()).orElse(Moneda.POR_DEFECTO),
                UnidadesMedida.desde(usuario.getUnidades()).orElse(UnidadesMedida.POR_DEFECTO));

        String nombreVisible = usuario.getNombreVisible() != null
                ? usuario.getNombreVisible()
                : usuario.getNombre();

        EmpresaPerfilResponseDTO empresa = null;
        if (usuario.getEmpresa() != null) {
            Empresa entidad = usuario.getEmpresa();
            empresa = new EmpresaPerfilResponseDTO(
                    entidad.getNombreEmpresa(),
                    entidad.getSectorIndustrial() != null ? entidad.getSectorIndustrial().name() : null,
                    entidad.getPais(),
                    entidad.getCantidadEmpleados());
        }

        return new PerfilInicialResponseDTO(
                nombreVisible,
                preferencias,
                usuario.getRol().name(),
                usuario.isConfiguracionCompleta(),
                RedirectResolver.paraUsuario(usuario),
                empresa);
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No se pudo identificar al usuario autenticado."));
    }
}
