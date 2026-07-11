package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasResponseDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.Unidades;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gestiona las preferencias de interfaz del usuario autenticado (PP-31):
 * idioma, moneda y sistema de unidades.
 */
@Service
public class PreferenciasService {

    private final UsuarioRepository usuarioRepository;

    public PreferenciasService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve las preferencias del usuario. Si un valor almacenado ya no
     * pertenece al catálogo soportado (o es nulo), se resuelve al valor por
     * defecto sin producir error.
     */
    @Transactional(readOnly = true)
    public PreferenciasResponseDTO obtener(UUID usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return construirRespuesta(usuario);
    }

    /**
     * Actualiza las preferencias del usuario. Solo se persisten valores
     * pertenecientes al catálogo soportado; un valor fuera de catálogo se
     * rechaza con 422.
     */
    @Transactional
    public PreferenciasResponseDTO actualizar(UUID usuarioId, PreferenciasRequestDTO request) {
        Usuario usuario = buscarUsuario(usuarioId);

        Idioma idioma = Idioma.desdeValor(request.getIdioma())
                .orElseThrow(() -> ApiException.preferenciaInvalida("idioma"));
        Moneda moneda = Moneda.desdeValor(request.getMoneda())
                .orElseThrow(() -> ApiException.preferenciaInvalida("moneda"));
        Unidades unidades = Unidades.desdeValor(request.getUnidades())
                .orElseThrow(() -> ApiException.preferenciaInvalida("unidades"));

        usuario.setIdioma(idioma.name());
        usuario.setMoneda(moneda.name());
        usuario.setUnidades(unidades.name());
        usuarioRepository.save(usuario);

        return construirRespuesta(usuario);
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(ApiException::usuarioNoEncontrado);
    }

    private PreferenciasResponseDTO construirRespuesta(Usuario usuario) {
        return new PreferenciasResponseDTO(
                Idioma.desdeValor(usuario.getIdioma()).orElse(Idioma.POR_DEFECTO).name(),
                Moneda.desdeValor(usuario.getMoneda()).orElse(Moneda.POR_DEFECTO).name(),
                Unidades.desdeValor(usuario.getUnidades()).orElse(Unidades.POR_DEFECTO).name());
    }
}
