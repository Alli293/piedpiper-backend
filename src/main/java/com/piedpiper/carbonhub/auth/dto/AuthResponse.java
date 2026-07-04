package com.piedpiper.carbonhub.auth.dto;

public record AuthResponse(String token, String rol, String estado, String redirect) {
}
