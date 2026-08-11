package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.ResultadoAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ResultadoAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ResultadoAuditoriaService {

    /**
     * La solicitud de auditoria guarda si el tramite es inicial o renovacion, no el producto
     * certificable. Mientras PP-48 no capture ese dato, la aprobacion emite Inventario GEI, y de
     * ese tipo sale tambien el tope de vigencia.
     */
    private static final TipoCertificacion TIPO_EMITIDO = TipoCertificacion.INVENTARIO_GEI;

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    private final TransicionEstadoAuditoriaService transicionEstadoAuditoriaService;
    private final EmisionCertificacionPort emisionCertificacionPort;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;
    private final TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper;

    public ResultadoAuditoriaService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            TransicionEstadoAuditoriaService transicionEstadoAuditoriaService,
            EmisionCertificacionPort emisionCertificacionPort,
            CatalogoTiposCertificacion catalogoTiposCertificacion,
            SolicitudAuditoriaMapper solicitudAuditoriaMapper,
            TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.transicionEstadoAuditoriaService = transicionEstadoAuditoriaService;
        this.emisionCertificacionPort = emisionCertificacionPort;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
        this.transicionEstadoAuditoriaMapper = transicionEstadoAuditoriaMapper;
    }

    @Transactional
    public SolicitudAuditoriaDetalleResponseDTO emitir(UUID solicitudId,
                                                       ResultadoAuditoriaRequestDTO datos,
                                                       UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);
        Usuario auditor = validarAuditorAsignado(solicitud, usuarioId);
        validarEstadoPermiteResultado(solicitud);

        ResultadoAuditoria resultado = ResultadoAuditoria.desde(datos.getResultado())
                .orElseThrow(ApiException::resultadoAuditoriaInvalido);

        solicitud.setResultadoAuditoria(resultado);
        solicitud.setFechaResolucion(Instant.now());

        if (resultado == ResultadoAuditoria.APROBADA) {
            solicitud.setFechaVencimientoCert(
                    validarVencimiento(datos.getFechaVencimientoCert(), solicitud));
            aplicar(solicitud, EventoTransicionAuditoria.RESULTADO_APROBADA, auditor);
            SolicitudAuditoria guardada = guardar(solicitud);
            emitirCertificacionTrasCommit(comandoEmision(guardada, auditor));
            return detalleDe(guardada);
        }

        solicitud.setObservaciones(validarObservaciones(datos.getObservaciones()));
        aplicar(solicitud, EventoTransicionAuditoria.RESULTADO_OBSERVACIONES, auditor);
        return detalleDe(guardar(solicitud));
    }

    /**
     * Una certificacion que vence antes de la auditoria que la sustenta nace invalida, asi que la
     * comparacion es contra {@code fechaAuditoriaRealizada} y no contra hoy: el auditor puede
     * registrar el resultado dias despues de haber hecho la auditoria.
     *
     * <p>El limite superior sale del catalogo y no es negociable por el auditor. Antes de PP-49 la
     * vigencia siempre se derivaba de {@code vigenciaMeses}; al dejar que el auditor la escriba,
     * sin tope un simple error de tipeo ({@code 2099-08-05}) emitiria una certificacion valida por
     * decadas y firmada, que es exactamente lo que el catalogo existe para impedir. El auditor
     * puede acortar la vigencia, nunca estirarla.</p>
     */
    private LocalDate validarVencimiento(LocalDate fechaVencimiento, SolicitudAuditoria solicitud) {
        if (fechaVencimiento == null) {
            throw ApiException.fechaVencimientoCertRequerida();
        }
        if (!fechaVencimiento.isAfter(solicitud.getFechaAuditoriaRealizada())) {
            throw ApiException.fechaVencimientoCertInvalida();
        }
        if (fechaVencimiento.isAfter(vencimientoMaximo(solicitud))) {
            throw ApiException.fechaVencimientoCertExcedeVigencia(vigenciaMeses());
        }
        return fechaVencimiento;
    }

    private LocalDate vencimientoMaximo(SolicitudAuditoria solicitud) {
        return solicitud.getFechaAuditoriaRealizada().plusMonths(vigenciaMeses());
    }

    /**
     * La vigencia del unico tipo que hoy emite una aprobacion. Sale del catalogo y no de una
     * constante local para que, si alguien cambia la vigencia ahi, este tope la siga sin que nadie
     * tenga que acordarse de este archivo.
     */
    private int vigenciaMeses() {
        return catalogoTiposCertificacion.buscar(TIPO_EMITIDO)
                .orElseThrow(() -> ApiException.errorInterno(
                        "No se pudo determinar la vigencia de la certificación."))
                .vigenciaMeses();
    }

    /**
     * El minimo de 20 caracteres existe porque estas observaciones son la unica instruccion que
     * recibe la empresa sobre que corregir: un "revisar" suelto la obliga a volver a preguntar.
     */
    private String validarObservaciones(String observaciones) {
        String limpio = observaciones == null ? "" : observaciones.trim();
        if (limpio.length() < SolicitudAuditoria.OBSERVACIONES_MIN) {
            throw ApiException.observacionesResultadoRequeridas();
        }
        if (limpio.length() > SolicitudAuditoria.OBSERVACIONES_MAX) {
            throw ApiException.observacionesResultadoExcedidas();
        }
        return limpio;
    }

    private Usuario validarAuditorAsignado(SolicitudAuditoria solicitud, UUID usuarioId) {
        Usuario auditor = solicitud.getAuditor();
        if (auditor == null || !auditor.getId().equals(usuarioId)) {
            throw ApiException.resultadoAuditoriaAjena();
        }
        return auditor;
    }

    private void validarEstadoPermiteResultado(SolicitudAuditoria solicitud) {
        if (solicitud.getEstado() != EstadoSolicitudAuditoria.REPORTE_CARGADO
                || solicitud.getReporteAuditoria() == null
                || solicitud.getFechaAuditoriaRealizada() == null) {
            throw ApiException.resultadoAuditoriaNoDisponible();
        }
    }

    private void aplicar(SolicitudAuditoria solicitud,
                         EventoTransicionAuditoria evento,
                         Usuario auditor) {
        transicionEstadoAuditoriaService.aplicar(
                solicitud, evento, ActorTransicionAuditoria.AUDITOR, auditor);
    }

    private EmitirCertificacionRequestDTO comandoEmision(SolicitudAuditoria solicitud, Usuario auditor) {
        return new EmitirCertificacionRequestDTO(
                solicitud.getId(),
                solicitud.getEmpresa().getId(),
                auditor.getId(),
                ResultadoAuditoria.APROBADA.getCodigo(),
                solicitud.getFechaAuditoriaRealizada(),
                TIPO_EMITIDO,
                solicitud.getFechaVencimientoCert());
    }

    private void emitirCertificacionTrasCommit(EmitirCertificacionRequestDTO comando) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emisionCertificacionPort.emitirPorAuditoriaAprobada(comando);
                }
            });
        } else {
            emisionCertificacionPort.emitirPorAuditoriaAprobada(comando);
        }
    }

    private SolicitudAuditoria guardar(SolicitudAuditoria solicitud) {
        try {
            return solicitudAuditoriaRepository.saveAndFlush(solicitud);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.resultadoAuditoriaNoDisponible();
        }
    }

    private SolicitudAuditoriaDetalleResponseDTO detalleDe(SolicitudAuditoria solicitud) {
        SolicitudAuditoriaDetalleResponseDTO detalle = solicitudAuditoriaMapper.toDetalleDto(solicitud);
        detalle.setHistorial(transicionEstadoAuditoriaMapper.toDtos(
                transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(solicitud.getId())));
        return detalle;
    }
}
