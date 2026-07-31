package com.piedpiper.carbonhub.certificacion.models.entities;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Todavia no existe una entidad Auditoria en el backend, asi que se guarda
     * como columna simple con restriccion de unicidad (que es la que garantiza
     * la no duplicacion de certificaciones por auditoria). Cuando el dominio de
     * auditorias exista, este campo pasa a ser una relacion real conservando el
     * mismo nombre de columna, de modo que el cambio sea solo de restriccion.
     */
    @Column(name = "id_auditoria", nullable = false, unique = true)
    private UUID idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_id", nullable = false)
    private Usuario auditor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoCertificacion tipo;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    /**
     * Fecha de calendario, no instante: la vigencia de una certificacion se
     * cuenta en dias completos y no depende de la hora de emision.
     */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCertificacion estado;

    /**
     * Credencial OpenBadges 3.0 firmada, en formato JWT compacto (VC-JWT).
     * Se guarda en la fila para que la emision sea una sola transaccion atomica.
     */
    @Column(name = "credencial_jwt", nullable = false, columnDefinition = "text")
    private String credencialJwt;

    /**
     * Posicion de esta certificacion dentro de la lista de estado de revocacion
     * (Bitstring Status List, W3C). Se asigna al emitir (ver
     * {@code IndiceEstadoCertificacion}) y va embebido en la credencial firmada,
     * de modo que sea revocable en el futuro sin reemitir nada: hoy nada usa
     * este indice para revocar (no existe todavia la accion de revocar), pero
     * omitirlo ahora dejaria permanentemente no revocables todas las
     * certificaciones emitidas antes de agregar esa funcionalidad, porque el
     * campo debe estar en la credencial desde el momento de la firma.
     */
    @Column(name = "indice_estado", nullable = false, unique = true)
    private Long indiceEstado;
}
