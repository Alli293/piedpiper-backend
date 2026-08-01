package com.piedpiper.carbonhub.insignia.models.entities;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insignias_empresa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_insignias_empresa_empresa_insignia_nivel",
                columnNames = {"empresa_id", "id_insignia", "nivel_insignia"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "id_insignia", nullable = false)
    private Long idInsignia;

    @Column(name = "nivel_insignia", nullable = false, length = 20)
    private String nivelInsignia;

    @Column(name = "fecha_obtencion", nullable = false)
    private Instant fechaObtencion;
}
