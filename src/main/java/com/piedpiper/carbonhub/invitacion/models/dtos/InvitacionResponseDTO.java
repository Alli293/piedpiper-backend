package com.piedpiper.carbonhub.invitacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionResponseDTO {

    private UUID id;
    private String email;
    private String estado;
    private Instant fechaEmision;
    private Instant fechaExpiracion;
}
