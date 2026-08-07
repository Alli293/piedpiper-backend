package com.piedpiper.carbonhub.perfilpublico.exceptions;

public class SlugCambiadoException extends RuntimeException {

    private final String slugVigente;

    public SlugCambiadoException(String slugVigente) {
        super("Slug cambiado, redirigir a: " + slugVigente);
        this.slugVigente = slugVigente;
    }

    public String getSlugVigente() {
        return slugVigente;
    }
}
