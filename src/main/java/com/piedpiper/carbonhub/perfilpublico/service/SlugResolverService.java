package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.exceptions.SlugCambiadoException;
import com.piedpiper.carbonhub.perfilpublico.models.entities.SlugHistorico;
import com.piedpiper.carbonhub.perfilpublico.repository.SlugHistoricoRepository;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolvedor centralizado de slugs para todos los endpoints del perfil público.
 * Normaliza, valida formato, busca empresa activa, y lanza SlugCambiadoException
 * si el slug es histórico (para que el handler devuelva 301).
 */
@Service
public class SlugResolverService {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9-]{1,120}$");

    private final EmpresaRepository empresaRepository;
    private final SlugHistoricoRepository slugHistoricoRepository;

    public SlugResolverService(EmpresaRepository empresaRepository,
                               SlugHistoricoRepository slugHistoricoRepository) {
        this.empresaRepository = empresaRepository;
        this.slugHistoricoRepository = slugHistoricoRepository;
    }

    /**
     * Resuelve un slug a la empresa activa correspondiente.
     * Si el slug es histórico, lanza SlugCambiadoException (301).
     * Si no existe, lanza PerfilNoEncontradoException (404).
     */
    public Empresa resolver(String slugOriginal) {
        String slug = normalizar(slugOriginal);
        validarFormato(slug);

        Optional<Empresa> empresaOpt = empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO);

        if (empresaOpt.isPresent()) {
            return empresaOpt.get();
        }

        // Buscar en historial de slugs
        Optional<SlugHistorico> historicoOpt = slugHistoricoRepository.findBySlugAnterior(slug);
        if (historicoOpt.isPresent()) {
            SlugHistorico historico = historicoOpt.get();
            Optional<Empresa> empresaPorId = empresaRepository.findById(historico.getEmpresaId());

            if (empresaPorId.isPresent()
                    && empresaPorId.get().getEstado() == EstadoEmpresa.ACTIVO
                    && !empresaPorId.get().getSlug().equals(slug)) {
                throw new SlugCambiadoException(empresaPorId.get().getSlug());
            }
        }

        throw new PerfilNoEncontradoException(
                "El perfil que buscas no existe o ya no está disponible.");
    }

    private String normalizar(String slugOriginal) {
        if (slugOriginal == null) return "";
        return slugOriginal.trim().toLowerCase(Locale.ROOT);
    }

    private void validarFormato(String slug) {
        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new PerfilNoEncontradoException(
                    "El perfil que buscas no existe o ya no está disponible.");
        }
    }
}
