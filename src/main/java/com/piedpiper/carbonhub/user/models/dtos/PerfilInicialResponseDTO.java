package com.piedpiper.carbonhub.user.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilInicialResponseDTO {

    private String nombreVisible;
    private String nombre;
    private String apellidos;
    private PreferenciasUsuarioResponseDTO preferencias;
    private String rol;
    private boolean configuracionCompleta;
    private String redirect;
    private EmpresaPerfilResponseDTO empresa;
}
