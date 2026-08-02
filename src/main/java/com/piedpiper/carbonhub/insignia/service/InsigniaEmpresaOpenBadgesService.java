package com.piedpiper.carbonhub.insignia.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.service.FirmanteCredencialService;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCredencialOpenBadges;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class InsigniaEmpresaOpenBadgesService {

    private static final Pattern NO_ARCHIVO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern GUIONES_EXTREMOS = Pattern.compile("^-+|-+$");

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final CatalogoInsigniaRepository catalogoInsigniaRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    private final FirmanteCredencialService firmanteCredencialService;
    private final String urlBase;
    private final String emisorNombre;

    public InsigniaEmpresaOpenBadgesService(
            InsigniaEmpresaRepository insigniaEmpresaRepository,
            CatalogoInsigniaRepository catalogoInsigniaRepository,
            EmisionEmpresaService emisionEmpresaService,
            GeneradorCredencialOpenBadges generadorCredencialOpenBadges,
            FirmanteCredencialService firmanteCredencialService,
            @Value("${certificaciones.emision.url-base:http://localhost:8080}") String urlBase,
            @Value("${certificaciones.emision.emisor-nombre:CarbonHub}") String emisorNombre) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.generadorCredencialOpenBadges = generadorCredencialOpenBadges;
        this.firmanteCredencialService = firmanteCredencialService;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.emisorNombre = emisorNombre;
    }

    @Transactional(readOnly = true)
    public DocumentoInsigniaOpenBadges generarParaEmpresaAutenticada(UUID usuarioId, UUID idInsigniaEmpresa) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        InsigniaEmpresa insigniaEmpresa = buscarInsigniaPropia(idInsigniaEmpresa, empresaId);
        CatalogoInsignia catalogo = buscarCatalogo(insigniaEmpresa);
        Map<String, Object> credencial = construirCredencial(insigniaEmpresa, catalogo);
        return new DocumentoInsigniaOpenBadges(nombreArchivo(insigniaEmpresa, catalogo), credencial);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generarPublica(UUID idInsigniaEmpresa) {
        InsigniaEmpresa insigniaEmpresa = insigniaEmpresaRepository.findById(idInsigniaEmpresa)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La insignia no existe."));
        return construirCredencial(insigniaEmpresa, buscarCatalogo(insigniaEmpresa));
    }

    @Transactional(readOnly = true)
    public String generarJwtPublico(UUID idInsigniaEmpresa) {
        InsigniaEmpresa insigniaEmpresa = insigniaEmpresaRepository.findById(idInsigniaEmpresa)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La insignia no existe."));
        return firmarCredencial(insigniaEmpresa, buscarCatalogo(insigniaEmpresa));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generarLogro(Long idInsignia, String nivelInsignia) {
        CatalogoInsignia catalogo = catalogoInsigniaRepository
                .findByIdInsigniaAndNivelInsigniaAndActivaTrue(idInsignia, nivelInsignia)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("El logro de la insignia no existe."));
        return construirLogro(idInsignia, catalogo);
    }

    public String urlVerificacionPublica(UUID idInsigniaEmpresa) {
        return urlBase + "/api/insignias/" + idInsigniaEmpresa + "/verificacion";
    }

    public String urlVerificacionJwt(UUID idInsigniaEmpresa) {
        return urlBase + "/api/insignias/" + idInsigniaEmpresa + "/verificacion.jwt";
    }

    public String urlLinkedIn(InsigniaEmpresa insigniaEmpresa, CatalogoInsignia catalogo) {
        return UriComponentsBuilder.fromUriString("https://www.linkedin.com/profile/add")
                .queryParam("startTask", "CERTIFICATION_NAME")
                .queryParam("name", nombreCredencial(catalogo))
                .queryParam("organizationName", emisorNombre)
                .queryParam("issueYear", insigniaEmpresa.getFechaObtencion().atZone(java.time.ZoneOffset.UTC).getYear())
                .queryParam("issueMonth", insigniaEmpresa.getFechaObtencion().atZone(java.time.ZoneOffset.UTC).getMonthValue())
                .queryParam("certId", insigniaEmpresa.getId().toString())
                .queryParam("certUrl", urlVerificacionJwt(insigniaEmpresa.getId()))
                .build()
                .encode()
                .toUriString();
    }

    public String criterios(CatalogoInsignia catalogo) {
        String cantidad = catalogo.getCantidadMinimaCertificacionesActivas() == 1
                ? "1 certificacion activa"
                : catalogo.getCantidadMinimaCertificacionesActivas() + " certificaciones activas";
        if (catalogo.getTiposCertificacionesRequeridas().isEmpty()) {
            return "La empresa debe mantener al menos " + cantidad + " validada por CarbonHub.";
        }

        String tipos = catalogo.getTiposCertificacionesRequeridas().stream()
                .sorted(Comparator.comparing(TipoCertificacion::name))
                .map(TipoCertificacion::getCodigo)
                .map(codigo -> codigo.replace('_', ' '))
                .toList()
                .stream()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return "La empresa debe mantener al menos " + cantidad
                + " y cumplir estos tipos de certificacion: " + tipos + ".";
    }

    public String emisorNombre() {
        return emisorNombre;
    }

    private InsigniaEmpresa buscarInsigniaPropia(UUID idInsigniaEmpresa, UUID empresaId) {
        InsigniaEmpresa insigniaEmpresa = insigniaEmpresaRepository.findById(idInsigniaEmpresa)
                .orElseThrow(() -> ApiException.accesoDenegado("No tiene permiso para descargar esta insignia."));
        if (!insigniaEmpresa.getEmpresa().getId().equals(empresaId)) {
            throw ApiException.accesoDenegado("No tiene permiso para descargar esta insignia.");
        }
        return insigniaEmpresa;
    }

    private CatalogoInsignia buscarCatalogo(InsigniaEmpresa insigniaEmpresa) {
        return catalogoInsigniaRepository.findByIdInsigniaAndNivelInsigniaAndActivaTrue(
                        insigniaEmpresa.getIdInsignia(), insigniaEmpresa.getNivelInsignia())
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No fue posible encontrar la definicion de la insignia."));
    }

    private Map<String, Object> construirCredencial(InsigniaEmpresa insigniaEmpresa,
                                                    CatalogoInsignia catalogo) {
        Map<String, Object> credencial = new LinkedHashMap<>();
        credencial.put("@context", GeneradorCredencialOpenBadges.contexto());
        credencial.put("id", urlVerificacionPublica(insigniaEmpresa.getId()));
        credencial.put("type", List.of("VerifiableCredential", "OpenBadgeCredential"));
        credencial.put("name", nombreCredencial(catalogo));
        credencial.put("description", catalogo.getDescripcion());
        credencial.put("issuer", generadorCredencialOpenBadges.construirEmisor());
        credencial.put("validFrom", insigniaEmpresa.getFechaObtencion().toString());
        credencial.put("credentialSubject", construirSujeto(insigniaEmpresa, catalogo));
        return credencial;
    }

    private String firmarCredencial(InsigniaEmpresa insigniaEmpresa, CatalogoInsignia catalogo) {
        Map<String, Object> credencial = construirCredencial(insigniaEmpresa, catalogo);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(generadorCredencialOpenBadges.urlEmisor())
                .subject(urlEmpresa(insigniaEmpresa.getEmpresa()))
                .jwtID(urlVerificacionJwt(insigniaEmpresa.getId()))
                .notBeforeTime(Date.from(insigniaEmpresa.getFechaObtencion()))
                .issueTime(Date.from(insigniaEmpresa.getFechaObtencion()))
                .claim("vc", credencial)
                .build();
        return firmanteCredencialService.firmar(claims);
    }

    private Map<String, Object> construirSujeto(InsigniaEmpresa insigniaEmpresa,
                                                CatalogoInsignia catalogo) {
        Empresa empresa = insigniaEmpresa.getEmpresa();
        Map<String, Object> sujeto = new LinkedHashMap<>();
        sujeto.put("id", urlEmpresa(empresa));
        sujeto.put("type", "AchievementSubject");
        sujeto.put("name", empresa.getNombreEmpresa());
        sujeto.put("achievement", construirLogro(insigniaEmpresa.getIdInsignia(), catalogo));
        return sujeto;
    }

    private Map<String, Object> construirLogro(Long idInsignia, CatalogoInsignia catalogo) {
        Map<String, Object> criterio = new LinkedHashMap<>();
        criterio.put("narrative", criterios(catalogo));

        Map<String, Object> logro = new LinkedHashMap<>();
        logro.put("id", urlLogro(idInsignia, catalogo.getNivelInsignia()));
        logro.put("type", "Achievement");
        logro.put("achievementType", "Badge");
        logro.put("name", nombreCredencial(catalogo));
        logro.put("description", catalogo.getDescripcion());
        logro.put("criteria", criterio);
        logro.put("tag", List.of("nivel " + nivelLabel(catalogo.getNivelInsignia())));
        return logro;
    }

    private String nombreCredencial(CatalogoInsignia catalogo) {
        return catalogo.getNombre() + " - " + nivelLabel(catalogo.getNivelInsignia());
    }

    private String nivelLabel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            return "Insignia";
        }
        return nivel.substring(0, 1).toUpperCase(Locale.ROOT)
                + nivel.substring(1).toLowerCase(Locale.ROOT);
    }

    private String urlEmpresa(Empresa empresa) {
        return urlBase + "/api/perfil-publico/" + empresa.getSlug();
    }

    private String urlLogro(Long idInsignia, String nivelInsignia) {
        return urlBase + "/api/insignias/logros/" + idInsignia + "/" + nivelInsignia;
    }

    private String nombreArchivo(InsigniaEmpresa insigniaEmpresa, CatalogoInsignia catalogo) {
        String base = Normalizer.normalize(catalogo.getNombre() + "-" + catalogo.getNivelInsignia(),
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String seguro = GUIONES_EXTREMOS.matcher(NO_ARCHIVO.matcher(base).replaceAll("-")).replaceAll("");
        return seguro + "-" + insigniaEmpresa.getId() + ".jsonld";
    }

    public record DocumentoInsigniaOpenBadges(String nombreArchivo, Map<String, Object> contenido) {
    }
}
