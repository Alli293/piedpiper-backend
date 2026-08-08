package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Entrega el contenido de un documento de respaldo para previsualizarlo.
 *
 * <p>Reusa exactamente la misma regla de acceso que el detalle: los documentos son parte de la
 * solicitud, asi que quien puede ver el detalle puede abrirlos, y nadie mas. Tener dos reglas
 * distintas para el mismo recurso es como se filtra un documento sin que nadie lo note.</p>
 */
@Service
public class DocumentoRespaldoDescargaService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoSolicitudAuditoria accesoSolicitudAuditoria;

    public DocumentoRespaldoDescargaService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            AccesoSolicitudAuditoria accesoSolicitudAuditoria) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.accesoSolicitudAuditoria = accesoSolicitudAuditoria;
    }

    @Transactional(readOnly = true)
    public DocumentoRespaldo obtener(UUID solicitudId, UUID documentoId, UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (!accesoSolicitudAuditoria.puedeConsultar(solicitud, usuario)) {
            throw ApiException.solicitudAuditoriaAjena();
        }

        return solicitud.getDocumentos().stream()
                .filter(documento -> documento.getId().equals(documentoId))
                .findFirst()
                .orElseThrow(ApiException::documentoRespaldoNoEncontrado);
    }
}
