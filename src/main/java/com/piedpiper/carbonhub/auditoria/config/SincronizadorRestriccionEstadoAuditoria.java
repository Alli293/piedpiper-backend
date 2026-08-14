package com.piedpiper.carbonhub.auditoria.config;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
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
 * <p><strong>De donde sale el nombre a borrar.</strong> No se asume la convencion de Hibernate
 * ({@code <tabla>_<columna>_check}): esa convencion puede cambiar entre versiones o dialectos, y un
 * {@code drop constraint if exists} con el nombre equivocado no falla, simplemente no borra nada y
 * deja en pie la restriccion vieja, que es justo el bug que esta clase evita. Se consultan por
 * catalogo las restricciones CHECK reales de la columna y se borran todas. La que se crea despues
 * lleva un nombre propio y estable, que ya no depende de Hibernate.</p>
 *
 * <p><strong>Dos instancias a la vez.</strong> El DDL toma un lock exclusivo de la tabla, asi que
 * la segunda instancia espera y despues repite el mismo trabajo con el mismo resultado. Si aun asi
 * la creacion choca porque la otra la creo entremedio, se captura abajo: la restriccion ya quedo
 * con los valores correctos, que es lo que importa.</p>
 *
 * <p>Es un parche puntual, no una solucion: lo que corresponde es adoptar Flyway y versionar el
 * esquema. Queda propuesto aparte porque afecta a todos los dominios, no solo a este.</p>
 */
@Component
public class SincronizadorRestriccionEstadoAuditoria {

    private static final Logger log =
            LoggerFactory.getLogger(SincronizadorRestriccionEstadoAuditoria.class);

    static final String TABLA = "solicitudes_auditoria";
    static final String COLUMNA = "estado";

    /** Nombre propio: no depende de como bautice Hibernate sus restricciones. */
    static final String RESTRICCION = "chk_solicitudes_auditoria_estado";

    /**
     * Las restricciones CHECK de una columna, por catalogo. {@code constraint_column_usage} es
     * parte del estandar SQL y lo implementan tanto Postgres como H2, asi que la misma consulta
     * sirve en la base real y en las pruebas.
     */
    private static final String CONSULTA_RESTRICCIONES = """
            select tc.constraint_name
              from information_schema.table_constraints tc
              join information_schema.constraint_column_usage ccu
                on ccu.constraint_name = tc.constraint_name
               and ccu.constraint_schema = tc.constraint_schema
             where tc.constraint_type = 'CHECK'
               and lower(ccu.table_name) = ?
               and lower(ccu.column_name) = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public SincronizadorRestriccionEstadoAuditoria(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizar() {
        try {
            for (String nombre : restriccionesDeLaColumna()) {
                jdbcTemplate.execute(
                        "alter table %s drop constraint if exists %s".formatted(TABLA, nombre));
            }
            jdbcTemplate.execute("alter table %s add constraint %s check (%s in (%s))"
                    .formatted(TABLA, RESTRICCION, COLUMNA, valoresDelEnum()));
            log.info("Restriccion {} realineada con los {} estados del enum",
                    RESTRICCION, EstadoSolicitudAuditoria.values().length);
        } catch (RuntimeException e) {
            // No corta el arranque: si la base no permite el DDL, es preferible una aplicacion
            // arriba con la restriccion desactualizada que una que no levanta.
            log.error("No se pudo realinear la restriccion {}. Los estados agregados despues de la "
                    + "creacion de la tabla van a ser rechazados al guardar.", RESTRICCION, e);
        }
    }

    private List<String> restriccionesDeLaColumna() {
        return jdbcTemplate.queryForList(CONSULTA_RESTRICCIONES, String.class, TABLA, COLUMNA);
    }

    private static String valoresDelEnum() {
        return Arrays.stream(EstadoSolicitudAuditoria.values())
                .map(estado -> "'" + estado.name() + "'")
                .collect(Collectors.joining(", "));
    }
}
