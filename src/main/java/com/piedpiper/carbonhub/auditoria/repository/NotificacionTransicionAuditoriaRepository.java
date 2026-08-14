package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.NotificacionTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Las transiciones de estado se hacen con {@code UPDATE} condicionales y no con lectura, cambio y
 * guardado: el {@code where} es lo que decide quien se queda con la notificacion. Con leer primero,
 * dos barridos simultaneos (o dos instancias) leerian la misma fila pendiente y la enviarian dos
 * veces. Es el mismo criterio de PP-71.
 */
public interface NotificacionTransicionAuditoriaRepository
        extends JpaRepository<NotificacionTransicionAuditoria, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificacionTransicionAuditoria n
               set n.intentosEnvio = n.intentosEnvio + 1
             where n.id = :notificacionId
               and n.estado = :pendiente
               and n.intentosEnvio < :intentosMaximos
            """)
    int reclamarParaEnvio(@Param("notificacionId") UUID notificacionId,
                          @Param("pendiente") EstadoNotificacionTransicion pendiente,
                          @Param("intentosMaximos") int intentosMaximos);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificacionTransicionAuditoria n
               set n.estado = :enviada, n.fechaEnvio = :fechaEnvio
             where n.id = :notificacionId
               and n.estado = :pendiente
            """)
    int marcarEnviada(@Param("notificacionId") UUID notificacionId,
                      @Param("enviada") EstadoNotificacionTransicion enviada,
                      @Param("pendiente") EstadoNotificacionTransicion pendiente,
                      @Param("fechaEnvio") Instant fechaEnvio);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificacionTransicionAuditoria n
               set n.estado = :fallida
             where n.id = :notificacionId
               and n.estado = :pendiente
               and n.intentosEnvio >= :intentosMaximos
            """)
    int marcarFallidaSiAgotoIntentos(@Param("notificacionId") UUID notificacionId,
                                     @Param("fallida") EstadoNotificacionTransicion fallida,
                                     @Param("pendiente") EstadoNotificacionTransicion pendiente,
                                     @Param("intentosMaximos") int intentosMaximos);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificacionTransicionAuditoria n
               set n.estado = :fallida, n.intentosEnvio = :intentosMaximos
             where n.id = :notificacionId
               and n.estado = :pendiente
            """)
    int marcarFallidaDefinitiva(@Param("notificacionId") UUID notificacionId,
                                @Param("fallida") EstadoNotificacionTransicion fallida,
                                @Param("pendiente") EstadoNotificacionTransicion pendiente,
                                @Param("intentosMaximos") int intentosMaximos);

    List<NotificacionTransicionAuditoria> findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaCreacionAsc(
            EstadoNotificacionTransicion estado, int intentosMaximos);
}
