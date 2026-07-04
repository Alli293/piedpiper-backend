package com.piedpiper.carbonhub.auth.google;

public record GoogleClaims(String sub, String email, boolean emailVerified, String name) {
}
