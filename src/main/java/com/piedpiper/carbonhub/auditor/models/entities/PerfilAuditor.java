package com.piedpiper.carbonhub.auditor.models.entities;

import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "perfiles_auditor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_perfiles_auditor_auditor", columnNames = "auditor_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilAuditor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_id", nullable = false)
    private Usuario auditor;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(nullable = false)
    @Builder.Default
    private boolean disponible = true;

    @Column(name = "auditorias_completadas")
    private Integer auditoriasCompletadas;

    @Column(name = "calificacion_promedio", precision = 2, scale = 1)
    private BigDecimal calificacionPromedio;

    @Column(name = "total_resenas", nullable = false)
    @Builder.Default
    private int totalResenas = 0;

    @Column(name = "tiempo_respuesta_horas")
    private Integer tiempoRespuestaHoras;

    @Column(name = "tiempo_promedio_respuesta_dias", precision = 5, scale = 1)
    private BigDecimal tiempoPromedioRespuestaDias;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "perfil_auditor_distribucion_sectores",
            joinColumns = @JoinColumn(name = "perfil_auditor_id"))
    @OrderColumn(name = "orden", nullable = false, columnDefinition = "integer default 0")
    @BatchSize(size = 50)
    @Builder.Default
    private List<DistribucionSectorAuditor> distribucionSectores = new ArrayList<>();

    @Column(name = "anios_experiencia")
    private Integer aniosExperiencia;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProvinciaCR provincia;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "perfil_auditor_especialidades", joinColumns = @JoinColumn(name = "perfil_auditor_id"))
    @Column(name = "especialidad", length = 30)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<EspecialidadAuditor> especialidades = new HashSet<>();

    @ElementCollection(targetClass = ProvinciaCR.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "perfil_auditor_zonas", joinColumns = @JoinColumn(name = "perfil_auditor_id"))
    @Column(name = "zona")
    @Builder.Default
    private Set<ProvinciaCR> zonasCobertura = new HashSet<>();

    @Column(name = "descripcion_profesional", length = 500)
    private String descripcionProfesional;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;
}
