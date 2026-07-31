package com.piedpiper.carbonhub.certificacion.models.enums;

/**
 * Valores de la enumeracion {@code AchievementType} de OpenBadges 3.0.
 * El token es el literal exacto que exige la especificacion; no traducir.
 *
 * @see <a href="https://www.imsglobal.org/spec/ob/v3p0">OpenBadges 3.0</a>
 */
public enum TipoLogroOpenBadges {
    CERTIFICATE("Certificate"),
    CERTIFICATION("Certification"),
    QUALITY_ASSURANCE_CREDENTIAL("QualityAssuranceCredential");

    private final String token;

    TipoLogroOpenBadges(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
