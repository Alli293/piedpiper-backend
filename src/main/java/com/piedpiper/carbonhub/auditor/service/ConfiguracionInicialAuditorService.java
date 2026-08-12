package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.CompletarConfiguracionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auditoria.service.TipoDocumentoAdjunto;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        // Lock de fila (mismo patron que VerificarCorreoService/RestablecerContrasenaService): sin
        // esto, dos submits concurrentes (doble clic, reintento de red en un POST multipart lento)
        // leen configuracionCompleta=false antes de que cualquiera de los dos haga commit, y ambos
        // pasan el guard de abajo -- resultando en dos SolicitudValidacion y dos juegos de
        // documentos para el mismo auditor.
        Usuario auditor = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        // Callejon sin salida intencional para RECHAZADO: no vuelve a PENDIENTE_VALIDACION, asi que
        // este guard tambien lo bloquea. No hay endpoint para corregir y reenviar; el auditor
        // rechazado tiene que contactar a soporte. RedirectResolver nunca lo trae a esta pantalla
        // (isConfiguracionCompleta ya es true para el), asi que en la practica esto solo se alcanza
        // llamando al endpoint directamente.
        if (auditor.getEstado() != EstadoUsuario.PENDIENTE_VALIDACION || auditor.isConfiguracionCompleta()) {
            throw ApiException.configuracionAuditorNoDisponible();
        }

        Set<EspecialidadAuditor> especialidades = resolverEspecialidades(datos.getEspecialidades());
        List<byte[]> contenidosDocumentos = validadorDocumentosPdf.validarYLeer(
                documentos, TipoDocumentoAdjunto.CREDENCIAL_AUDITOR);
        validarMetadatosDocumentos(documentos);

        PerfilAuditor perfil = perfilAuditorService.asegurarPerfil(auditor);

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

        guardarDocumentos(solicitud, documentos, contenidosDocumentos, ahora);

        auditor.setConfiguracionCompleta(true);
        usuarioRepository.save(auditor);

        return new MensajeResponseDTO(
                "Recibimos tu información. Tu cuenta de auditor está en revisión; "
                        + "te avisaremos cuando el administrador la apruebe.");
    }

    private Set<EspecialidadAuditor> resolverEspecialidades(List<String> especialidades) {
        return Catalogos.resolverConjunto(EspecialidadAuditor.class, especialidades,
                () -> ApiException.datosInvalidos("La lista de especialidades contiene duplicados."),
                ApiException::especialidadesInvalidas);
    }

    private void validarMetadatosDocumentos(List<MultipartFile> documentos) {
        for (MultipartFile documento : documentos) {
            String nombreArchivo = documento.getOriginalFilename();
            String tipoContenido = documento.getContentType();
            if (nombreArchivo == null || nombreArchivo.isBlank() || tipoContenido == null || tipoContenido.isBlank()) {
                throw ApiException.documentoCredencialMetadatosInvalidos();
            }
        }
    }

    private void guardarDocumentos(SolicitudValidacion solicitud, List<MultipartFile> documentos,
                                   List<byte[]> contenidosDocumentos, Instant ahora) {
        List<DocumentoCredencialAuditor> entidades = new ArrayList<>(documentos.size());
        for (int i = 0; i < documentos.size(); i++) {
            MultipartFile documento = documentos.get(i);
            entidades.add(DocumentoCredencialAuditor.builder()
                    .solicitud(solicitud)
                    .nombreArchivo(documento.getOriginalFilename())
                    .tipoContenido(documento.getContentType())
                    .tamanioBytes(documento.getSize())
                    .contenido(contenidosDocumentos.get(i))
                    .fechaCarga(ahora)
                    .build());
        }
        documentoCredencialAuditorRepository.saveAll(entidades);
    }
}
