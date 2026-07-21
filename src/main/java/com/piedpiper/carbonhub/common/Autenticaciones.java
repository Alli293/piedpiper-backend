package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.exceptions.ApiException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class Autenticaciones {

    private Autenticaciones() {
    }

    public static UUID usuarioId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.accesoDenegado("No se pudo identificar al usuario autenticado.");
        }
    }
}
