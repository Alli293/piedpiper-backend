package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
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

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ejercita el guardado y la recuperación del documento, que los tests de servicio no cubren porque
 * mockean el repositorio.
 *
 * <p>Ojo con el alcance: esto corre sobre H2, no sobre Postgres, y el bug original (un {@code @Lob}
 * sobre {@code byte[]} que hacía guardar un OID en vez de los bytes) solo se manifestaba en
 * Postgres. O sea que los dos tests de ida y vuelta pasarían igual con el mapeo roto. Lo que sí
 * protege contra esa regresión, sin depender del motor, es
 * {@link #elContenidoSeMapeaComoBinarioYNoComoLargeObject()}, que verifica las anotaciones de la
 * entidad. Reproducirlo de verdad exigiría Testcontainers, que hoy el proyecto no usa.</p>
 */
@DataJpaTest
class DocumentoRespaldoPersistenciaTest {

    private static final byte[] CONTENIDO_PDF = "%PDF-1.7 contenido de respaldo"
            .getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void elContenidoSeMapeaComoBinarioYNoComoLargeObject() throws NoSuchFieldException {
        Field contenido = DocumentoRespaldo.class.getDeclaredField("contenido");

        assertThat(contenido.getAnnotation(Lob.class))
                .as("@Lob sobre un byte[] hace que Postgres guarde un OID en vez de los bytes")
                .isNull();
        assertThat(contenido.getAnnotation(JdbcTypeCode.class))
                .as("sin @JdbcTypeCode(VARBINARY) Hibernate vuelve a elegir el tipo por su cuenta")
                .isNotNull()
                .extracting(JdbcTypeCode::value)
                .isEqualTo(SqlTypes.VARBINARY);
    }

    @Test
    void elContenidoDelDocumentoSeGuardaYSeRecuperaByteAByte() {
        SolicitudAuditoria solicitud = solicitudDe(empresaGuardada());
        solicitud.agregarDocumento(DocumentoRespaldo.builder()
                .nombreArchivo("balance.pdf")
                .tipoContenido("application/pdf")
                .tamanioBytes(CONTENIDO_PDF.length)
                .contenido(CONTENIDO_PDF)
                .fechaCarga(Instant.now())
                .build());

        SolicitudAuditoria guardada = solicitudAuditoriaRepository.saveAndFlush(solicitud);
        entityManager.clear();

        SolicitudAuditoria recuperada = solicitudAuditoriaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(recuperada.getDocumentos()).hasSize(1);
        DocumentoRespaldo documento = recuperada.getDocumentos().get(0);
        assertThat(documento.getContenido()).isEqualTo(CONTENIDO_PDF);
        assertThat(new String(documento.getContenido(), StandardCharsets.US_ASCII)).startsWith("%PDF-");
    }

    @Test
    void alBorrarLaSolicitudSeVanSusDocumentos() {
        SolicitudAuditoria solicitud = solicitudDe(empresaGuardada());
        solicitud.agregarDocumento(DocumentoRespaldo.builder()
                .nombreArchivo("balance.pdf")
                .tipoContenido("application/pdf")
                .tamanioBytes(CONTENIDO_PDF.length)
                .contenido(CONTENIDO_PDF)
                .fechaCarga(Instant.now())
                .build());
        SolicitudAuditoria guardada = solicitudAuditoriaRepository.saveAndFlush(solicitud);

        solicitudAuditoriaRepository.delete(guardada);
        solicitudAuditoriaRepository.flush();
        entityManager.clear();

        assertThat(solicitudAuditoriaRepository.findById(guardada.getId())).isEmpty();
    }

    private Empresa empresaGuardada() {
        return empresaRepository.saveAndFlush(Empresa.builder()
                .nombreEmpresa("Cafetalera Los Santos")
                .cedulaJuridica("3-101-" + System.nanoTime())
                .correoCorporativo("contacto" + System.nanoTime() + "@lossantos.cr")
                .pais("Costa Rica")
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .slug("cafetalera-" + System.nanoTime())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build());
    }

    private SolicitudAuditoria solicitudDe(Empresa empresa) {
        return SolicitudAuditoria.builder()
                .empresa(empresa)
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2025, 1, 1))
                .periodoFin(LocalDate.of(2025, 12, 31))
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .fechaCreacion(Instant.now())
                .documentos(new ArrayList<>())
                .build();
    }
}
