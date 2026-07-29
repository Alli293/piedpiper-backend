package com.piedpiper.carbonhub.insignia.models.entities;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "catalogo_insignias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_catalogo_insignias_id_nivel",
                columnNames = {"id_insignia", "nivel_insignia"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoInsignia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_insignia", nullable = false)
    private Long idInsignia;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "nivel_insignia", nullable = false, length = 20)
    private String nivelInsignia;

    @Column(name = "cantidad_minima_certificaciones_activas", nullable = false)
    private Integer cantidadMinimaCertificacionesActivas;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "catalogo_insignias_tipos_certificacion",
            joinColumns = @JoinColumn(name = "catalogo_insignia_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificacion", nullable = false, length = 40)
    @Builder.Default
    private Set<TipoCertificacion> tiposCertificacionesRequeridas = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
}
