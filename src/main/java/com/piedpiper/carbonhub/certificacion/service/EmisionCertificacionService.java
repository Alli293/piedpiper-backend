package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.IndiceEstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.NotificacionPanel;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.IndiceEstadoCertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.NotificacionPanelRepository;
import com.piedpiper.carbonhub.common.ValidacionesAuditor;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaEvaluacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Emision automatica de certificaciones digitales al aprobarse una auditoria
 * (PP-58).
 *
 * <p>La credencial firmada se guarda en la propia fila, de modo que no hay
 * archivo que limpiar si algo falla y el rollback de la base de datos es el
 * unico mecanismo de compensacion necesario. El guardado de la certificacion
 * ocurre en la transaccion aparte de {@link CertificacionPersistenciaService}
 * -- no en esta -- para que una emision concurrente perdida no deje esta
 * transaccion marcada rollback-only y pueda seguir usandose para recuperar la
 * certificacion ganadora.
 */
@Service
public class EmisionCertificacionService implements EmisionCertificacionPort {

    private static final Logger log = LoggerFactory.getLogger(EmisionCertificacionService.class);
    private static final String RESULTADO_APROBADA = "aprobada";

    /**
     * Intentos ante una colision de {@code codigoVerificacion} al guardar (dos
     * emisiones concurrentes pueden generar el mismo codigo porque {@code
     * existsByCodigoVerificacion} se chequea antes de persistir, no de forma
     * atomica). No cubre la colision de {@code idAuditoria}, que ya se maneja
     * aparte porque esa si es esperable (reintento de webhook) y no debe
     * generar un codigo nuevo.
     */
    private static final int REINTENTOS_CODIGO_VERIFICACION = 3;

    private final CertificacionRepository certificacionRepository;
    private final NotificacionPanelRepository notificacionPanelRepository;
    private final IndiceEstadoCertificacionRepository indiceEstadoCertificacionRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    private final CertificacionMapper certificacionMapper;
    private final CertificacionPersistenciaService certificacionPersistenciaService;
    private final InsigniaEmpresaEvaluacionService insigniaEmpresaEvaluacionService;
    private final GeneradorCodigoVerificacionService generadorCodigoVerificacionService;

    public EmisionCertificacionService(CertificacionRepository certificacionRepository,
                                       NotificacionPanelRepository notificacionPanelRepository,
                                       IndiceEstadoCertificacionRepository
                                               indiceEstadoCertificacionRepository,
                                       EmpresaRepository empresaRepository,
                                       UsuarioRepository usuarioRepository,
                                       CatalogoTiposCertificacion catalogoTiposCertificacion,
                                       GeneradorCredencialOpenBadges generadorCredencialOpenBadges,
                                       CertificacionMapper certificacionMapper,
                                       CertificacionPersistenciaService certificacionPersistenciaService,
                                       InsigniaEmpresaEvaluacionService
                                               insigniaEmpresaEvaluacionService,
                                       GeneradorCodigoVerificacionService
                                               generadorCodigoVerificacionService) {
        this.certificacionRepository = certificacionRepository;
        this.notificacionPanelRepository = notificacionPanelRepository;
        this.indiceEstadoCertificacionRepository = indiceEstadoCertificacionRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.generadorCredencialOpenBadges = generadorCredencialOpenBadges;
        this.certificacionMapper = certificacionMapper;
        this.certificacionPersistenciaService = certificacionPersistenciaService;
        this.insigniaEmpresaEvaluacionService = insigniaEmpresaEvaluacionService;
        this.generadorCodigoVerificacionService = generadorCodigoVerificacionService;
    }

    /**
     * <b>{@code REQUIRES_NEW} y no {@code @Transactional} a secas.</b> El único llamador real
     * invoca este método desde el {@code afterCommit} de la transacción que aprueba la auditoría
     * ({@code ResultadoAuditoriaService.publicarAprobacionTrasCommit}). Dentro de ese enganche la
     * transacción original ya se confirmó, pero la sincronización sigue activa y sus recursos
     * siguen ligados al hilo, así que una propagación {@code REQUIRED} <b>se une a esa transacción
     * ya terminada</b> en vez de abrir una nueva.
     *
     * <p>El síntoma no era un error claro sino uno a dos pasos de distancia: la reserva del índice
     * de estado ({@code IndiceEstadoCertificacion}, con identidad autoincremental) no llegaba a
     * ejecutar su {@code INSERT}, {@code getIndice()} devolvía nulo, y la certificación fallaba al
     * insertarse con {@code null value in column "indice_estado"}. Al reintentar, la entidad ya
     * tenía id asignado, así que el {@code save} pasaba a ser un {@code merge} y terminaba en
     * {@code StaleObjectStateException} y un 500. La auditoría quedaba en
     * {@code CERTIFICACION_EMITIDA} sin ninguna certificación detrás.
     *
     * <p>Con {@code REQUIRES_NEW} la emisión corre en su propia transacción, que es lo que
     * corresponde: es una unidad de trabajo independiente de la aprobación que ya se confirmó, y si
     * falla no debe arrastrar nada de aquello. Es el mismo motivo por el que
     * {@link CertificacionPersistenciaService#guardar} ya usaba esa propagación.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CertificacionResponseDTO emitirPorAuditoriaAprobada(
            EmitirCertificacionRequestDTO comando) {

        if (!RESULTADO_APROBADA.equalsIgnoreCase(comando.getResultadoAuditoria())) {
            log.info("Auditoria {} con resultado '{}': no corresponde emitir certificacion.",
                    comando.getIdAuditoria(), comando.getResultadoAuditoria());
            throw ApiException.resultadoAuditoriaNoAprobado();
        }

        Optional<Certificacion> existente =
                certificacionRepository.findByIdAuditoria(comando.getIdAuditoria());
        if (existente.isPresent()) {
            log.info("Ya existe una certificacion para la auditoria {}; se omite la emision.",
                    comando.getIdAuditoria());
            return aDto(existente.get(), false);
        }

        DefinicionCertificacion definicion = catalogoTiposCertificacion.buscar(comando.getTipo())
                .orElseThrow(() -> {
                    log.error("Tipo de certificacion no reconocido: {}", comando.getTipo());
                    return ApiException.datosInvalidos("El tipo de certificacion no es valido.");
                });

        Empresa empresa = empresaRepository.findById(comando.getIdEmpresa())
                .orElseThrow(() -> {
                    log.error("No se emitio la certificacion de la auditoria {}: la empresa {} "
                            + "no existe.", comando.getIdAuditoria(), comando.getIdEmpresa());
                    return ApiException.recursoNoEncontrado("La empresa indicada no existe.");
                });

        Usuario auditor = usuarioRepository.findById(comando.getIdAuditor())
                .orElseThrow(() -> {
                    log.error("No se emitio la certificacion de la auditoria {}: el auditor {} "
                            + "no existe.", comando.getIdAuditoria(), comando.getIdAuditor());
                    return ApiException.recursoNoEncontrado("El auditor indicado no existe.");
                });

        if (!ValidacionesAuditor.esAuditorCertificadoActivo(auditor)) {
            log.error("No se emitio la certificacion de la auditoria {}: el usuario {} no es un "
                    + "auditor certificado activo.", comando.getIdAuditoria(), comando.getIdAuditor());
            throw ApiException.auditorNoValido();
        }

        LocalDate fechaVencimiento = resolverVencimiento(comando, definicion);

        // Reserva la posicion de esta certificacion en la lista de estado de
        // revocacion (W3C Bitstring Status List) antes de firmar: el indice debe
        // quedar embebido en la credencial desde su creacion para que sea
        // revocable en el futuro sin tener que reemitirla.
        //
        // Si mas abajo se detecta una emision concurrente ganada por otra
        // transaccion, este indice ya quedo reservado (y su insert se
        // confirma con el resto de esta transaccion) pero no lo usa ninguna
        // certificacion: queda un hueco permanente en la lista de estado.
        // Es inofensivo -- la lista tiene 131 072 posiciones y un hueco no
        // afecta la verificacion de ninguna credencial -- asi que se acepta
        // en vez de complicar la reserva con otra transaccion aparte.
        Long indiceEstado = indiceEstadoCertificacionRepository
                .save(new IndiceEstadoCertificacion())
                .getIndice();

        Certificacion certificacion = Certificacion.builder()
                .idAuditoria(comando.getIdAuditoria())
                .empresa(empresa)
                .auditor(auditor)
                .tipo(comando.getTipo())
                .fechaEmision(Instant.now())
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoCertificacion.ACTIVA)
                .indiceEstado(indiceEstado)
                .codigoVerificacion(generadorCodigoVerificacionService.generar())
                .build();

        certificacion.setCredencialJwt(
                generadorCredencialOpenBadges.generar(certificacion, definicion));

        for (int intento = 1;; intento++) {
            try {
                certificacion = certificacionPersistenciaService.guardar(certificacion);
                break;
            } catch (DataIntegrityViolationException e) {
                Optional<Certificacion> existentePorCarrera =
                        certificacionRepository.findByIdAuditoria(comando.getIdAuditoria());
                if (existentePorCarrera.isPresent()) {
                    // Dos aprobaciones simultaneas de la misma auditoria: la restriccion de
                    // unicidad sobre id_auditoria es la que garantiza que no haya duplicados.
                    log.info("Emision concurrente detectada para la auditoria {}; se reutiliza la "
                            + "certificacion existente.", comando.getIdAuditoria());
                    return aDto(existentePorCarrera.get(), false);
                }

                if (intento >= REINTENTOS_CODIGO_VERIFICACION) {
                    throw e;
                }
                // No fue la auditoria: la colision es en codigoVerificacion (dos emisiones
                // generaron el mismo codigo antes de que ninguna lo persistiera). Se genera
                // uno nuevo y se reintenta; la credencial firmada no lo referencia, asi que
                // no hace falta volver a firmar.
                log.warn("Colision de codigoVerificacion al emitir la auditoria {} (intento {} de "
                        + "{}); se genera uno nuevo.", comando.getIdAuditoria(), intento,
                        REINTENTOS_CODIGO_VERIFICACION);
                certificacion.setCodigoVerificacion(generadorCodigoVerificacionService.generar());
            }
        }

        notificacionPanelRepository.save(NotificacionPanel.builder()
                .empresa(empresa)
                .certificacion(certificacion)
                .mensaje("Se emitio la certificacion \"" + definicion.nombre() + "\".")
                .fechaCreacion(certificacion.getFechaEmision())
                .build());

        log.info("Certificacion {} emitida para la empresa {} por la auditoria {}.",
                definicion.tipo(), empresa.getId(), comando.getIdAuditoria());

        evaluarInsigniasTrasCommit(empresa.getId());

        return aDto(certificacion, true);
    }

    private void evaluarInsigniasTrasCommit(UUID empresaId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            insigniaEmpresaEvaluacionService.evaluarPorNuevaCertificacion(empresaId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                insigniaEmpresaEvaluacionService.evaluarPorNuevaCertificacion(empresaId);
            }
        });
    }

    /**
     * La vigencia la determina el tipo de certificacion. Si el llamante envio
     * una fecha explicita, se respeta solo si es posterior a la de la auditoria.
     */
    private LocalDate resolverVencimiento(EmitirCertificacionRequestDTO comando,
                                          DefinicionCertificacion definicion) {
        LocalDate solicitada = comando.getFechaVencimientoCert();
        if (solicitada == null) {
            return comando.getFechaAuditoria().plusMonths(definicion.vigenciaMeses());
        }
        if (!solicitada.isAfter(comando.getFechaAuditoria())) {
            log.error("No se emitio la certificacion de la auditoria {}: la fecha de vencimiento "
                            + "{} no es posterior a la fecha de auditoria {}.",
                    comando.getIdAuditoria(), solicitada, comando.getFechaAuditoria());
            throw ApiException.fechaVencimientoCertInvalida();
        }
        return solicitada;
    }

    private CertificacionResponseDTO aDto(Certificacion certificacion, boolean recienEmitida) {
        CertificacionResponseDTO dto = certificacionMapper.toDto(certificacion);
        dto.setRecienEmitida(recienEmitida);
        // Misma semantica que exp en el VC-JWT: vence a medianoche UTC del dia
        // de fechaVencimiento.
        dto.setVigente(certificacion.getFechaVencimiento().isAfter(LocalDate.now()));
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }
}
