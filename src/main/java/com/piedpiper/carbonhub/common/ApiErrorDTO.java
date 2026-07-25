package com.piedpiper.carbonhub.common;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorDTO {

    private int status;
    private String message;
    private Instant timestamp;

    public static ApiErrorDTO of(int status, String message) {
        return new ApiErrorDTO(status, message, Instant.now());
    }
}
