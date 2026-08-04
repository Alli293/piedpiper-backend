package com.piedpiper.carbonhub.insignia.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaEmpresaResponseDTO {

    private UUID idInsigniaEmpresa;
    private Long idInsignia;
    private String nivelInsignia;
    private String nombre;
    private String descripcion;
    private Instant fechaObtencion;
    private String criteriosObtencion;
    private String emisor;
    private String receptor;
    private String urlVerificacionPublica;
    private String urlVerificacionJwt;
    private String urlLinkedIn;

    public InsigniaEmpresaResponseDTO(Long idInsignia,
                                      String nivelInsignia,
                                      String nombre,
                                      String descripcion,
                                      Instant fechaObtencion) {
        this.idInsignia = idInsignia;
        this.nivelInsignia = nivelInsignia;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaObtencion = fechaObtencion;
    }
}
