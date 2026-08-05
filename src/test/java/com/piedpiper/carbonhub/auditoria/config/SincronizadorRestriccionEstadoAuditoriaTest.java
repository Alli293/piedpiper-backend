package com.piedpiper.carbonhub.auditoria.config;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * La base de estos tests <strong>no</strong> usa {@code create-drop}: se crea a mano la tabla con
 * la restriccion vieja de tres estados, que es exactamente la situacion que el componente existe
 * para arreglar y que el resto de la suite no puede reproducir, porque ahi H2 rehace el esquema
 * completo en cada arranque y la restriccion nace ya con los seis valores.
 */
class SincronizadorRestriccionEstadoAuditoriaTest {

    /** Los tres estados que tenia el enum antes de PP-47. */
    private static final String CHECK_VIEJO =
            "check (estado in ('SOLICITUD_ENVIADA', 'EN_REVISION', 'CERTIFICACION_EMITIDA'))";

    private JdbcTemplate jdbcTemplate;
    private SincronizadorRestriccionEstadoAuditoria sincronizador;

    @BeforeEach
    void crearTablaConLaRestriccionVieja() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:mem:restriccion-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");

        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                create table solicitudes_auditoria (
                    id uuid primary key,
                    estado varchar(40) not null constraint solicitudes_auditoria_estado_check %s
                )
                """.formatted(CHECK_VIEJO));

        sincronizador = new SincronizadorRestriccionEstadoAuditoria(jdbcTemplate);
    }

    @AfterEach
    void cerrar() {
        jdbcTemplate.execute("drop all objects");
    }

    @Test
    void antesDeSincronizarLaBaseRechazaLosEstadosNuevos() {
        assertThatThrownBy(() -> insertar(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void despuesDeSincronizarLaBaseAceptaTodosLosEstadosDelEnum() {
        sincronizador.sincronizar();

        for (EstadoSolicitudAuditoria estado : EstadoSolicitudAuditoria.values()) {
            assertThatCode(() -> insertar(estado))
                    .as("el estado %s deberia poder guardarse", estado)
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Borrar la restriccion vieja sin poner otra dejaria la columna sin ninguna proteccion, que es
     * peor que el bug original: cualquier cadena entraria a la tabla.
     */
    @Test
    void laColumnaSigueProtegidaContraValoresQueNoSonDelEnum() {
        sincronizador.sincronizar();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into solicitudes_auditoria (id, estado) values (?, ?)",
                UUID.randomUUID(), "ESTADO_INVENTADO"))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * El nombre viejo lo eligio Hibernate; el nuevo lo elegimos nosotros. Si el componente
     * dependiera del nombre asumido, este test fallaria al cambiar la convencion de Hibernate.
     */
    @Test
    void reemplazaLaRestriccionDeHibernatePorUnaConNombrePropio() {
        sincronizador.sincronizar();

        assertThat(nombresDeRestriccionesCheck())
                .containsExactly(SincronizadorRestriccionEstadoAuditoria.RESTRICCION.toUpperCase());
    }

    /**
     * Si la convencion de nombres de Hibernate cambia, el componente no tiene forma de adivinar el
     * nombre: por eso lo consulta. Este caso cubre una restriccion que no se llama como Hibernate
     * la habria llamado.
     */
    @Test
    void tambienReemplazaUnaRestriccionConUnNombreQueNoSigueLaConvencionDeHibernate() {
        jdbcTemplate.execute("drop table solicitudes_auditoria");
        jdbcTemplate.execute("""
                create table solicitudes_auditoria (
                    id uuid primary key,
                    estado varchar(40) not null constraint un_nombre_cualquiera %s
                )
                """.formatted(CHECK_VIEJO));

        sincronizador.sincronizar();

        assertThatCode(() -> insertar(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO))
                .doesNotThrowAnyException();
    }

    /** Arrancar dos veces seguidas es lo que pasa en cada redeploy, y no debe fallar. */
    @Test
    void sincronizarDosVecesSeguidasEsInocuo() {
        sincronizador.sincronizar();
        sincronizador.sincronizar();

        assertThat(nombresDeRestriccionesCheck())
                .containsExactly(SincronizadorRestriccionEstadoAuditoria.RESTRICCION.toUpperCase());
        assertThatCode(() -> insertar(EstadoSolicitudAuditoria.REPORTE_CARGADO))
                .doesNotThrowAnyException();
    }

    private void insertar(EstadoSolicitudAuditoria estado) {
        jdbcTemplate.update("insert into solicitudes_auditoria (id, estado) values (?, ?)",
                UUID.randomUUID(), estado.name());
    }

    private List<String> nombresDeRestriccionesCheck() {
        return jdbcTemplate.queryForList("""
                select tc.constraint_name
                  from information_schema.table_constraints tc
                  join information_schema.constraint_column_usage ccu
                    on ccu.constraint_name = tc.constraint_name
                   and ccu.constraint_schema = tc.constraint_schema
                 where tc.constraint_type = 'CHECK'
                   and lower(ccu.table_name) = 'solicitudes_auditoria'
                   and lower(ccu.column_name) = 'estado'
                """, String.class);
    }
}
