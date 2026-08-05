package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudAuditoriaListadoServiceTest {

    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID ADMIN_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private SolicitudAuditoriaListadoService service;

    @BeforeEach
    void configurar() {
        service = new SolicitudAuditoriaListadoService(
                solicitudAuditoriaRepository, usuarioRepository, new SolicitudAuditoriaMapperImpl());

        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(ADMIN_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .build()));
        when(solicitudAuditoriaRepository.listarPorEmpresa(EMPRESA_ID))
                .thenReturn(List.of(conAuditor(), sinAuditor()));
        when(solicitudAuditoriaRepository.listarAsignadasA(AUDITOR_ID))
                .thenReturn(List.of(conAuditor()));
    }

    @Test
    void laEmpresaVeSusSolicitudesConElAuditorYElConteoDeAdjuntos() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = service.listarDeMiEmpresa(ADMIN_ID);

        assertThat(listado).hasSize(2);
        assertThat(listado.get(0).getNombreAuditor()).isEqualTo("Ana Auditora");
        assertThat(listado.get(0).getNombreEmpresa()).isEqualTo("Acme S.A.");
        assertThat(listado.get(0).getEstadoDescripcion()).isEqualTo("En revisión");
        assertThat(listado.get(0).getCantidadDocumentos()).isEqualTo(1);
    }

    /** Una solicitud sin auditor asignado es normal en el listado y no debe romper el mapeo. */
    @Test
    void unaSolicitudSinAuditorSeListaConElNombreEnNulo() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = service.listarDeMiEmpresa(ADMIN_ID);

        assertThat(listado.get(1).getNombreAuditor()).isNull();
        assertThat(listado.get(1).getIdAuditor()).isNull();
        assertThat(listado.get(1).getCantidadDocumentos()).isZero();
    }

    /**
     * El listado sale del usuario autenticado y no de un parametro: por eso el servicio consulta
     * por la empresa del usuario y nunca por una recibida desde afuera.
     */
    @Test
    void elListadoDeEmpresaConsultaPorLaEmpresaDelUsuarioAutenticado() {
        service.listarDeMiEmpresa(ADMIN_ID);

        verify(solicitudAuditoriaRepository).listarPorEmpresa(EMPRESA_ID);
    }

    @Test
    void unUsuarioSinEmpresaConfiguradaRecibe422() {
        when(usuarioRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(Usuario.builder().id(ADMIN_ID).build()));

        assertThatThrownBy(() -> service.listarDeMiEmpresa(ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void elAuditorVeSoloLasSolicitudesQueTieneAsignadas() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = service.listarAsignadasA(AUDITOR_ID);

        assertThat(listado).hasSize(1);
        assertThat(listado.get(0).getIdAuditor()).isEqualTo(AUDITOR_ID);
        verify(solicitudAuditoriaRepository).listarAsignadasA(AUDITOR_ID);
    }

    @Test
    void elResumenNoExponeElContenidoDeLosDocumentos() {
        assertThat(SolicitudAuditoriaResumenResponseDTO.class.getDeclaredFields())
                .noneMatch(campo -> campo.getType() == byte[].class || campo.getType() == List.class);
    }

    private static SolicitudAuditoria conAuditor() {
        SolicitudAuditoria solicitud = base(EstadoSolicitudAuditoria.EN_REVISION);
        solicitud.setAuditor(Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build());
        solicitud.setFechaAsignacion(Instant.parse("2026-07-01T10:00:00Z"));
        solicitud.agregarDocumento(DocumentoRespaldo.builder().nombreArchivo("uno.pdf").build());
        return solicitud;
    }

    private static SolicitudAuditoria sinAuditor() {
        return base(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    private static SolicitudAuditoria base(EstadoSolicitudAuditoria estado) {
        return SolicitudAuditoria.builder()
                .id(UUID.randomUUID())
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2025, 1, 1))
                .periodoFin(LocalDate.of(2025, 12, 31))
                .estado(estado)
                .fechaCreacion(Instant.parse("2026-06-02T14:32:00Z"))
                .documentos(new ArrayList<>())
                .build();
    }
}
