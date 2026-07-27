package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persiste una solicitud con su documento contra una base real. Los tests de servicio mockean el
 * repositorio, así que no ejercitan el mapeo de la columna binaria: el contenido llegó a guardarse
 * como referencia a Large Object en vez de como bytes, y el insert fallaba solo en Postgres.
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
