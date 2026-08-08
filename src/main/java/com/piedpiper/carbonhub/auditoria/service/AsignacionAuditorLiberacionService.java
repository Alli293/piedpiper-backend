package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class AsignacionAuditorLiberacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionAuditorLiberacionService.class);

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final EnvioCorreoAsignacionAuditorService envioCorreoAsignacionAuditorService;
    private final TransicionEstadoAuditoriaService transicionEstadoAuditoriaService;

    public AsignacionAuditorLiberacionService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            EnvioCorreoAsignacionAuditorService envioCorreoAsignacionAuditorService,
            TransicionEstadoAuditoriaService transicionEstadoAuditoriaService) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.envioCorreoAsignacionAuditorService = envioCorreoAsignacionAuditorService;
        this.transicionEstadoAuditoriaService = transicionEstadoAuditoriaService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void liberar(UUID solicitudId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId).orElse(null);
        if (solicitud == null || solicitud.getAuditor() == null) {
            log.warn("No se encontro una asignacion vigente para liberar en la solicitud {}", solicitudId);
            return;
        }

        Usuario auditor = solicitud.getAuditor();
        Empresa empresa = solicitud.getEmpresa();
        String nombreAuditor = auditor.nombreCompleto();
        String nombreEmpresa = empresa.getNombreEmpresa();
        String correoEmpresa = empresa.getCorreoCorporativo();

        // Se registra antes de soltar la asignacion: despues, la solicitud ya no sabe quien era el
        // auditor y la entrada del historial quedaria sin decir a quien se le vencio el plazo.
        transicionEstadoAuditoriaService.aplicar(solicitud,
                EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA,
                ActorTransicionAuditoria.SISTEMA,
                null);

        solicitud.setAuditor(null);
        solicitud.setOrigenAsignacion(null);
        solicitud.setFechaAsignacion(null);
        solicitudAuditoriaRepository.saveAndFlush(solicitud);

        log.info("Se libero al auditor de la solicitud {} por falta de respuesta dentro del plazo", solicitudId);
        notificarTrasCommit(correoEmpresa, nombreEmpresa, nombreAuditor);
    }

    private void notificarTrasCommit(String correoEmpresa, String nombreEmpresa, String nombreAuditor) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envioCorreoAsignacionAuditorService.enviarExpiracion(
                            correoEmpresa, nombreEmpresa, nombreAuditor);
                }
            });
        } else {
            envioCorreoAsignacionAuditorService.enviarExpiracion(correoEmpresa, nombreEmpresa, nombreAuditor);
        }
    }
}
