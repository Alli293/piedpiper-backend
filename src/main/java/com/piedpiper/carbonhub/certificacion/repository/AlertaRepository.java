package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    boolean existsByCertificacionIdAndTipoAlerta(UUID certificacionId, TipoAlerta tipoAlerta);

    /**
     * Trae la alerta con su empresa y su certificacion en la misma consulta. El fetch join es
     * necesario porque las dos relaciones son LAZY y el correo se arma fuera de la transaccion
     * que la leyo (PP-71).
     */
    @Query("""
            select a from Alerta a
            join fetch a.empresa
            join fetch a.certificacion
            where a.id = :alertaId
            """)
    Optional<Alerta> buscarConEmpresaYCertificacion(UUID alertaId);

    /**
     * Alertas cuyo correo todavia no salio y que no agotaron los reintentos, para el barrido de
     * recuperacion. Las mas viejas primero.
     *
     * <p>Va acotado a 50 por corrida a proposito: el pool del scheduler de Spring es de un solo hilo
     * y cada reintento es un envio SMTP sincronico, asi que un lote grande (por ejemplo el SMTP caido
     * durante una noche con muchas certificaciones) bloquearia a los otros procesos programados. Lo
     * que no entra en una corrida se toma en la siguiente, cinco minutos despues.</p>
     */
    List<Alerta> findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(EstadoAlerta estado,
                                                                                   int intentosMaximos);

    /**
     * Reclama la alerta para enviarla: sube el contador de intentos en una sola sentencia y solo si
     * sigue pendiente y le quedan intentos. Devuelve 1 si la reclamo y 0 si no.
     *
     * <p>Este update es el que hace de exclusion mutua. Sin el, dos procesos que leyeran la misma
     * alerta pendiente enviarian los dos el correo, y el contador leido-modificado-escrito perderia
     * incrementos. Con el, la base decide quien la toma: el que consigue 1 fila envia, el que
     * consigue 0 se sale.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Alerta a
               set a.intentosEnvio = a.intentosEnvio + 1
             where a.id = :alertaId
               and a.estado = :pendiente
               and a.intentosEnvio < :intentosMaximos
            """)
    int reclamarParaEnvio(UUID alertaId, EstadoAlerta pendiente, int intentosMaximos);

    /** Cierra la alerta como enviada, solo si nadie la cerro antes. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Alerta a
               set a.estado = :enviada, a.fechaEnvio = :fechaEnvio
             where a.id = :alertaId
               and a.estado = :pendiente
            """)
    int marcarEnviada(UUID alertaId, EstadoAlerta enviada, EstadoAlerta pendiente, Instant fechaEnvio);

    /**
     * Cierra la alerta como fallida solo si ya agoto los intentos. Si todavia le quedan no toca
     * nada y devuelve 0, con lo que sigue pendiente y el barrido la retoma.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Alerta a
               set a.estado = :fallida
             where a.id = :alertaId
               and a.estado = :pendiente
               and a.intentosEnvio >= :intentosMaximos
            """)
    int marcarFallidaSiAgotoIntentos(UUID alertaId, EstadoAlerta fallida, EstadoAlerta pendiente,
                                     int intentosMaximos);

    /** Cierra la alerta como fallida sin importar los intentos, para fallos que no vale reintentar. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Alerta a
               set a.estado = :fallida, a.intentosEnvio = :intentosMaximos
             where a.id = :alertaId
               and a.estado = :pendiente
            """)
    int marcarFallidaDefinitiva(UUID alertaId, EstadoAlerta fallida, EstadoAlerta pendiente,
                                int intentosMaximos);
}
