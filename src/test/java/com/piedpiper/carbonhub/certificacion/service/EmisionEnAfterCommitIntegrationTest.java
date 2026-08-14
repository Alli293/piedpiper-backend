package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.IndiceEstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.IndiceEstadoCertificacionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce, con transacciones reales de Spring y no con mocks, el motivo por el que aprobar una
 * auditoría devolvía 500 y dejaba la solicitud en {@code CERTIFICACION_EMITIDA} sin certificación.
 *
 * <p>La emisión se dispara desde el {@code afterCommit} de la transacción que aprueba. Ahí dentro
 * la transacción ya se confirmó, pero la sincronización sigue activa y sus recursos siguen ligados
 * al hilo, así que un {@code @Transactional} con propagación {@code REQUIRED} se une a esa
 * transacción terminada en vez de abrir una nueva. Un {@code save} sobre una entidad con identidad
 * autoincremental no llega a ejecutar su {@code INSERT} y devuelve el id en nulo, que es de donde
 * salía el {@code null value in column "indice_estado"}.</p>
 *
 * <p>Ningún test de unidad con mocks podía verlo: el fallo no está en la lógica sino en la
 * propagación, y con un repositorio mockeado el id siempre vuelve.</p>
 *
 * <p>Los métodos van con {@code NOT_SUPPORTED} porque {@code @DataJpaTest} envuelve cada prueba en
 * una transacción que revierte al final: dentro de ella el {@code TransactionTemplate} se uniría a
 * la del test y el {@code afterCommit} no llegaría a dispararse nunca.</p>
 */
@DataJpaTest
@Import(EmisionEnAfterCommitIntegrationTest.PropagacionTestConfig.class)
class EmisionEnAfterCommitIntegrationTest {

    @Autowired
    private ReservaIndiceDePrueba reserva;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @TestConfiguration
    static class PropagacionTestConfig {
        @org.springframework.context.annotation.Bean
        ReservaIndiceDePrueba reservaIndiceDePrueba(IndiceEstadoCertificacionRepository repo) {
            return new ReservaIndiceDePrueba(repo);
        }

        @org.springframework.context.annotation.Bean
        TransactionTemplate transactionTemplate(
                org.springframework.transaction.PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }
    }

    /** Reserva el índice con las dos propagaciones, para poder comparar el comportamiento. */
    @Service
    static class ReservaIndiceDePrueba {

        private final IndiceEstadoCertificacionRepository repo;

        ReservaIndiceDePrueba(IndiceEstadoCertificacionRepository repo) {
            this.repo = repo;
        }

        @Transactional
        Long conPropagacionHeredada() {
            return repo.save(new IndiceEstadoCertificacion()).getIndice();
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        Long conTransaccionPropia() {
            return repo.save(new IndiceEstadoCertificacion()).getIndice();
        }
    }

    /**
     * El arreglo: llamada desde {@code afterCommit}, la propagación propia sí reserva un índice.
     * Sin ella el valor volvía nulo y la certificación no se podía insertar.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void desdeAfterCommitLaTransaccionPropiaSiReservaElIndice() {
        AtomicReference<Long> indice = new AtomicReference<>();

        transactionTemplate.executeWithoutResult(estado ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        indice.set(reserva.conTransaccionPropia());
                    }
                }));

        assertThat(indice.get())
                .as("dentro de afterCommit, REQUIRES_NEW abre una transacción nueva y el INSERT sí corre")
                .isNotNull();
    }

    /** Fuera de un afterCommit no hay nada raro: la propagación heredada funciona igual de bien. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void fueraDeAfterCommitLaPropagacionHeredadaFuncionaIgual() {
        assertThat(reserva.conPropagacionHeredada()).isNotNull();
    }
}
