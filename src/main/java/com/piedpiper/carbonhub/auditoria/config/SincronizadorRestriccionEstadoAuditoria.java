package com.piedpiper.carbonhub.auditoria.config;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Realinea la restriccion CHECK de {@code solicitudes_auditoria.estado} con los valores del enum.
 *
 * <p><strong>Por que existe.</strong> El proyecto corre con {@code ddl-auto=update} y sin
 * herramienta de migraciones. Hibernate crea la restriccion CHECK de una columna enum cuando crea
 * la columna, pero <em>nunca la actualiza</em> despues: al agregar un estado nuevo, la base sigue
 * aceptando solo los viejos y rechaza el valor nuevo en tiempo de ejecucion. Tampoco alcanza con
 * borrarla, porque Hibernate no la vuelve a crear sobre una columna que ya existe: quedaria la
 * tabla sin ninguna proteccion.</p>
 *
 * <p>Lo verifique contra Postgres real: sin esto, la aceptacion de un auditor falla al guardar
 * {@code AUDITOR_ASIGNADO} en cualquier base que ya existiera. Las pruebas no lo detectan porque
 * H2 corre con {@code create-drop} y recrea la restriccion completa en cada arranque, asi que el
 * CI queda verde con la aplicacion rota. Es el mismo patron que ya nos paso con
 * {@code catalogo_insignias_tipos_certificacion}.</p>
 *
 * <p>La lista de valores sale del enum y no de una constante escrita a mano, para que agregar un
 * estado en el futuro no requiera acordarse de tocar tambien este archivo.</p>
 *
 * <p>Es un parche puntual, no una solucion: lo que corresponde es adoptar Flyway y versionar el
 * esquema. Queda propuesto aparte porque afecta a todos los dominios, no solo a este.</p>
 */
@Component
public class SincronizadorRestriccionEstadoAuditoria {

    private static final Logger log =
            LoggerFactory.getLogger(SincronizadorRestriccionEstadoAuditoria.class);

    private static final String TABLA = "solicitudes_auditoria";
    private static final String RESTRICCION = "solicitudes_auditoria_estado_check";

    private final JdbcTemplate jdbcTemplate;

    public SincronizadorRestriccionEstadoAuditoria(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizar() {
        String valores = Arrays.stream(EstadoSolicitudAuditoria.values())
                .map(estado -> "'" + estado.name() + "'")
                .collect(Collectors.joining(", "));

        try {
            jdbcTemplate.execute(
                    "alter table %s drop constraint if exists %s".formatted(TABLA, RESTRICCION));
            jdbcTemplate.execute(
                    "alter table %s add constraint %s check (estado in (%s))"
                            .formatted(TABLA, RESTRICCION, valores));
            log.info("Restriccion {} realineada con los {} estados del enum",
                    RESTRICCION, EstadoSolicitudAuditoria.values().length);
        } catch (RuntimeException e) {
            // No corta el arranque: si la base no permite el DDL, es preferible una aplicacion
            // arriba con la restriccion desactualizada que una que no levanta.
            log.error("No se pudo realinear la restriccion {}. Los estados agregados despues de la "
                    + "creacion de la tabla van a ser rechazados al guardar.", RESTRICCION, e);
        }
    }
}
