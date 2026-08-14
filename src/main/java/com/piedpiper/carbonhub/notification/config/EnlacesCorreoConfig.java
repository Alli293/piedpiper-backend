package com.piedpiper.carbonhub.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EnlacesCorreoConfig {

    private final Set<String> hostsPermitidos;
    private final EnlaceConfigurado[] enlaces;

    public EnlacesCorreoConfig(
            @Value("${frontend.allowed-link-hosts}") String hostsPermitidos,
            @Value("${frontend.verificar-correo-url}") String verificarCorreoUrl,
            @Value("${frontend.invitacion-url}") String invitacionUrl,
            @Value("${frontend.reset-contrasena-url}") String resetContrasenaUrl,
            @Value("${frontend.login-url}") String loginUrl,
            @Value("${frontend.certificacion-detalle-url}") String certificacionDetalleUrl,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.hostsPermitidos = Arrays.stream(hostsPermitidos.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.enlaces = new EnlaceConfigurado[] {
                new EnlaceConfigurado("frontend.verificar-correo-url", verificarCorreoUrl),
                new EnlaceConfigurado("frontend.invitacion-url", invitacionUrl),
                new EnlaceConfigurado("frontend.reset-contrasena-url", resetContrasenaUrl),
                new EnlaceConfigurado("frontend.login-url", loginUrl),
                new EnlaceConfigurado("frontend.certificacion-detalle-url", certificacionDetalleUrl),
                new EnlaceConfigurado("app.frontend-url", frontendUrl)
        };
    }

    @PostConstruct
    public void validar() {
        if (hostsPermitidos.isEmpty()) {
            throw new IllegalStateException("frontend.allowed-link-hosts debe declarar al menos un host.");
        }
        Arrays.stream(enlaces).forEach(this::validar);
    }

    private void validar(EnlaceConfigurado enlace) {
        URI uri;
        try {
            uri = URI.create(enlace.url().replace("{slug}", "slug-seguro"));
        } catch (IllegalArgumentException e) {
            throw invalido(enlace, "no es una URL valida");
        }

        String host = uri.getHost();
        if (!uri.isAbsolute() || host == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw invalido(enlace, "debe ser absoluta y no incluir credenciales, query ni fragmento");
        }

        String hostNormalizado = host.toLowerCase(Locale.ROOT);
        if (!hostsPermitidos.contains(hostNormalizado)) {
            throw invalido(enlace, "usa un host que no esta permitido");
        }

        boolean local = hostNormalizado.equals("localhost")
                || hostNormalizado.equals("127.0.0.1")
                || hostNormalizado.equals("::1");
        boolean esquemaSeguro = uri.getScheme().equalsIgnoreCase("https")
                || (local && uri.getScheme().equalsIgnoreCase("http"));
        if (!esquemaSeguro) {
            throw invalido(enlace, "debe usar HTTPS fuera del entorno local");
        }
    }

    private IllegalStateException invalido(EnlaceConfigurado enlace, String detalle) {
        return new IllegalStateException("La propiedad %s %s.".formatted(enlace.propiedad(), detalle));
    }

    private record EnlaceConfigurado(String propiedad, String url) {
    }
}
