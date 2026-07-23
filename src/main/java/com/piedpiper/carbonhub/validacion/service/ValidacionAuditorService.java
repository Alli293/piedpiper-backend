package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.mappers.ValidacionAuditorMapper;
import com.piedpiper.carbonhub.validacion.models.dtos.DecisionSolicitudRequestDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.RegistroAuditoriaInterna;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.DecisionSolicitud;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.RegistroAuditoriaInternaRepository;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class ValidacionAuditorService {

    public static final String TIPO_EVENTO = "validacion_auditor";
    public static final String DECISION_APROBADO = "aprobado";
    public static final String DECISION_RECHAZADO = "rechazado";
    private static final int PAGINA_TAMANO = 25;
    private static final int MOTIVO_MIN = 10;

    private final SolicitudValidacionRepository solicitudValidacionRepository;
    private final RegistroAuditoriaInternaRepository registroAuditoriaInternaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnvioCorreoValidacionService envioCorreoValidacionService;
    private final ValidacionAuditorMapper validacionAuditorMapper;

    public ValidacionAuditorService(SolicitudValidacionRepository solicitudValidacionRepository,
                                    RegistroAuditoriaInternaRepository registroAuditoriaInternaRepository,
                                    UsuarioRepository usuarioRepository,
                                    EnvioCorreoValidacionService envioCorreoValidacionService,
                                    ValidacionAuditorMapper validacionAuditorMapper) {
        this.solicitudValidacionRepository = solicitudValidacionRepository;
        this.registroAuditoriaInternaRepository = registroAuditoriaInternaRepository;
        this.usuarioRepository = usuarioRepository;
        this.envioCorreoValidacionService = envioCorreoValidacionService;
        this.validacionAuditorMapper = validacionAuditorMapper;
    }

    @Transactional(readOnly = true)
    public PaginaSolicitudesResponseDTO listarPendientes(UUID usuarioId, int pagina) {
        validarAdministradorPlataforma(usuarioId);
        Page<SolicitudValidacion> pendientes = solicitudValidacionRepository
                .findAllByEstadoOrderByFechaSolicitudAsc(
                        EstadoSolicitud.PENDIENTE, PageRequest.of(Math.max(pagina, 0), PAGINA_TAMANO));
        return new PaginaSolicitudesResponseDTO(
                pendientes.getContent().stream().map(validacionAuditorMapper::aPendienteDto).toList(),
                pendientes.getNumber(),
                pendientes.getTotalPages(),
                pendientes.getTotalElements());
    }

    @Transactional
    public SolicitudResueltaResponseDTO resolver(UUID usuarioId, UUID solicitudId,
                                                 DecisionSolicitudRequestDTO request) {
        Usuario administrador = validarAdministradorPlataforma(usuarioId);
        boolean aprobado = validarDecision(request);

        SolicitudValidacion solicitud = solicitudValidacionRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudNoEncontrada);

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw ApiException.solicitudYaProcesada();
        }

        Usuario auditor = solicitud.getAuditor();
        Instant ahora = Instant.now();

        solicitud.setEstado(aprobado ? EstadoSolicitud.APROBADO : EstadoSolicitud.RECHAZADO);
        solicitud.setFechaResolucion(ahora);
        solicitud.setResueltaPor(administrador);
        solicitud.setMotivoRechazo(aprobado ? null : request.getMotivoRechazo().trim());
        auditor.setEstado(aprobado ? EstadoUsuario.ACTIVO : EstadoUsuario.RECHAZADO);

        try {
            solicitudValidacionRepository.saveAndFlush(solicitud);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.solicitudConflictoConcurrente();
        }
        usuarioRepository.save(auditor);

        registroAuditoriaInternaRepository.save(RegistroAuditoriaInterna.builder()
                .tipoEvento(TIPO_EVENTO)
                .solicitudId(solicitud.getId())
                .decision(aprobado ? DECISION_APROBADO : DECISION_RECHAZADO)
                .administradorId(administrador.getId())
                .fechaEvento(ahora)
                .build());

        enviarCorreoTrasCommit(auditor.getNombre(), auditor.getEmail(), aprobado,
                solicitud.getMotivoRechazo());

        return validacionAuditorMapper.aResueltaDto(solicitud);
    }

    private boolean validarDecision(DecisionSolicitudRequestDTO request) {
        DecisionSolicitud decision = DecisionSolicitud.desde(request.getDecision())
                .orElseThrow(() -> ApiException.valorNoSoportado(
                        "La decisión debe ser 'aprobado' o 'rechazado'."));
        if (decision == DecisionSolicitud.RECHAZADO) {
            String motivo = request.getMotivoRechazo() == null ? "" : request.getMotivoRechazo().trim();
            if (motivo.length() < MOTIVO_MIN || motivo.length() > SolicitudValidacion.MOTIVO_MAX) {
                throw ApiException.valorNoSoportado(
                        "El motivo de rechazo debe tener entre 10 y 500 caracteres.");
            }
        }
        return decision == DecisionSolicitud.APROBADO;
    }

    private void enviarCorreoTrasCommit(String nombre, String email, boolean aprobado, String motivo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envioCorreoValidacionService.enviar(nombre, email, aprobado, motivo);
                }
            });
        } else {
            envioCorreoValidacionService.enviar(nombre, email, aprobado, motivo);
        }
    }

    private Usuario validarAdministradorPlataforma(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "Solo el administrador de la plataforma puede gestionar solicitudes."));
        if (usuario.getRol() != Rol.ADMINISTRADOR_PLATAFORMA) {
            throw ApiException.accesoDenegado(
                    "Solo el administrador de la plataforma puede gestionar solicitudes.");
        }
        return usuario;
    }

}
