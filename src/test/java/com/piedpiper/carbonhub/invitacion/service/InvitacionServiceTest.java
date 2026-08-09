package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.mappers.InvitacionMapperImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitacionServiceTest {

    @Mock
    private InvitacionRepository invitacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private EnvioCorreoInvitacionService envioCorreoInvitacionService;

    private InvitacionService service() {
        return new InvitacionService(
                invitacionRepository, usuarioRepository, empresaRepository,
                envioCorreoInvitacionService, new InvitacionMapperImpl(), 20);
    }

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final String TOKEN_VALIDO = "a".repeat(43);

    @BeforeEach
    void prepararLimiteDeInvitaciones() {
        lenient().when(empresaRepository.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa()));
        lenient().when(invitacionRepository.countByEmpresaIdAndFechaEmisionGreaterThanEqual(
                eq(EMPRESA_ID), any())).thenReturn(0L);
    }

    private Empresa empresa() {
        return Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build();
    }

    private Usuario administrador() {
        return Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_EMPRESA).empresa(empresa()).build();
    }

    private Invitacion invitacion(EstadoInvitacion estado, Instant expiracion) {
        return Invitacion.builder()
                .id(UUID.randomUUID())
                .email("colab@correo.com")
                .empresa(empresa())
                .tokenHash("hash")
                .estado(estado)
                .fechaEmision(Instant.now().minus(1, ChronoUnit.DAYS))
                .fechaExpiracion(expiracion)
                .build();
    }

    @Test
    void emitirCreaInvitacionConExpiracionDeSieteDiasYEnviaCorreo() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(usuarioRepository.findByEmailIgnoreCase("colab@correo.com")).thenReturn(Optional.empty());
        when(invitacionRepository.existsByEmpresaIdAndEmailIgnoreCaseAndEstadoAndFechaExpiracionAfter(
                eq(EMPRESA_ID), eq("colab@correo.com"), eq(EstadoInvitacion.ENVIADA), any()))
                .thenReturn(false);
        when(invitacionRepository.save(any(Invitacion.class))).thenAnswer(i -> i.getArgument(0));

        InvitacionResponseDTO response = service().emitir(
                ADMIN_ID, new InvitacionRequestDTO("colab@correo.com"));

        ArgumentCaptor<Invitacion> captor = ArgumentCaptor.forClass(Invitacion.class);
        verify(invitacionRepository).save(captor.capture());
        Invitacion guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoInvitacion.ENVIADA);
        assertThat(guardada.getTokenHash()).isNotBlank();
        assertThat(guardada.getFechaExpiracion())
                .isCloseTo(guardada.getFechaEmision().plus(7, ChronoUnit.DAYS),
                        org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES));
        verify(envioCorreoInvitacionService).enviar(eq("colab@correo.com"), eq("Acme S.A."), any());
        assertThat(response.getEstado()).isEqualTo("ENVIADA");
    }

    @Test
    void emitirConCorreoDeUsuarioDeLaMismaEmpresaLanza409YNoPersiste() {
        Usuario existente = Usuario.builder().id(UUID.randomUUID()).empresa(empresa()).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(usuarioRepository.findByEmailIgnoreCase("colab@correo.com")).thenReturn(Optional.of(existente));

        InvitacionService servicio = service();
        InvitacionRequestDTO request = new InvitacionRequestDTO("colab@correo.com");
        assertThatThrownBy(() -> servicio.emitir(ADMIN_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void emitirNormalizaElCorreoYDetectaAlUsuarioAunConDistintaCapitalizacion() {
        Usuario existente = Usuario.builder().id(UUID.randomUUID()).empresa(empresa()).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(usuarioRepository.findByEmailIgnoreCase("colab@correo.com"))
                .thenReturn(Optional.of(existente));

        InvitacionService servicio = service();
        InvitacionRequestDTO request = new InvitacionRequestDTO("  Colab@Correo.COM  ");
        assertThatThrownBy(() -> servicio.emitir(ADMIN_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void emitirConInvitacionPendienteLanza409YNoPersiste() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(usuarioRepository.findByEmailIgnoreCase("colab@correo.com")).thenReturn(Optional.empty());
        when(invitacionRepository.existsByEmpresaIdAndEmailIgnoreCaseAndEstadoAndFechaExpiracionAfter(
                eq(EMPRESA_ID), eq("colab@correo.com"), eq(EstadoInvitacion.ENVIADA), any()))
                .thenReturn(true);

        InvitacionService servicio = service();
        InvitacionRequestDTO request = new InvitacionRequestDTO("colab@correo.com");
        assertThatThrownBy(() -> servicio.emitir(ADMIN_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void emitirCuandoAlcanzaElLimitePorHoraLanza429YNoPersiste() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(invitacionRepository.countByEmpresaIdAndFechaEmisionGreaterThanEqual(
                eq(EMPRESA_ID), any())).thenReturn(20L);

        InvitacionService servicio = service();

        assertThatThrownBy(() -> servicio.emitir(
                ADMIN_ID, new InvitacionRequestDTO("otra@correo.com")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void emitirSinRolAdministradorLanza403() {
        Usuario general = Usuario.builder().id(ADMIN_ID).rol(Rol.USUARIO_GENERAL).empresa(empresa()).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(general));

        InvitacionService servicio = service();
        InvitacionRequestDTO request = new InvitacionRequestDTO("colab@correo.com");
        assertThatThrownBy(() -> servicio.emitir(ADMIN_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void emitirSinEmpresaConfiguradaLanza422() {
        Usuario sinEmpresa = Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_EMPRESA).build();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(sinEmpresa));

        InvitacionService servicio = service();
        InvitacionRequestDTO request = new InvitacionRequestDTO("colab@correo.com");
        assertThatThrownBy(() -> servicio.emitir(ADMIN_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void listarMarcaComoExpiradasLasEnviadasVencidas() {
        Invitacion vigente = invitacion(EstadoInvitacion.ENVIADA, Instant.now().plus(1, ChronoUnit.DAYS));
        Invitacion vencida = invitacion(EstadoInvitacion.ENVIADA, Instant.now().minus(1, ChronoUnit.HOURS));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(invitacionRepository.findAllByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID))
                .thenReturn(List.of(vigente, vencida));

        List<InvitacionResponseDTO> lista = service().listar(ADMIN_ID);

        assertThat(lista).extracting(InvitacionResponseDTO::getEstado)
                .containsExactly("ENVIADA", "EXPIRADA");
    }

    @Test
    void revocarCambiaElEstadoARevocada() {
        Invitacion enviada = invitacion(EstadoInvitacion.ENVIADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(invitacionRepository.findByIdAndEmpresaId(enviada.getId(), EMPRESA_ID))
                .thenReturn(Optional.of(enviada));
        when(invitacionRepository.saveAndFlush(any(Invitacion.class))).thenAnswer(i -> i.getArgument(0));

        InvitacionResponseDTO response = service().revocar(ADMIN_ID, enviada.getId());

        assertThat(response.getEstado()).isEqualTo("REVOCADA");
    }

    @Test
    void revocarInvitacionNoPendienteLanza409() {
        Invitacion revocada = invitacion(EstadoInvitacion.REVOCADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(invitacionRepository.findByIdAndEmpresaId(revocada.getId(), EMPRESA_ID))
                .thenReturn(Optional.of(revocada));

        InvitacionService servicio = service();
        UUID invitacionId = revocada.getId();
        assertThatThrownBy(() -> servicio.revocar(ADMIN_ID, invitacionId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void revocarInvitacionDeOtraEmpresaLanza404() {
        UUID otraInvitacion = UUID.randomUUID();
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrador()));
        when(invitacionRepository.findByIdAndEmpresaId(otraInvitacion, EMPRESA_ID))
                .thenReturn(Optional.empty());

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.revocar(ADMIN_ID, otraInvitacion))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resolverTokenValidoDevuelveCorreoEnmascaradoYEmpresa() {
        String token = TOKEN_VALIDO;
        Invitacion enviada = invitacion(EstadoInvitacion.ENVIADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitacionRepository.findByTokenHash(TokenVerificacionGenerator.hash(token)))
                .thenReturn(Optional.of(enviada));

        InvitacionPublicaResponseDTO response = service().resolver(token);

        assertThat(response.getEmailEnmascarado()).isEqualTo("c***@correo.com");
        assertThat(response.getNombreEmpresa()).isEqualTo("Acme S.A.");
    }

    @Test
    void resolverTokenInexistenteLanza404() {
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.resolver(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resolverTokenEnviadoVencidoPorFechaLanza410SinEscribir() {
        Invitacion vencida = invitacion(EstadoInvitacion.ENVIADA, Instant.now().minus(1, ChronoUnit.HOURS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(vencida));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.resolver(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void resolverTokenAceptadoLanza409() {
        Invitacion aceptada = invitacion(EstadoInvitacion.ACEPTADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(aceptada));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.resolver(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void validarParaAceptarConEnviadaVencidaLanza410() {
        Invitacion vencida = invitacion(EstadoInvitacion.ENVIADA, Instant.now().minus(1, ChronoUnit.HOURS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(vencida));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.validarParaAceptar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void marcarAceptadaGuardaElEstadoYLaFechaDeAceptacion() {
        Invitacion enviada = invitacion(EstadoInvitacion.ENVIADA, Instant.now().plus(1, ChronoUnit.DAYS));

        service().marcarAceptada(enviada);

        ArgumentCaptor<Invitacion> captor = ArgumentCaptor.forClass(Invitacion.class);
        verify(invitacionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoInvitacion.ACEPTADA);
        assertThat(captor.getValue().getFechaAceptacion()).isNotNull();
    }

    @Test
    void marcarAceptadaConInvitacionNoEnviadaLanza409() {
        Invitacion revocada = invitacion(EstadoInvitacion.REVOCADA, Instant.now().plus(1, ChronoUnit.DAYS));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.marcarAceptada(revocada))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(invitacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void marcarAceptadaConLockOptimistaLanza409() {
        Invitacion enviada = invitacion(EstadoInvitacion.ENVIADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitacionRepository.saveAndFlush(any(Invitacion.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Invitacion.class, enviada.getId()));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.marcarAceptada(enviada))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resolverTokenRevocadoLanza409YNoLoConfundeConInexistente() {
        Invitacion revocada = invitacion(EstadoInvitacion.REVOCADA, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(revocada));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.resolver(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resolverTokenYaPersistidoComoExpiradaSigueDando410EnAccesosPosteriores() {
        Invitacion expirada = invitacion(EstadoInvitacion.EXPIRADA, Instant.now().minus(1, ChronoUnit.DAYS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(expirada));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.resolver(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void validarParaAceptarConEstadoPersistidoExpiradaSigueDando410() {
        Invitacion expirada = invitacion(EstadoInvitacion.EXPIRADA, Instant.now().minus(1, ChronoUnit.DAYS));
        when(invitacionRepository.findByTokenHash(any())).thenReturn(Optional.of(expirada));

        InvitacionService servicio = service();
        assertThatThrownBy(() -> servicio.validarParaAceptar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);
        verify(invitacionRepository, never()).save(any());
    }
}
