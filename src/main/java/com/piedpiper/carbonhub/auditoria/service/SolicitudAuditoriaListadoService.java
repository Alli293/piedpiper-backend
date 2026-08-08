package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.DocumentoRespaldoRepository;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listados de solicitudes de auditoria, uno por cada rol que las mira.
 *
 * <p>Son dos consultas distintas y no una con filtros porque la pertenencia se resuelve distinto:
 * la empresa ve las suyas por {@code empresa_id}, y el auditor ve las que tiene asignadas. Ninguno
 * de los dos recibe un parametro para elegir de quien listar; sale del usuario autenticado, asi que
 * no hay forma de pedir el listado de otro.</p>
 */
@Service
public class SolicitudAuditoriaListadoService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRespaldoRepository documentoRespaldoRepository;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;

    public SolicitudAuditoriaListadoService(SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                            UsuarioRepository usuarioRepository,
                                            DocumentoRespaldoRepository documentoRespaldoRepository,
                                            SolicitudAuditoriaMapper solicitudAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoRespaldoRepository = documentoRespaldoRepository;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
    }

    @Transactional(readOnly = true)
    public List<SolicitudAuditoriaResumenResponseDTO> listarDeMiEmpresa(UUID usuarioId) {
        Usuario usuario = usuarioDe(usuarioId);
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return aResumenes(solicitudAuditoriaRepository.listarPorEmpresa(usuario.getEmpresa().getId()));
    }

    @Transactional(readOnly = true)
    public List<SolicitudAuditoriaResumenResponseDTO> listarAsignadasA(UUID usuarioId) {
        return aResumenes(solicitudAuditoriaRepository.listarAsignadasA(usuarioId));
    }

    /**
     * El conteo de adjuntos sale de una consulta agregada y no de {@code solicitud.getDocumentos()}:
     * esa coleccion es perezosa, asi que contarla por fila dispararia una consulta extra por
     * solicitud que ademas trae el contenido binario completo de cada PDF.
     */
    private List<SolicitudAuditoriaResumenResponseDTO> aResumenes(List<SolicitudAuditoria> solicitudes) {
        List<SolicitudAuditoriaResumenResponseDTO> resumenes =
                solicitudAuditoriaMapper.toResumenDtos(solicitudes);
        if (resumenes.isEmpty()) {
            return resumenes;
        }

        Map<UUID, Integer> conteos = new HashMap<>();
        documentoRespaldoRepository
                .contarPorSolicitud(solicitudes.stream().map(SolicitudAuditoria::getId).toList())
                .forEach(fila -> conteos.put((UUID) fila[0], ((Number) fila[1]).intValue()));

        resumenes.forEach(resumen ->
                resumen.setCantidadDocumentos(conteos.getOrDefault(resumen.getId(), 0)));
        return resumenes;
    }

    private Usuario usuarioDe(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
    }
}
