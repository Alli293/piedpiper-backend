package com.piedpiper.carbonhub.limite.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "limites_emisiones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_limites_emisiones_empresa_anio",
                columnNames = {"empresa_id", "anio"}
        )
)
public class LimiteEmisiones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "limite_mt", nullable = false, precision = 16, scale = 4)
    private BigDecimal limiteMt;

    @Column(length = 500)
    private String justificacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public LimiteEmisiones() {
    }

    public LimiteEmisiones(Long empresaId, Integer anio, BigDecimal limiteMt) {
        this(empresaId, anio, limiteMt, null);
    }

    public LimiteEmisiones(Long empresaId, Integer anio, BigDecimal limiteMt, String justificacion) {
        this.empresaId = empresaId;
        this.anio = anio;
        this.limiteMt = limiteMt;
        this.justificacion = justificacion;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public BigDecimal getLimiteMt() {
        return limiteMt;
    }

    public void setLimiteMt(BigDecimal limiteMt) {
        this.limiteMt = limiteMt;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
