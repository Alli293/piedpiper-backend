package com.piedpiper.carbonhub.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class EmailProviderProductionGuard {

    private final String provider;

    public EmailProviderProductionGuard(@Value("${app.email.provider:stub}") String provider) {
        this.provider = provider;
    }

    @PostConstruct
    void validar() {
        if (!"gmail".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "El perfil prod requiere un proveedor de correo real; 'stub' no esta permitido.");
        }
    }
}
