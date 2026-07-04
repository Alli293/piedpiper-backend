package com.piedpiper.carbonhub.auditor;

import com.piedpiper.carbonhub.user.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "perfiles_auditor")
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
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "numero_certificacion", nullable = false, length = 50)
    private String numeroCertificacion;

    @Column(name = "entidad_certificadora", nullable = false, length = 100)
    private String entidadCertificadora;

    @Column(name = "fecha_vigencia_cert", nullable = false)
    private LocalDate fechaVigenciaCert;

    @Column(name = "anios_experiencia", nullable = false)
    private int aniosExperiencia;

    @Column(name = "doc_certificado_path", nullable = false)
    private String docCertificadoPath;

    @Column(name = "doc_identificacion_path", nullable = false)
    private String docIdentificacionPath;
}
