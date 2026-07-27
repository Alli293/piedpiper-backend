package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionAuditorLiberacionServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private EnvioCorreoAsignacionAuditorService envioCorreoAsignacionAuditorService;

    private AsignacionAuditorLiberacionService service;

    @BeforeEach
    void configurar() {
        service = new AsignacionAuditorLiberacionService(
                solicitudAuditoriaRepository, envioCorreoAsignacionAuditorService);
    }

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void liberarQuitaLaAsignacionSinModificarElEstadoDeLaSolicitud() {
        SolicitudAuditoria vencida = solicitudAsignada();
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(vencida));

        service.liberar(SOLICITUD_ID);

        assertThat(vencida.getAuditor()).isNull();
        assertThat(vencida.getOrigenAsignacion()).isNull();
        assertThat(vencida.getFechaAsignacion()).isNull();
        assertThat(vencida.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        verify(solicitudAuditoriaRepository).saveAndFlush(vencida);
    }

    @Test
    void laEmpresaSeNotificaSoloDespuesDelCommit() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitudAsignada()));
        TransactionSynchronizationManager.initSynchronization();

        service.liberar(SOLICITUD_ID);

        verifyNoInteractions(envioCorreoAsignacionAuditorService);

        List<TransactionSynchronization> sincronizaciones =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        assertThat(sincronizaciones).hasSize(1);
        sincronizaciones.forEach(TransactionSynchronization::afterCommit);

        verify(envioCorreoAsignacionAuditorService)
                .enviarExpiracion("contacto@empresa.cr", "Empresa Demo", "Ana Auditora");
    }

    @Test
    void solicitudSinAuditorAsignadoNoSeGuardaNiSeNotifica() {
        SolicitudAuditoria sinAuditor = solicitudAsignada();
        sinAuditor.setAuditor(null);
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(sinAuditor));

        service.liberar(SOLICITUD_ID);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any(SolicitudAuditoria.class));
        verifyNoInteractions(envioCorreoAsignacionAuditorService);
    }

    @Test
    void solicitudInexistenteNoProduceEfectos() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        service.liberar(SOLICITUD_ID);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any(SolicitudAuditoria.class));
        verifyNoInteractions(envioCorreoAsignacionAuditorService);
    }

    private static SolicitudAuditoria solicitudAsignada() {
        return SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .nombreEmpresa("Empresa Demo")
                        .correoCorporativo("contacto@empresa.cr")
                        .build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.now().minusMonths(6))
                .periodoFin(LocalDate.now().minusMonths(1))
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .fechaCreacion(Instant.now().minus(10, ChronoUnit.DAYS))
                .auditor(Usuario.builder()
                        .id(AUDITOR_ID)
                        .nombreVisible("Ana Auditora")
                        .email("auditora@carbonhub.cr")
                        .rol(Rol.AUDITOR_CERTIFICADO)
                        .estado(EstadoUsuario.ACTIVO)
                        .build())
                .origenAsignacion(OrigenAsignacion.MANUAL)
                .fechaAsignacion(Instant.now().minus(121, ChronoUnit.HOURS))
                .build();
    }
}
