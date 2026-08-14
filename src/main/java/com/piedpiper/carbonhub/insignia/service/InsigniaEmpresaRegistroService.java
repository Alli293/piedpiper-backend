package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code REQUIRES_NEW} porque el llamador ({@link InsigniaEmpresaEvaluacionService},
 * vía {@code EmisionCertificacionService.evaluarInsigniasTrasCommit}) se invoca
 * desde un callback {@code TransactionSynchronization.afterCommit()}: en ese punto
 * la transaccion original ya se cerro, asi que no hay ninguna que unir con
 * {@code REQUIRED} — intentarlo lanza {@code InvalidDataAccessApiUsageException:
 * no transaction is in progress}. Mismo patron que
 * {@link com.piedpiper.carbonhub.certificacion.service.CertificacionPersistenciaService#guardar}.
 */
@Service
public class InsigniaEmpresaRegistroService {

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;

    public InsigniaEmpresaRegistroService(InsigniaEmpresaRepository insigniaEmpresaRepository) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(InsigniaEmpresa insigniaEmpresa) {
        insigniaEmpresaRepository.saveAndFlush(insigniaEmpresa);
    }
}
