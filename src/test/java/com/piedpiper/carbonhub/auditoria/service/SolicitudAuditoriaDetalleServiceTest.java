package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudAuditoriaDetalleServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID OTRA_EMPRESA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ADMIN_EMPRESA_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID AUDITOR_ANTERIOR_ID = UUID.fromString("deadbeef-1111-2222-3333-444455556666");
    private static final UUID AJENO_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID PLATAFORMA_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private SolicitudAuditoriaDetalleService service;

    @BeforeEach
    void configurar() {
        service = new SolicitudAuditoriaDetalleService(
                solicitudAuditoriaRepository,
                transicionEstadoAuditoriaRepository,
                usuarioRepository,
                new SolicitudAuditoriaMapperImpl(),
                new TransicionEstadoAuditoriaMapperImpl(),
                new AccesoSolicitudAuditoria(transicionEstadoAuditoriaRepository));

        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitud(EMPRESA_ID, auditorActual())));
        when(transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(SOLICITUD_ID))
                .thenReturn(List.of(transicionDeCreacion()));
        when(transicionEstadoAuditoriaRepository.idsAuditoresConHistorial(SOLICITUD_ID))
                .thenReturn(List.of());

        when(usuarioRepository.findById(ADMIN_EMPRESA_ID)).thenReturn(Optional.of(
                usuario(ADMIN_EMPRESA_ID, Rol.ADMINISTRADOR_EMPRESA, EMPRESA_ID)));
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(
                usuario(AUDITOR_ID, Rol.AUDITOR_CERTIFICADO, null)));
        when(usuarioRepository.findById(AUDITOR_ANTERIOR_ID)).thenReturn(Optional.of(
                usuario(AUDITOR_ANTERIOR_ID, Rol.AUDITOR_CERTIFICADO, null)));
        when(usuarioRepository.findById(AJENO_ID)).thenReturn(Optional.of(
                usuario(AJENO_ID, Rol.ADMINISTRADOR_EMPRESA, OTRA_EMPRESA_ID)));
        when(usuarioRepository.findById(PLATAFORMA_ID)).thenReturn(Optional.of(
                usuario(PLATAFORMA_ID, Rol.ADMINISTRADOR_PLATAFORMA, null)));
    }

    @Test
    void laEmpresaDuenaVeElDetalleConSuLineaDeTiempo() {
        SolicitudAuditoriaDetalleResponseDTO detalle = service.obtenerDetalle(SOLICITUD_ID, ADMIN_EMPRESA_ID);

        assertThat(detalle.getId()).isEqualTo(SOLICITUD_ID);
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
        assertThat(detalle.getEstadoDescripcion()).isEqualTo("Solicitud enviada");
        assertThat(detalle.getNombreEmpresa()).isEqualTo("Acme S.A.");
        assertThat(detalle.getHistorial()).hasSize(1);
        assertThat(detalle.getHistorial().get(0).getEvento())
                .isEqualTo(EventoTransicionAuditoria.SOLICITUD_CREADA);
        assertThat(detalle.getHistorial().get(0).getResponsable()).isEqualTo("Marta Gerente");
    }

    @Test
    void elAuditorAsignadoVeElDetalle() {
        assertThat(service.obtenerDetalle(SOLICITUD_ID, AUDITOR_ID).getId()).isEqualTo(SOLICITUD_ID);
    }

    @Test
    void elAuditorQueYaNoEstaAsignadoSigueViendoElDetalleGraciasAlHistorial() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitud(EMPRESA_ID, null)));
        when(transicionEstadoAuditoriaRepository.idsAuditoresConHistorial(SOLICITUD_ID))
                .thenReturn(List.of(AUDITOR_ANTERIOR_ID));

        assertThat(service.obtenerDetalle(SOLICITUD_ID, AUDITOR_ANTERIOR_ID).getId()).isEqualTo(SOLICITUD_ID);
    }

    @Test
    void elAdministradorDePlataformaVeCualquierSolicitud() {
        assertThat(service.obtenerDetalle(SOLICITUD_ID, PLATAFORMA_ID).getId()).isEqualTo(SOLICITUD_ID);
    }

    @Test
    void unUsuarioSinRelacionConLaSolicitudRecibe403() {
        assertThatThrownBy(() -> service.obtenerDetalle(SOLICITUD_ID, AJENO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unAuditorSinRelacionConLaSolicitudRecibe403() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitud(EMPRESA_ID, null)));

        assertThatThrownBy(() -> service.obtenerDetalle(SOLICITUD_ID, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unaSolicitudInexistenteDevuelve404() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetalle(SOLICITUD_ID, ADMIN_EMPRESA_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void lasTransicionesDelSistemaSeMuestranComoProcesoAutomatico() {
        when(transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(SOLICITUD_ID))
                .thenReturn(List.of(TransicionEstadoAuditoria.builder()
                        .estadoAnterior(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                        .estadoNuevo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                        .evento(EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA)
                        .actor(ActorTransicionAuditoria.SISTEMA)
                        .fecha(Instant.parse("2026-08-01T10:00:00Z"))
                        .build()));

        assertThat(service.obtenerDetalle(SOLICITUD_ID, ADMIN_EMPRESA_ID)
                .getHistorial().get(0).getResponsable())
                .isEqualTo("Proceso automático");
    }

    private static SolicitudAuditoria solicitud(UUID empresaId, Usuario auditor) {
        return SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder().id(empresaId).nombreEmpresa("Acme S.A.").build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2025, 1, 1))
                .periodoFin(LocalDate.of(2025, 12, 31))
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .fechaCreacion(Instant.parse("2026-07-27T18:00:00Z"))
                .auditor(auditor)
                .documentos(new ArrayList<>())
                .build();
    }

    private static TransicionEstadoAuditoria transicionDeCreacion() {
        return TransicionEstadoAuditoria.builder()
                .estadoAnterior(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .estadoNuevo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .evento(EventoTransicionAuditoria.SOLICITUD_CREADA)
                .actor(ActorTransicionAuditoria.EMPRESA)
                .responsableId(ADMIN_EMPRESA_ID)
                .responsableNombre("Marta Gerente")
                .fecha(Instant.parse("2026-07-27T18:00:00Z"))
                .build();
    }

    private static Usuario auditorActual() {
        return Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build();
    }

    private static Usuario usuario(UUID id, Rol rol, UUID empresaId) {
        return Usuario.builder()
                .id(id)
                .rol(rol)
                .empresa(empresaId == null ? null : Empresa.builder().id(empresaId).build())
                .build();
    }
}
