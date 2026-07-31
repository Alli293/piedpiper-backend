package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste la certificacion en su propia transaccion.
 *
 * <p>{@code REQUIRES_NEW} es lo que permite que, si el flush falla por una
 * emision concurrente de la misma auditoria, solo esta transaccion quede
 * marcada rollback-only. La transaccion del llamante ({@link
 * EmisionCertificacionService}) sigue intacta y puede usar su EntityManager
 * con normalidad para recuperar la certificacion ganadora.
 */
@Service
public class CertificacionPersistenciaService {

    private final CertificacionRepository certificacionRepository;

    public CertificacionPersistenciaService(CertificacionRepository certificacionRepository) {
        this.certificacionRepository = certificacionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Certificacion guardar(Certificacion certificacion) {
        return certificacionRepository.saveAndFlush(certificacion);
    }
}
