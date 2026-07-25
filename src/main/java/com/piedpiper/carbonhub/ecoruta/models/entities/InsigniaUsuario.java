package com.piedpiper.carbonhub.ecoruta.models.entities;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
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
@Table(name = "insignias_usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_insignias_usuario_usuario_insignia",
                columnNames = {"usuario_id", "id_insignia"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "id_insignia", nullable = false)
    private Long idInsignia;

    @Column(name = "evento_desbloqueo", nullable = false, length = 80)
    private String eventoDesbloqueo;

    @Column(name = "fecha_obtencion", nullable = false)
    private Instant fechaObtencion;
}
