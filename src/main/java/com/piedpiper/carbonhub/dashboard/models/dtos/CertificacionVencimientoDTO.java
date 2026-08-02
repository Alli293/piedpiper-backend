package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Una certificacion que vence en un día concreto del calendario (PP-77). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificacionVencimientoDTO {
    private UUID id;
    private String nombre;

    /** '90_dias' | '30_dias' | '7_dias' (mismos códigos que {@code TipoAlerta}, PP-70). */
    private String urgencia;
}
