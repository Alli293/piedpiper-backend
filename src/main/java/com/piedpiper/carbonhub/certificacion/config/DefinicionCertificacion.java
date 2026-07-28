package com.piedpiper.carbonhub.certificacion.config;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;

/**
 * Definicion inmutable de un tipo de certificacion: el nombre que aparece en la
 * credencial, su vigencia y como se representa en OpenBadges 3.0.
 */
public record DefinicionCertificacion(
        TipoCertificacion tipo,
        String nombre,
        String descripcion,
        int vigenciaMeses,
        TipoLogroOpenBadges tipoLogro,
        String criterio) {
}
