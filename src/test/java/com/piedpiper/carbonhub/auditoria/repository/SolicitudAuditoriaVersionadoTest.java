package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que el bloqueo optimista de {@code SolicitudAuditoria} exista de verdad en el mapeo. El
 * test de servicio que cubre el 409 por concurrencia lanza la excepción desde un mock, así que
 * pasaría igual si a la entidad le faltara el {@code @Version}: lo único que prueba es la
 * traducción a {@code ApiException}, no que la base rechace la escritura perdedora.
 */
@DataJpaTest
class SolicitudAuditoriaVersionadoTest {

    @Autowired
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void laSegundaEscrituraSobreUnaVersionVencidaFalla() {
        UUID solicitudId = solicitudAuditoriaRepository.saveAndFlush(solicitudDe(empresaGuardada())).getId();
        entityManager.clear();

        SolicitudAuditoria copiaVieja = solicitudAuditoriaRepository.findById(solicitudId).orElseThrow();
        entityManager.detach(copiaVieja);

        SolicitudAuditoria ganadora = solicitudAuditoriaRepository.findById(solicitudId).orElseThrow();
        ganadora.setOrigenAsignacion(OrigenAsignacion.MANUAL);
        ganadora.setFechaAsignacion(Instant.now());
        solicitudAuditoriaRepository.saveAndFlush(ganadora);
        entityManager.clear();

        copiaVieja.setOrigenAsignacion(OrigenAsignacion.RECOMENDACION_IA);
        copiaVieja.setFechaAsignacion(Instant.now());

        assertThatThrownBy(() -> solicitudAuditoriaRepository.saveAndFlush(copiaVieja))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void laVersionAvanzaEnCadaEscritura() {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.saveAndFlush(solicitudDe(empresaGuardada()));
        long versionInicial = solicitud.getVersion();

        solicitud.setOrigenAsignacion(OrigenAsignacion.MANUAL);
        SolicitudAuditoria actualizada = solicitudAuditoriaRepository.saveAndFlush(solicitud);

        assertThat(actualizada.getVersion()).isGreaterThan(versionInicial);
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
