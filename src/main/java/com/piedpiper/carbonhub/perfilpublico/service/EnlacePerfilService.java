package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.exceptions.SlugCambiadoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;
import com.piedpiper.carbonhub.perfilpublico.models.entities.SlugHistorico;
import com.piedpiper.carbonhub.perfilpublico.repository.SlugHistoricoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class EnlacePerfilService {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9-]{1,120}$");

    private final EmpresaRepository empresaRepository;
    private final SlugHistoricoRepository slugHistoricoRepository;
    private final QrGeneradorService qrGeneradorService;
    private final String baseUrl;
    private final String ogImagenFallback;

    public EnlacePerfilService(EmpresaRepository empresaRepository,
                               SlugHistoricoRepository slugHistoricoRepository,
                               QrGeneradorService qrGeneradorService,
                               @Value("${app.perfil-publico.base-url}") String baseUrl,
                               @Value("${app.perfil-publico.og-imagen-fallback}") String ogImagenFallback) {
        this.empresaRepository = empresaRepository;
        this.slugHistoricoRepository = slugHistoricoRepository;
        this.qrGeneradorService = qrGeneradorService;
        this.baseUrl = baseUrl;
        this.ogImagenFallback = ogImagenFallback;
    }

    @Transactional(readOnly = true)
    public EnlacePerfilDTO obtenerEnlacePerfil(String slugOriginal) {
        // 1. Normalizar slug a minúsculas
        String slug = slugOriginal == null ? "" : slugOriginal.toLowerCase();

        // 2. Validar formato con regex
        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new PerfilNoEncontradoException(
                    "El perfil que buscas no existe o ya no está disponible.");
        }

        // 3. Buscar empresa activa por slug
        Optional<Empresa> empresaOpt = empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO);

        if (empresaOpt.isEmpty()) {
            // 4. Buscar en historial de slugs
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

        // 5. Construir respuesta
        Empresa empresa = empresaOpt.get();

        String urlCanonica = baseUrl + "/empresa/" + empresa.getSlug() + "/reputacion";
        String qrBase64 = qrGeneradorService.generarQrBase64(urlCanonica);
        String ogTitulo = empresa.getNombreEmpresa() + " — Perfil de Reputación Ecológica | CarbonHub";
        String ogDescripcion = "Nivel ecológico: " + resolverNivel(empresa)
                + ". Consulta el desempeño ambiental verificado de " + empresa.getNombreEmpresa() + ".";
        String ogImagen = resolverOgImagen(empresa.getLogoUrl());
        String codigoIncrustar = generarCodigoIncrustar(urlCanonica, empresa.getNombreEmpresa());

        return new EnlacePerfilDTO(urlCanonica, codigoIncrustar, qrBase64, ogTitulo, ogDescripcion, ogImagen, urlCanonica);
    }

    private String resolverNivel(Empresa empresa) {
        if (empresa.getNivelEcologico() == null || empresa.getNivelEcologico().isBlank()) {
            return "Sin nivel";
        }
        return empresa.getNivelEcologico();
    }

    private String resolverOgImagen(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return ogImagenFallback;
        }
        return logoUrl;
    }

    private String generarCodigoIncrustar(String urlCanonica, String nombreEmpresa) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombreEmpresa);
        return "<a href=\"" + urlCanonica + "\" target=\"_blank\" rel=\"noopener\" "
                + "style=\"display:inline-block;padding:12px 20px;background:#1a5c3a;color:#fff;"
                + "font-family:system-ui,sans-serif;font-size:14px;border-radius:8px;text-decoration:none;\">"
                + nombreEscapado + " — Perfil verificado en CarbonHub</a>";
    }
}
