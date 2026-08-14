package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.IndiceEstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.IndiceEstadoCertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.NotificacionPanelRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaEvaluacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    private static final UUID ID_AUDITORIA = UUID.randomUUID();
    private static final UUID ID_EMPRESA = UUID.randomUUID();
    private static final UUID ID_AUDITOR = UUID.randomUUID();
    private static final LocalDate FECHA_AUDITORIA = LocalDate.of(2026, 3, 10);

    @Autowired
    private ReservaIndiceDePrueba reserva;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EmisionCertificacionService emisionCertificacionService;

    @MockitoBean
    private CertificacionRepository certificacionRepository;
    @MockitoBean
    private NotificacionPanelRepository notificacionPanelRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private CatalogoTiposCertificacion catalogoTiposCertificacion;
    @MockitoBean
    private GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    @MockitoBean
    private CertificacionMapper certificacionMapper;
    @MockitoBean
    private CertificacionPersistenciaService certificacionPersistenciaService;
    @MockitoBean
    private InsigniaEmpresaEvaluacionService insigniaEmpresaEvaluacionService;
    @MockitoBean
    private GeneradorCodigoVerificacionService generadorCodigoVerificacionService;

    @TestConfiguration
    static class PropagacionTestConfig {
        @Bean
        ReservaIndiceDePrueba reservaIndiceDePrueba(IndiceEstadoCertificacionRepository repo) {
            return new ReservaIndiceDePrueba(repo);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }

        /**
         * Se declara como bean para que Spring lo envuelva en su proxy transaccional: sin proxy la
         * anotación del método no se aplicaría y el test no probaría nada.
         */
        @Bean
        EmisionCertificacionService emisionCertificacionService(
                CertificacionRepository certificacionRepository,
                NotificacionPanelRepository notificacionPanelRepository,
                IndiceEstadoCertificacionRepository indiceEstadoCertificacionRepository,
                EmpresaRepository empresaRepository,
                UsuarioRepository usuarioRepository,
                CatalogoTiposCertificacion catalogoTiposCertificacion,
                GeneradorCredencialOpenBadges generadorCredencialOpenBadges,
                CertificacionMapper certificacionMapper,
                CertificacionPersistenciaService certificacionPersistenciaService,
                InsigniaEmpresaEvaluacionService insigniaEmpresaEvaluacionService,
                GeneradorCodigoVerificacionService generadorCodigoVerificacionService) {
            return new EmisionCertificacionService(certificacionRepository,
                    notificacionPanelRepository, indiceEstadoCertificacionRepository,
                    empresaRepository, usuarioRepository, catalogoTiposCertificacion,
                    generadorCredencialOpenBadges, certificacionMapper,
                    certificacionPersistenciaService, insigniaEmpresaEvaluacionService,
                    generadorCodigoVerificacionService);
        }
    }

    /** Reserva el índice con las dos propagaciones, para poder comparar el comportamiento. */
    @org.springframework.stereotype.Service
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

    @BeforeEach
    void configurarColaboradores() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR)).thenReturn(Optional.of(Usuario.builder()
                .id(ID_AUDITOR)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .build()));
        when(catalogoTiposCertificacion.buscar(TipoCertificacion.CARBONO_NEUTRAL))
                .thenReturn(Optional.of(new DefinicionCertificacion(
                        TipoCertificacion.CARBONO_NEUTRAL, "Carbono Neutral", "descripcion", 12,
                        TipoLogroOpenBadges.CERTIFICATION, "criterio")));
        when(generadorCodigoVerificacionService.generar()).thenReturn("CH-2026-TESTCODE1");
        when(generadorCredencialOpenBadges.generar(any(), any())).thenReturn("jwt.firmado.aqui");
        when(certificacionMapper.toDto(any(Certificacion.class)))
                .thenAnswer(invocacion -> new CertificacionResponseDTO());
    }

    /**
     * El test que protege directamente el arreglo: invoca el método productivo, no un espejo.
     *
     * <p>Se comprueba sobre la certificación que llega a persistirse, porque el índice nulo es el
     * síntoma exacto del fallo: con propagación {@code REQUIRED} el {@code INSERT} del índice no
     * corría, {@code getIndice()} volvía nulo y el guardado moría con
     * {@code null value in column "indice_estado"}.</p>
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void laEmisionRealDesdeAfterCommitPersisteLaCertificacionConSuIndice() {
        AtomicReference<Certificacion> guardada = new AtomicReference<>();
        when(certificacionPersistenciaService.guardar(any(Certificacion.class)))
                .thenAnswer(invocacion -> {
                    Certificacion certificacion = invocacion.getArgument(0);
                    guardada.set(certificacion);
                    return certificacion;
                });

        transactionTemplate.executeWithoutResult(estado ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emisionCertificacionService.emitirPorAuditoriaAprobada(
                                new EmitirCertificacionRequestDTO(ID_AUDITORIA, ID_EMPRESA,
                                        ID_AUDITOR, "aprobada", FECHA_AUDITORIA,
                                        TipoCertificacion.CARBONO_NEUTRAL, null));
                    }
                }));

        assertThat(guardada.get())
                .as("la emision llego a persistir la certificacion sin propagar un error")
                .isNotNull();
        assertThat(guardada.get().getIndiceEstado())
                .as("el indice de estado se reservo de verdad; en nulo es el fallo que devolvia 500")
                .isNotNull();
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
