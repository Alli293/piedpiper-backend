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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PreferenciasUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(PreferenciasUsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public PreferenciasUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PreferenciasUsuarioResponseDTO obtenerPreferencias(UUID usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return PreferenciasUsuarioResponseDTO.de(
                Idioma.desde(usuario.getIdioma()).orElse(Idioma.POR_DEFECTO),
                Moneda.desde(usuario.getMoneda()).orElse(Moneda.POR_DEFECTO),
                UnidadesMedida.desde(usuario.getUnidades()).orElse(UnidadesMedida.POR_DEFECTO));
    }

    @Transactional
    public PreferenciasUsuarioResponseDTO actualizarPreferencias(
            UUID usuarioId, PreferenciasUsuarioRequestDTO request) {
        Usuario usuario = buscarUsuario(usuarioId);

        Optional<Idioma> idioma = Idioma.desde(request.getIdioma());
        Optional<Moneda> moneda = Moneda.desde(request.getMoneda());
        Optional<UnidadesMedida> unidades = UnidadesMedida.desde(request.getUnidades());

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
        if (!errores.isEmpty()) {
            throw ApiException.valorNoSoportado(String.join(" ", errores));
        }

        usuario.setIdioma(idioma.get().name());
        usuario.setMoneda(moneda.get().name());
        usuario.setUnidades(unidades.get().name());

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataAccessException e) {
            log.error("Error inesperado al guardar las preferencias del usuario {}", usuarioId, e);
            throw ApiException.errorInterno(
                    "No se pudieron guardar tus preferencias. Intenta nuevamente.");
        }

        return PreferenciasUsuarioResponseDTO.de(idioma.get(), moneda.get(), unidades.get());
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No se pudo identificar al usuario autenticado."));
    }
}
