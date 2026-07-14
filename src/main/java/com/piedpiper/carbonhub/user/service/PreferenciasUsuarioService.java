package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.UnidadesMedida;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PreferenciasUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(PreferenciasUsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public PreferenciasUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lee las preferencias almacenadas en el perfil. Si un valor almacenado
     * dejó de estar soportado, se resuelve al valor por defecto sin error.
     */
    @Transactional(readOnly = true)
    public PreferenciasUsuarioResponseDTO obtenerPreferencias(UUID usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return PreferenciasUsuarioResponseDTO.de(
                Idioma.desde(usuario.getIdioma()).orElse(Idioma.POR_DEFECTO),
                Moneda.desde(usuario.getMoneda()).orElse(Moneda.POR_DEFECTO),
                UnidadesMedida.desde(usuario.getUnidades()).orElse(UnidadesMedida.POR_DEFECTO));
    }

    /**
     * Persiste las preferencias del usuario. Solo acepta valores del catálogo
     * soportado; un valor fuera de catálogo se rechaza con 422.
     */
    @Transactional
    public PreferenciasUsuarioResponseDTO actualizarPreferencias(
            UUID usuarioId, PreferenciasUsuarioRequestDTO request) {
        Usuario usuario = buscarUsuario(usuarioId);

        Idioma idioma = Idioma.desde(request.getIdioma())
                .orElseThrow(() -> ApiException.valorNoSoportado(
                        "El idioma seleccionado no está soportado."));
        Moneda moneda = Moneda.desde(request.getMoneda())
                .orElseThrow(() -> ApiException.valorNoSoportado(
                        "La moneda seleccionada no está soportada."));
        UnidadesMedida unidades = UnidadesMedida.desde(request.getUnidades())
                .orElseThrow(() -> ApiException.valorNoSoportado(
                        "El sistema de unidades seleccionado no está soportado."));

        usuario.setIdioma(idioma.name());
        usuario.setMoneda(moneda.name());
        usuario.setUnidades(unidades.name());

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (Exception e) {
            log.error("Error inesperado al guardar las preferencias del usuario {}", usuarioId, e);
            throw ApiException.errorInterno(
                    "No se pudieron guardar tus preferencias. Intenta nuevamente.");
        }

        return PreferenciasUsuarioResponseDTO.de(idioma, moneda, unidades);
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno(
                        "No se pudo identificar al usuario autenticado."));
    }
}
