package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.CrearSolicitudAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SolicitudAuditoriaService {

    private static final int MESES_MAXIMOS_PERIODO = 12;

    private static final Set<EstadoSolicitudAuditoria> ESTADOS_CERRADOS = EnumSet.of(
            EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
            EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES);

    private static final Logger log = LoggerFactory.getLogger(SolicitudAuditoriaService.class);

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CertificacionActivaConsulta certificacionActivaConsulta;
    private final ValidadorDocumentosPdf validadorDocumentosPdf;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;

    public SolicitudAuditoriaService(SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                     UsuarioRepository usuarioRepository,
                                     CertificacionActivaConsulta certificacionActivaConsulta,
                                     ValidadorDocumentosPdf validadorDocumentosPdf,
                                     SolicitudAuditoriaMapper solicitudAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.certificacionActivaConsulta = certificacionActivaConsulta;
        this.validadorDocumentosPdf = validadorDocumentosPdf;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
    }

    @Transactional
    public SolicitudAuditoriaResponseDTO crear(CrearSolicitudAuditoriaRequestDTO datos,
                                               List<MultipartFile> documentos,
                                               UUID usuarioId) {
        Empresa empresa = empresaDe(usuarioId);

        Optional<LocalDate> vencimiento =
                certificacionActivaConsulta.fechaVencimientoCertificacionActiva(empresa.getId());
        TipoCertificacionSolicitud tipoCertificacion = vencimiento.isPresent()
                ? TipoCertificacionSolicitud.RENOVACION
                : TipoCertificacionSolicitud.INICIAL;
        LocalDate periodoInicio = vencimiento
                .map(fecha -> fecha.plusDays(1))
                .orElseGet(datos::getPeriodoInicio);

        validarPeriodo(tipoCertificacion, periodoInicio, datos.getPeriodoFin());
        validadorDocumentosPdf.validar(documentos);
        validarTraslape(empresa.getId(), periodoInicio, datos.getPeriodoFin());

        Instant ahora = Instant.now();
        SolicitudAuditoria solicitud = SolicitudAuditoria.builder()
                .empresa(empresa)
                .tipoCertificacion(tipoCertificacion)
                .periodoInicio(periodoInicio)
                .periodoFin(datos.getPeriodoFin())
                .descripcionSolicitud(datos.getDescripcionSolicitud())
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .fechaCreacion(ahora)
                .documentos(new ArrayList<>())
                .build();

        documentos.forEach(documento -> solicitud.agregarDocumento(documentoRespaldoDe(documento, ahora)));

        return solicitudAuditoriaMapper.toDto(solicitudAuditoriaRepository.save(solicitud));
    }

    private Empresa empresaDe(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa();
    }

    private void validarPeriodo(TipoCertificacionSolicitud tipoCertificacion,
                                LocalDate periodoInicio,
                                LocalDate periodoFin) {
        if (tipoCertificacion == TipoCertificacionSolicitud.INICIAL
                && periodoInicio.isAfter(LocalDate.now())) {
            throw ApiException.periodoAuditoriaFuturo();
        }
        if (!periodoFin.isAfter(periodoInicio)) {
            throw ApiException.periodoAuditoriaFinInvalido();
        }
        if (periodoFin.isAfter(periodoInicio.plusMonths(MESES_MAXIMOS_PERIODO))) {
            throw ApiException.periodoAuditoriaExcedeDoceMeses();
        }
    }

    private void validarTraslape(UUID empresaId, LocalDate periodoInicio, LocalDate periodoFin) {
        if (solicitudAuditoriaRepository.existeSolicitudEnCursoTraslapada(
                empresaId, ESTADOS_CERRADOS, periodoInicio, periodoFin)) {
            throw ApiException.solicitudAuditoriaTraslapada();
        }
    }

    private DocumentoRespaldo documentoRespaldoDe(MultipartFile documento, Instant fechaCarga) {
        try {
            return DocumentoRespaldo.builder()
                    .nombreArchivo(documento.getOriginalFilename())
                    .tipoContenido(documento.getContentType())
                    .tamanioBytes(documento.getSize())
                    .contenido(documento.getBytes())
                    .fechaCarga(fechaCarga)
                    .build();
        } catch (IOException e) {
            log.error("No se pudo leer el documento de respaldo {}", documento.getOriginalFilename(), e);
            throw ApiException.errorInterno("No se pudo guardar uno de los archivos adjuntos. Intenta nuevamente.");
        }
    }
}
