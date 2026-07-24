package com.piedpiper.carbonhub.reconocimiento.models.entities;

import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "eventos_reconocimiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_eventos_reconocimiento_usuario_evento",
                columnNames = {"usuario_id", "evento_generado"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoReconocimiento {

    public static final int EVENTO_GENERADO_MAX = 80;
    public static final int ERROR_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "evento_generado", nullable = false, length = EVENTO_GENERADO_MAX)
    private String eventoGenerado;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 40)
    private EstadoEnvioCertificacion estadoEnvio;

    @Column(name = "intentos_envio", nullable = false)
    @Builder.Default
    private int intentosEnvio = 0;

    @Column(name = "fecha_ultimo_intento")
    private Instant fechaUltimoIntento;

    @Column(name = "ultimo_error", length = ERROR_MAX)
    private String ultimoError;

    public void marcarEnviado(Instant fechaIntento) {
        estadoEnvio = EstadoEnvioCertificacion.ENVIADO;
        fechaUltimoIntento = fechaIntento;
        ultimoError = null;
    }

    public void marcarPendienteReintento(String mensajeError, Instant fechaIntento) {
        estadoEnvio = EstadoEnvioCertificacion.PENDIENTE_REINTENTO;
        fechaUltimoIntento = fechaIntento;
        intentosEnvio++;
        ultimoError = recortarError(mensajeError);
    }

    private String recortarError(String mensajeError) {
        if (mensajeError == null || mensajeError.isBlank()) {
            return "Error de comunicacion con Certificacion.";
        }
        return mensajeError.length() > ERROR_MAX ? mensajeError.substring(0, ERROR_MAX) : mensajeError;
    }
}
