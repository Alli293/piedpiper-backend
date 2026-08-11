package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilPublicoAuditorResponseDTO {

    private UUID auditorId;
    private String nombre;
    private String fotoPerfil;
    private String descripcionProfesional;
    private String provincia;
    private List<String> especialidades;
    private List<CertificacionPublicaDTO> certificaciones;
    private boolean disponible;
    private BigDecimal calificacionPromedio;
    private Integer totalResenas;
    private Integer auditoriasCompletadas;
    private BigDecimal tiempoPromedioRespuestaDias;
    private List<DistribucionSectorDTO> distribucionSectores;
    private List<ResenaVerificadaDTO> resenas;
}
