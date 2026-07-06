package com.piedpiper.carbonhub.empresa.models.entities;

import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "sector_industrial", nullable = false, length = 40)
    private SectorIndustrial sectorIndustrial;

    @Column(nullable = false, length = 2)
    private String pais;

    @Column(name = "cantidad_empleados", nullable = false)
    private int cantidadEmpleados;

    @Column(name = "correo_corporativo", nullable = false, unique = true, length = 254)
    private String correoCorporativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEmpresa estado;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
}
