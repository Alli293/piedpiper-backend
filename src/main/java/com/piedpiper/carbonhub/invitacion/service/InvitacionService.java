package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.mappers.InvitacionMapper;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionPublicaResponseDTO;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionRequestDTO;
import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionResponseDTO;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.models.enums.EstadoInvitacion;
import com.piedpiper.carbonhub.invitacion.repository.InvitacionRepository;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitacionService {

    private static final long DIAS_EXPIRACION = 7;
    private static final long HORAS_VENTANA_EMISION = 1;

    private final InvitacionRepository invitacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final EnvioCorreoInvitacionService envioCorreoInvitacionService;
    private final InvitacionMapper invitacionMapper;
    private final int maxEmisionesPorHora;

    public InvitacionService(InvitacionRepository invitacionRepository,
                             UsuarioRepository usuarioRepository,
                             EmpresaRepository empresaRepository,
                             EnvioCorreoInvitacionService envioCorreoInvitacionService,
                             InvitacionMapper invitacionMapper,
                             @Value("${invitacion.max-emisiones-por-hora:20}") int maxEmisionesPorHora) {
        if (maxEmisionesPorHora < 1) {
            throw new IllegalArgumentException("invitacion.max-emisiones-por-hora debe ser mayor que cero.");
        }
        this.invitacionRepository = invitacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.envioCorreoInvitacionService = envioCorreoInvitacionService;
        this.invitacionMapper = invitacionMapper;
        this.maxEmisionesPorHora = maxEmisionesPorHora;
    }

    @Transactional
    public InvitacionResponseDTO emitir(UUID usuarioId, InvitacionRequestDTO request) {
        Usuario administrador = validarAdministrador(usuarioId);
        UUID empresaId = administrador.getEmpresa().getId();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Instant ahora = Instant.now();

        empresaRepository.bloquearPorId(empresaId)
                .orElseThrow(ApiException::empresaDeInvitacionNoEncontrada);
        // El límite protege los correos emitidos. Revocar o expirar una invitación no deshace
        // el envío y, por tanto, no recupera cuota dentro de la ventana.
        long emitidasEnVentana = invitacionRepository.countByEmpresaIdAndFechaEmisionGreaterThanEqual(
                empresaId, ahora.minus(HORAS_VENTANA_EMISION, ChronoUnit.HOURS));
        if (emitidasEnVentana >= maxEmisionesPorHora) {
            throw ApiException.limiteInvitacionesExcedido();
        }

        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            if (usuario.getEmpresa() != null && empresaId.equals(usuario.getEmpresa().getId())) {
                throw ApiException.invitacionCorreoYaEnEmpresa();
            }
        });

        if (invitacionRepository.existsByEmpresaIdAndEmailIgnoreCaseAndEstadoAndFechaExpiracionAfter(
                empresaId, email, EstadoInvitacion.ENVIADA, ahora)) {
            throw ApiException.invitacionPendiente();
        }

        String token = TokenVerificacionGenerator.generar();

        Invitacion invitacion = Invitacion.builder()
                .email(email)
                .empresa(administrador.getEmpresa())
                .tokenHash(TokenVerificacionGenerator.hash(token))
                .estado(EstadoInvitacion.ENVIADA)
                .fechaEmision(ahora)
                .fechaExpiracion(ahora.plus(DIAS_EXPIRACION, ChronoUnit.DAYS))
                .build();

        invitacion = invitacionRepository.save(invitacion);

        enviarTrasCommit(email, administrador.getEmpresa().getNombreEmpresa(), token);

        return aDto(invitacion, ahora);
    }

    private void enviarTrasCommit(String email, String nombreEmpresa, String token) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envioCorreoInvitacionService.enviar(email, nombreEmpresa, token);
                }
            });
        } else {
            envioCorreoInvitacionService.enviar(email, nombreEmpresa, token);
        }
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponseDTO> listar(UUID usuarioId) {
        Usuario administrador = validarAdministrador(usuarioId);
        Instant ahora = Instant.now();
        return invitacionRepository
                .findAllByEmpresaIdOrderByFechaEmisionDesc(administrador.getEmpresa().getId())
                .stream()
                .map(invitacion -> aDto(invitacion, ahora))
                .toList();
    }

    @Transactional
    public InvitacionResponseDTO revocar(UUID usuarioId, UUID invitacionId) {
        Usuario administrador = validarAdministrador(usuarioId);
        Instant ahora = Instant.now();

        Invitacion invitacion = invitacionRepository
                .findByIdAndEmpresaId(invitacionId, administrador.getEmpresa().getId())
                .orElseThrow(ApiException::invitacionNoEncontrada);

        if (invitacion.estadoEfectivo(ahora) != EstadoInvitacion.ENVIADA) {
            throw ApiException.invitacionNoRevocable();
        }

        invitacion.setEstado(EstadoInvitacion.REVOCADA);
        try {
            invitacion = invitacionRepository.saveAndFlush(invitacion);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.invitacionNoDisponible();
        }

        return aDto(invitacion, ahora);
    }

    @Transactional(readOnly = true)
    public InvitacionPublicaResponseDTO resolver(String token) {
        Invitacion invitacion = validarParaAceptarInterno(token);
        return new InvitacionPublicaResponseDTO(
                enmascararEmail(invitacion.getEmail()), invitacion.getEmpresa().getNombreEmpresa());
    }

    @Transactional(readOnly = true)
    public Invitacion validarParaAceptar(String token) {
        return validarParaAceptarInterno(token);
    }

    private Invitacion validarParaAceptarInterno(String token) {
        if (!TokenVerificacionGenerator.formatoValido(token)) {
            throw ApiException.invitacionInvalida();
        }
        Invitacion invitacion = invitacionRepository
                .findByTokenHash(TokenVerificacionGenerator.hash(token))
                .orElseThrow(ApiException::invitacionInvalida);

        if (invitacion.getEstado() == EstadoInvitacion.ACEPTADA) {
            throw ApiException.invitacionYaUtilizada();
        }
        if (invitacion.getEstado() == EstadoInvitacion.REVOCADA) {
            throw ApiException.invitacionNoDisponible();
        }
        if (invitacion.estadoEfectivo(Instant.now()) == EstadoInvitacion.EXPIRADA) {
            throw ApiException.invitacionExpirada();
        }
        return invitacion;
    }

    @Transactional
    public void marcarAceptada(Invitacion invitacion) {
        if (invitacion.getEstado() != EstadoInvitacion.ENVIADA) {
            throw ApiException.invitacionNoDisponible();
        }
        invitacion.setEstado(EstadoInvitacion.ACEPTADA);
        invitacion.setFechaAceptacion(Instant.now());
        try {
            invitacionRepository.saveAndFlush(invitacion);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.invitacionNoDisponible();
        }
    }

    private Usuario validarAdministrador(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "Solo el administrador de la empresa puede gestionar invitaciones."));
        if (usuario.getRol() != Rol.ADMINISTRADOR_EMPRESA) {
            throw ApiException.accesoDenegado(
                    "Solo el administrador de la empresa puede gestionar invitaciones.");
        }
        if (usuario.getEmpresa() == null) {
            throw ApiException.invitacionSinEmpresa();
        }
        return usuario;
    }

    private InvitacionResponseDTO aDto(Invitacion invitacion, Instant ahora) {
        InvitacionResponseDTO dto = invitacionMapper.toDto(invitacion);
        dto.setEstado(invitacion.estadoEfectivo(ahora).name());
        return dto;
    }

    private String enmascararEmail(String email) {
        int separador = email.indexOf('@');
        if (separador <= 0 || separador == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, separador);
        return "%s***%s".formatted(local.substring(0, 1), email.substring(separador));
    }
}
