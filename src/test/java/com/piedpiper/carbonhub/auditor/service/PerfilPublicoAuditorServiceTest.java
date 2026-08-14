package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.DistribucionSectorAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicoAuditorServiceTest {

    private static final UUID AUDITOR_ID = UUID.randomUUID();
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-01-15T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private CatalogoTiposCertificacion catalogoTiposCertificacion;
    @Mock
    private PerfilPublicoAuditorMapper mapper;

    private PerfilPublicoAuditorService service;

    @BeforeEach
    void setUp() {
        service = new PerfilPublicoAuditorService(
                perfilAuditorRepository,
                certificacionRepository,
                calificacionRepository,
                catalogoTiposCertificacion,
                mapper,
                FIXED_CLOCK);
    }

    @Test
    void auditorActivoRetornaMetricasPersistidasYDistribucion() {
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = perfilConMetricas(auditor);
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Collections.emptyList());

        PerfilPublicoAuditorResponseDTO expectedDto = new PerfilPublicoAuditorResponseDTO();
        expectedDto.setAuditorId(AUDITOR_ID);
        expectedDto.setAuditoriasCompletadas(5);
        expectedDto.setTiempoPromedioRespuestaDias(new BigDecimal("1.8"));
        expectedDto.setDistribucionSectores(List.of(
                new DistribucionSectorDTO("AGROINDUSTRIA", 3, new BigDecimal("60.0"))));
        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(expectedDto);

        PerfilPublicoAuditorResponseDTO result = service.obtenerPerfilPublico(AUDITOR_ID);

        ArgumentCaptor<MetricasAuditor> metricasCaptor = ArgumentCaptor.forClass(MetricasAuditor.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DistribucionSectorDTO>> distribucionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mapper).aPerfilPublicoDto(
                eq(perfil),
                metricasCaptor.capture(),
                any(),
                distribucionCaptor.capture(),
                any());
        assertThat(metricasCaptor.getValue().auditoriasCompletadas()).isEqualTo(5);
        assertThat(metricasCaptor.getValue().tiempoPromedioRespuestaDias())
                .isEqualByComparingTo("1.8");
        assertThat(distribucionCaptor.getValue()).singleElement()
                .satisfies(distribucion -> {
                    assertThat(distribucion.getSector()).isEqualTo("AGROINDUSTRIA");
                    assertThat(distribucion.getCantidad()).isEqualTo(3);
                    assertThat(distribucion.getPorcentaje()).isEqualByComparingTo("60.0");
                });
        assertThat(result.getAuditoriasCompletadas()).isEqualTo(5);
    }

    @Test
    void auditorInexistenteLanzaApiException404() {
        UUID idInexistente = UUID.randomUUID();
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                idInexistente, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfilPublico(idInexistente))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("El perfil solicitado no está disponible.");
                });
    }

    @Test
    void perfilSinMetricasPersistidasExponeSinDatos() {
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = perfilConMetricas(auditor);
        perfil.setAuditoriasCompletadas(null);
        perfil.setTiempoPromedioRespuestaDias(null);
        perfil.getDistribucionSectores().clear();
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Collections.emptyList());
        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(new PerfilPublicoAuditorResponseDTO());

        service.obtenerPerfilPublico(AUDITOR_ID);

        ArgumentCaptor<MetricasAuditor> metricasCaptor = ArgumentCaptor.forClass(MetricasAuditor.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DistribucionSectorDTO>> distribucionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mapper).aPerfilPublicoDto(
                eq(perfil),
                metricasCaptor.capture(),
                any(),
                distribucionCaptor.capture(),
                any());
        assertThat(metricasCaptor.getValue().auditoriasCompletadas()).isNull();
        assertThat(metricasCaptor.getValue().tiempoPromedioRespuestaDias()).isNull();
        assertThat(distribucionCaptor.getValue()).isEmpty();
    }

    private Usuario auditorActivo() {
        return Usuario.builder()
                .id(AUDITOR_ID)
                .nombre("Carlos")
                .apellidos("Ramírez")
                .email("carlos@example.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    private PerfilAuditor perfilConMetricas(Usuario auditor) {
        return PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .fotoPerfil("https://cdn.example.com/foto.jpg")
                .disponible(true)
                .auditoriasCompletadas(5)
                .tiempoPromedioRespuestaDias(new BigDecimal("1.8"))
                .calificacionPromedio(new BigDecimal("4.5"))
                .totalResenas(10)
                .especialidades(Set.of(EspecialidadAuditor.ENERGIA_RENOVABLE))
                .descripcionProfesional("Auditor con 5 años de experiencia")
                .distribucionSectores(new ArrayList<>(List.of(
                        new DistribucionSectorAuditor("AGROINDUSTRIA", 3, new BigDecimal("60.0")))))
                .build();
    }

    @Test
    void auditorActivoIncluyeResenasVerificadasYTotalResenasReal() {
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = perfilConMetricas(auditor);
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .nombreEmpresa("Café del Valle S.A.")
                .build();
        Calificacion calificacion = Calificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .calificacion(5)
                .comentario("Excelente proceso de auditoría.")
                .nombreCalificador("María Pérez")
                .creadoEn(Instant.parse("2025-01-10T10:00:00Z"))
                .build();
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Collections.emptyList());
        when(calificacionRepository.findByAuditorIdOrderByCreadoEnDesc(AUDITOR_ID))
                .thenReturn(List.of(calificacion));
        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(new PerfilPublicoAuditorResponseDTO());

        service.obtenerPerfilPublico(AUDITOR_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ResenaVerificadaDTO>> resenasCaptor = ArgumentCaptor.forClass(List.class);
        verify(mapper).aPerfilPublicoDto(eq(perfil), any(), any(), any(), resenasCaptor.capture());
        assertThat(resenasCaptor.getValue()).singleElement().satisfies(resena -> {
            assertThat(resena.getNombreCalificador()).isEqualTo("María Pérez");
            assertThat(resena.getNombreEmpresa()).isEqualTo("Café del Valle S.A.");
            assertThat(resena.getCalificacion()).isEqualByComparingTo("5");
        });
    }
}
