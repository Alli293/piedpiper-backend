package com.piedpiper.carbonhub.auditoria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migra el contenido binario de reportes de auditoria a su tabla separada.
 *
 * <p>Existe por el mismo motivo que los sincronizadores de restricciones: el proyecto usa
 * {@code ddl-auto=update}, asi que Hibernate crea la tabla nueva pero no elimina la columna vieja
 * {@code reportes_auditoria.contenido}. Si esa columna queda con {@code NOT NULL}, cualquier carga
 * nueva falla porque la entidad de metadatos ya no la inserta. Antes de retirarla se copian los
 * bytes existentes para no perder reportes ya cargados.</p>
 */
@Component
public class MigradorContenidoReporteAuditoria {

    private static final Logger log = LoggerFactory.getLogger(MigradorContenidoReporteAuditoria.class);

    static final String TABLA_REPORTES = "reportes_auditoria";
    static final String TABLA_CONTENIDOS = "contenidos_reportes_auditoria";
    static final String COLUMNA_LEGADO = "contenido";

    private static final String CONSULTA_COLUMNA = """
            select count(*)
              from information_schema.columns
             where lower(table_name) = ?
               and lower(column_name) = ?
            """;

    private static final String COPIAR_CONTENIDO = """
            insert into contenidos_reportes_auditoria (reporte_auditoria_id, contenido)
            select r.id, r.contenido
              from reportes_auditoria r
             where r.contenido is not null
               and not exists (
                   select 1
                     from contenidos_reportes_auditoria c
                    where c.reporte_auditoria_id = r.id
               )
            """;

    private final JdbcTemplate jdbcTemplate;

    public MigradorContenidoReporteAuditoria(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrar() {
        try {
            if (!columnaLegadoExiste()) {
                return;
            }

            int migrados = jdbcTemplate.update(COPIAR_CONTENIDO);
            jdbcTemplate.execute("alter table %s drop column %s".formatted(TABLA_REPORTES, COLUMNA_LEGADO));
            log.info("Contenido legacy de reportes de auditoria migrado: {} filas", migrados);
        } catch (RuntimeException e) {
            // No corta el arranque, pero si esto falla una base vieja puede conservar el NOT NULL
            // legacy y rechazar cargas nuevas hasta aplicar la migracion manualmente.
            log.error("No se pudo migrar la columna legacy {}.{} a {}",
                    TABLA_REPORTES, COLUMNA_LEGADO, TABLA_CONTENIDOS, e);
        }
    }

    private boolean columnaLegadoExiste() {
        Integer total = jdbcTemplate.queryForObject(
                CONSULTA_COLUMNA, Integer.class, TABLA_REPORTES, COLUMNA_LEGADO);
        return total != null && total > 0;
    }
}
