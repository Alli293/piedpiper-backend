package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicadorAmbientalDTO {

    private UUID empresaId;
    private List<CertificacionActivaDTO> certificacionesActivas;
    private Instant consultadoEn;
}
