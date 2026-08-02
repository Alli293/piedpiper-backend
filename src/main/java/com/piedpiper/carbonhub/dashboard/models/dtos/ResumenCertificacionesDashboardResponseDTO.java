package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCertificacionesDashboardResponseDTO {
    private int activas;
    private int proximasAVencer;
    private int vencidas;
}
