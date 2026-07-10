package com.piedpiper.carbonhub.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleClaims {

    private String sub;
    private String email;
    private boolean emailVerified;
    private String name;
    private String givenName;
    private String familyName;
}
