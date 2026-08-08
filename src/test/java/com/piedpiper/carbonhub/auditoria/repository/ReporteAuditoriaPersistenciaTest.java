package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.ContenidoReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.ReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;

import jakarta.persistence.Lob;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.http.MediaType;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReporteAuditoriaPersistenciaTest {

    private static final byte[] CONTENIDO_PDF = "%PDF-1.7 reporte de auditoria"
            .getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Autowired
    private ContenidoReporteAuditoriaRepository contenidoReporteAuditoriaRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void elContenidoBinarioViveFueraDeLaEntidadDeMetadatos() throws NoSuchFieldException {
        assertThat(Arrays.stream(ReporteAuditoria.class.getDeclaredFields())
                .filter(campo -> campo.getType().equals(byte[].class)))
                .as("el detalle de auditoria no debe cargar el PDF completo al mapear metadatos")
                .isEmpty();

        Field contenido = ContenidoReporteAuditoria.class.getDeclaredField("contenido");
        assertThat(contenido.getAnnotation(Lob.class))
                .as("@Lob sobre byte[] guarda OID en Postgres en vez de bytea")
                .isNull();
        assertThat(contenido.getAnnotation(JdbcTypeCode.class))
                .isNotNull()
                .extracting(JdbcTypeCode::value)
                .isEqualTo(SqlTypes.VARBINARY);
    }

    @Test
    void elReporteGuardaYRecuperaElContenidoEnSuEntidadSeparada() {
        SolicitudAuditoria solicitud = solicitudDe(empresaGuardada());
        solicitud.reemplazarReporteAuditoria(reporte());

        SolicitudAuditoria guardada = solicitudAuditoriaRepository.saveAndFlush(solicitud);
        contenidoReporteAuditoriaRepository.saveAndFlush(ContenidoReporteAuditoria.builder()
                .reporteAuditoria(guardada.getReporteAuditoria())
                .contenido(CONTENIDO_PDF)
                .build());
        entityManager.clear();

        SolicitudAuditoria recuperada = solicitudAuditoriaRepository.findById(guardada.getId()).orElseThrow();
        ReporteAuditoria reporte = recuperada.getReporteAuditoria();

        assertThat(reporte.getNombreArchivo()).isEqualTo("reporte-auditoria.pdf");
        assertThat(contenidoReporteAuditoriaRepository.findById(reporte.getId()).orElseThrow().getContenido())
                .isEqualTo(CONTENIDO_PDF);
    }

    private Empresa empresaGuardada() {
        return empresaRepository.saveAndFlush(Empresa.builder()
                .nombreEmpresa("Finca Tres Rios")
                .cedulaJuridica("3-101-" + System.nanoTime())
                .correoCorporativo("contacto" + System.nanoTime() + "@tresrios.cr")
                .pais("Costa Rica")
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .slug("finca-tres-rios-" + System.nanoTime())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build());
    }

    private SolicitudAuditoria solicitudDe(Empresa empresa) {
        return SolicitudAuditoria.builder()
                .empresa(empresa)
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2026, 1, 1))
                .periodoFin(LocalDate.of(2026, 12, 31))
                .estado(EstadoSolicitudAuditoria.REPORTE_CARGADO)
                .fechaCreacion(Instant.now())
                .documentos(new ArrayList<>())
                .build();
    }

    private ReporteAuditoria reporte() {
        return ReporteAuditoria.builder()
                .nombreArchivo("reporte-auditoria.pdf")
                .tipoContenido(MediaType.APPLICATION_PDF_VALUE)
                .tamanioBytes(CONTENIDO_PDF.length)
                .fechaCarga(Instant.now())
                .build();
    }
}
