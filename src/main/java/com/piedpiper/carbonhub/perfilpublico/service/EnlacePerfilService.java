package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
public class EnlacePerfilService {

    private final SlugResolverService slugResolver;
    private final QrGeneradorService qrGeneradorService;
    private final String baseUrl;
    private final String ogImagenFallback;

    public EnlacePerfilService(SlugResolverService slugResolver,
                               QrGeneradorService qrGeneradorService,
                               @Value("${app.perfil-publico.base-url}") String baseUrl,
                               @Value("${app.perfil-publico.og-imagen-fallback}") String ogImagenFallback) {
        this.slugResolver = slugResolver;
        this.qrGeneradorService = qrGeneradorService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.ogImagenFallback = ogImagenFallback;
    }

    @Transactional(readOnly = true)
    public EnlacePerfilDTO obtenerEnlacePerfil(String slugOriginal) {
        Empresa empresa = slugResolver.resolver(slugOriginal);

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
