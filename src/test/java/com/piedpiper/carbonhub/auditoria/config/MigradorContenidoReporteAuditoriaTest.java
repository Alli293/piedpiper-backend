package com.piedpiper.carbonhub.auditoria.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MigradorContenidoReporteAuditoriaTest {

    private JdbcTemplate jdbcTemplate;
    private MigradorContenidoReporteAuditoria migrador;

    @BeforeEach
    void configurarBase() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:mem:contenido-reporte-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");

        jdbcTemplate = new JdbcTemplate(dataSource);
        migrador = new MigradorContenidoReporteAuditoria(jdbcTemplate);
    }

    @AfterEach
    void cerrar() {
        jdbcTemplate.execute("drop all objects");
    }

    @Test
    void migraLosBytesLegacyYRetiraLaColumnaVieja() {
        crearTablasConColumnaLegacy();
        UUID reporteMigrado = UUID.randomUUID();
        UUID reporteExistente = UUID.randomUUID();
        byte[] contenidoMigrado = bytes("reporte legacy");
        byte[] contenidoExistente = bytes("reporte ya migrado");

        jdbcTemplate.update("insert into reportes_auditoria (id, contenido) values (?, ?)",
                reporteMigrado, contenidoMigrado);
        jdbcTemplate.update("insert into reportes_auditoria (id, contenido) values (?, ?)",
                reporteExistente, bytes("no debe sobrescribir"));
        jdbcTemplate.update("""
                insert into contenidos_reportes_auditoria (reporte_auditoria_id, contenido)
                values (?, ?)
                """, reporteExistente, contenidoExistente);

        migrador.migrar();

        assertThat(contenidoDe(reporteMigrado)).isEqualTo(contenidoMigrado);
        assertThat(contenidoDe(reporteExistente)).isEqualTo(contenidoExistente);
        assertThat(columnaLegacyExiste()).isFalse();
    }

    @Test
    void migrarDosVecesEsInocuo() {
        crearTablasConColumnaLegacy();
        UUID reporteId = UUID.randomUUID();
        jdbcTemplate.update("insert into reportes_auditoria (id, contenido) values (?, ?)",
                reporteId, bytes("reporte"));

        migrador.migrar();

        assertThatCode(() -> migrador.migrar()).doesNotThrowAnyException();
        assertThat(contenidoDe(reporteId)).isEqualTo(bytes("reporte"));
    }

    @Test
    void noHaceNadaCuandoLaBaseYaNoTieneLaColumnaLegacy() {
        jdbcTemplate.execute("create table reportes_auditoria (id uuid primary key)");
        jdbcTemplate.execute("""
                create table contenidos_reportes_auditoria (
                    reporte_auditoria_id uuid primary key,
                    contenido bytea not null
                )
                """);

        assertThatCode(() -> migrador.migrar()).doesNotThrowAnyException();
    }

    private void crearTablasConColumnaLegacy() {
        jdbcTemplate.execute("""
                create table reportes_auditoria (
                    id uuid primary key,
                    contenido bytea not null
                )
                """);
        jdbcTemplate.execute("""
                create table contenidos_reportes_auditoria (
                    reporte_auditoria_id uuid primary key,
                    contenido bytea not null
                )
                """);
    }

    private boolean columnaLegacyExiste() {
        Integer total = jdbcTemplate.queryForObject("""
                select count(*)
                  from information_schema.columns
                 where lower(table_name) = 'reportes_auditoria'
                   and lower(column_name) = 'contenido'
                """, Integer.class);
        return total != null && total > 0;
    }

    private byte[] contenidoDe(UUID reporteId) {
        return jdbcTemplate.queryForObject("""
                select contenido
                  from contenidos_reportes_auditoria
                 where reporte_auditoria_id = ?
                """, byte[].class, reporteId);
    }

    private static byte[] bytes(String valor) {
        return valor.getBytes(StandardCharsets.UTF_8);
    }
}
