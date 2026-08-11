package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.CompletarConfiguracionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auditoria.service.ValidadorDocumentosPdf;
import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.DocumentoCredencialAuditorRepository;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConfiguracionInicialAuditorService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAuditorRepository perfilAuditorRepository;
    private final PerfilAuditorService perfilAuditorService;
    private final SolicitudValidacionRepository solicitudValidacionRepository;
    private final DocumentoCredencialAuditorRepository documentoCredencialAuditorRepository;
    private final ValidadorDocumentosPdf validadorDocumentosPdf;

    public ConfiguracionInicialAuditorService(UsuarioRepository usuarioRepository,
                                              PerfilAuditorRepository perfilAuditorRepository,
                                              PerfilAuditorService perfilAuditorService,
                                              SolicitudValidacionRepository solicitudValidacionRepository,
                                              DocumentoCredencialAuditorRepository documentoCredencialAuditorRepository,
                                              ValidadorDocumentosPdf validadorDocumentosPdf) {
        this.usuarioRepository = usuarioRepository;
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.perfilAuditorService = perfilAuditorService;
        this.solicitudValidacionRepository = solicitudValidacionRepository;
        this.documentoCredencialAuditorRepository = documentoCredencialAuditorRepository;
        this.validadorDocumentosPdf = validadorDocumentosPdf;
    }

    @Transactional
    public MensajeResponseDTO completar(UUID usuarioId, CompletarConfiguracionAuditorRequestDTO datos,
                                        List<MultipartFile> documentos) {
        Usuario auditor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (auditor.getEstado() != EstadoUsuario.PENDIENTE_VALIDACION || auditor.isConfiguracionCompleta()) {
            throw ApiException.configuracionAuditorNoDisponible();
        }

        Set<EspecialidadAuditor> especialidades = resolverEspecialidades(datos.getEspecialidades());
        validadorDocumentosPdf.validar(documentos);

        perfilAuditorService.asegurarPerfil(auditor);
        PerfilAuditor perfil = perfilAuditorRepository.findByAuditorId(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo crear el perfil del auditor."));

        perfil.setAniosExperiencia(datos.getAniosExperiencia());
        perfil.setEspecialidades(especialidades);
        perfil.setDescripcionProfesional(datos.getDescripcionProfesional());
        perfil.setSitioWeb(datos.getSitioWeb());
        perfil.setActualizadoEn(Instant.now());
        perfilAuditorRepository.save(perfil);

        Instant ahora = Instant.now();
        SolicitudValidacion solicitud = solicitudValidacionRepository.save(SolicitudValidacion.builder()
                .auditor(auditor)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(ahora)
                .build());

        guardarDocumentos(solicitud, documentos, ahora);

        auditor.setConfiguracionCompleta(true);
        usuarioRepository.save(auditor);

        return new MensajeResponseDTO(
                "Recibimos tu información. Tu cuenta de auditor está en revisión; "
                        + "te avisaremos cuando el administrador la apruebe.");
    }

    private Set<EspecialidadAuditor> resolverEspecialidades(List<String> especialidades) {
        if (especialidades.size() != new HashSet<>(especialidades).size()) {
            throw ApiException.datosInvalidos("La lista de especialidades contiene duplicados.");
        }
        List<String> invalidas = especialidades.stream()
                .filter(e -> Catalogos.desde(EspecialidadAuditor.class, e).isEmpty())
                .toList();
        if (!invalidas.isEmpty()) {
            throw ApiException.especialidadesInvalidas(invalidas);
        }
        return especialidades.stream()
                .map(e -> Catalogos.desde(EspecialidadAuditor.class, e).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void guardarDocumentos(SolicitudValidacion solicitud, List<MultipartFile> documentos, Instant ahora) {
        for (MultipartFile documento : documentos) {
            try {
                documentoCredencialAuditorRepository.save(DocumentoCredencialAuditor.builder()
                        .solicitud(solicitud)
                        .nombreArchivo(documento.getOriginalFilename())
                        .tipoContenido(documento.getContentType())
                        .tamanioBytes(documento.getSize())
                        .contenido(documento.getBytes())
                        .fechaCarga(ahora)
                        .build());
            } catch (IOException e) {
                throw ApiException.errorInterno("No se pudo guardar uno de tus documentos. Intenta nuevamente.");
            }
        }
    }
}
