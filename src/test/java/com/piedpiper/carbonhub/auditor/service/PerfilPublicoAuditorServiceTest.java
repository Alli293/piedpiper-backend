package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
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

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;

    @Mock
    private CertificacionRepository certificacionRepository;

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;

    @Mock
    private CatalogoTiposCertificacion catalogoTiposCertificacion;

    @Mock
    private PerfilPublicoAuditorMapper mapper;

    private PerfilPublicoAuditorService service;

    private static final UUID AUDITOR_ID = UUID.randomUUID();
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-01-15T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service = new PerfilPublicoAuditorService(
                perfilAuditorRepository,
                certificacionRepository,
                solicitudAuditoriaRepository,
                catalogoTiposCertificacion,
                mapper,
                FIXED_CLOCK);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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

    private PerfilAuditor perfilCompleto(Usuario auditor) {
        return PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .fotoPerfil("https://cdn.example.com/foto.jpg")
                .disponible(true)
                .auditoriasCompletadas(5)
                .calificacionPromedio(new BigDecimal("4.5"))
                .totalResenas(10)
                .tiempoRespuestaHoras(48)
                .especialidades(Set.of(EspecialidadAuditor.ENERGIA_RENOVABLE))
                .descripcionProfesional("Auditor con 5 años de experiencia")
                .build();
    }

    private SolicitudAuditoria solicitudCompletada() {
        return SolicitudAuditoria.builder()
                .id(UUID.randomUUID())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .estado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA)
                .fechaAsignacion(Instant.parse("2024-06-01T10:00:00Z"))
                .fechaAceptacion(Instant.parse("2024-06-02T10:00:00Z"))
                .build();
    }

    // ── Test 1: Auditor activo retorna perfil completo con todos los campos ──

    @Test
    void auditorActivoRetornaPerfilCompletoConTodosLosCampos() {
        // Arrange
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = perfilCompleto(auditor);
        List<Certificacion> certificaciones = Collections.emptyList();
        List<SolicitudAuditoria> auditoriasCompletadas = List.of(solicitudCompletada());

        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(certificaciones);
        when(solicitudAuditoriaRepository.findByAuditorIdAndEstado(AUDITOR_ID, EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA))
                .thenReturn(auditoriasCompletadas);

        PerfilPublicoAuditorResponseDTO expectedDto = new PerfilPublicoAuditorResponseDTO();
        expectedDto.setAuditorId(AUDITOR_ID);
        expectedDto.setNombre("Carlos Ramírez");
        expectedDto.setFotoPerfil("https://cdn.example.com/foto.jpg");
        expectedDto.setDisponible(true);
        expectedDto.setCalificacionPromedio(new BigDecimal("4.5"));
        expectedDto.setTotalResenas(10);
        expectedDto.setAuditoriasCompletadas(1);

        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(expectedDto);

        // Act
        PerfilPublicoAuditorResponseDTO result = service.obtenerPerfilPublico(AUDITOR_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAuditorId()).isEqualTo(AUDITOR_ID);
        assertThat(result.getNombre()).isEqualTo("Carlos Ramírez");
        assertThat(result.getFotoPerfil()).isEqualTo("https://cdn.example.com/foto.jpg");
        assertThat(result.isDisponible()).isTrue();
        assertThat(result.getCalificacionPromedio()).isEqualTo(new BigDecimal("4.5"));
        assertThat(result.getTotalResenas()).isEqualTo(10);
        assertThat(result.getAuditoriasCompletadas()).isEqualTo(1);

        // Verify mapper was called with correct args
        verify(mapper).aPerfilPublicoDto(
                eq(perfil), any(MetricasAuditor.class), any(), any(), any());
    }

    // ── Test 2: Auditor inexistente lanza ApiException con 404 ──

    @Test
    void auditorInexistenteLanzaApiException404() {
        // Arrange
        UUID idInexistente = UUID.randomUUID();
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(idInexistente, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerPerfilPublico(idInexistente))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("El perfil solicitado no está disponible.");
                });
    }

    // ── Test 3: Auditor con estado no activo lanza ApiException 404 ──

    @Test
    void auditorPendienteValidacionLanzaApiException404() {
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfilPublico(AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("El perfil solicitado no está disponible.");
                });
    }

    @Test
    void auditorRechazadoLanzaApiException404() {
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfilPublico(AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("El perfil solicitado no está disponible.");
                });
    }

    @Test
    void auditorDeshabilitadoLanzaApiException404() {
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfilPublico(AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getMessage()).isEqualTo("El perfil solicitado no está disponible.");
                });
    }

    // ── Test 4: Auditor sin auditorías completadas pero con calificación previa retorna métricas parciales ──

    @Test
    void auditorSinAuditoriasCompletadasPeroConCalificacionRetornaMetricasParciales() {
        // Arrange
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = perfilCompleto(auditor); // has calificacionPromedio=4.5, totalResenas=10

        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Collections.emptyList());
        when(solicitudAuditoriaRepository.findByAuditorIdAndEstado(AUDITOR_ID, EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA))
                .thenReturn(Collections.emptyList());

        PerfilPublicoAuditorResponseDTO dtoConMetricasParciales = new PerfilPublicoAuditorResponseDTO();
        dtoConMetricasParciales.setAuditorId(AUDITOR_ID);
        dtoConMetricasParciales.setCalificacionPromedio(new BigDecimal("4.5"));
        dtoConMetricasParciales.setTotalResenas(10);
        dtoConMetricasParciales.setAuditoriasCompletadas(0);
        dtoConMetricasParciales.setDistribucionSectores(Collections.emptyList());

        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(dtoConMetricasParciales);

        // Act
        PerfilPublicoAuditorResponseDTO result = service.obtenerPerfilPublico(AUDITOR_ID);

        // Assert — verify mapper is called with non-null metricas (partial) and empty distribucion
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

        MetricasAuditor metricas = metricasCaptor.getValue();
        assertThat(metricas).isNotNull();
        assertThat(metricas.calificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(metricas.totalResenas()).isEqualTo(10);
        assertThat(metricas.auditoriasCompletadas()).isEqualTo(0);
        assertThat(metricas.tiempoPromedioRespuestaDias()).isNull();
        assertThat(distribucionCaptor.getValue()).isEmpty();
    }

    // ── Test 4b: Auditor sin auditorías y sin calificación retorna métricas null ──

    @Test
    void auditorSinAuditoriasYSinCalificacionRetornaMetricasNull() {
        // Arrange
        Usuario auditor = auditorActivo();
        PerfilAuditor perfil = PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .fotoPerfil("https://cdn.example.com/foto.jpg")
                .disponible(true)
                .auditoriasCompletadas(0)
                .calificacionPromedio(null)
                .totalResenas(0)
                .especialidades(Set.of(EspecialidadAuditor.ENERGIA_RENOVABLE))
                .descripcionProfesional("Auditor nuevo")
                .build();

        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(AUDITOR_ID, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.of(perfil));
        when(certificacionRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Collections.emptyList());
        when(solicitudAuditoriaRepository.findByAuditorIdAndEstado(AUDITOR_ID, EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA))
                .thenReturn(Collections.emptyList());

        PerfilPublicoAuditorResponseDTO dtoSinMetricas = new PerfilPublicoAuditorResponseDTO();
        dtoSinMetricas.setAuditorId(AUDITOR_ID);
        dtoSinMetricas.setDistribucionSectores(Collections.emptyList());

        when(mapper.aPerfilPublicoDto(any(), any(), any(), any(), any()))
                .thenReturn(dtoSinMetricas);

        // Act
        PerfilPublicoAuditorResponseDTO result = service.obtenerPerfilPublico(AUDITOR_ID);

        // Assert — verify mapper is called with null metricas and empty distribucion
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

        assertThat(metricasCaptor.getValue()).isNull();
        assertThat(distribucionCaptor.getValue()).isEmpty();

        // Assert on the result DTO
        assertThat(result.getCalificacionPromedio()).isNull();
        assertThat(result.getTotalResenas()).isNull();
        assertThat(result.getAuditoriasCompletadas()).isNull();
        assertThat(result.getTiempoPromedioRespuestaDias()).isNull();
        assertThat(result.getDistribucionSectores()).isEmpty();
    }

    // ── Test 5: Mensaje de error 404 es idéntico para inexistente y no activo ──

    @Test
    void mensajeError404EsIdenticoParaInexistenteYNoActivo() {
        UUID idInexistente = UUID.randomUUID();

        // Caso 1: auditor inexistente
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(idInexistente, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        String mensajeInexistente = null;
        try {
            service.obtenerPerfilPublico(idInexistente);
        } catch (ApiException ex) {
            mensajeInexistente = ex.getMessage();
        }

        // Caso 2: auditor no activo (repo retorna vacío porque filtra por ACTIVO)
        UUID idNoActivo = UUID.randomUUID();
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstado(idNoActivo, EstadoUsuario.ACTIVO))
                .thenReturn(Optional.empty());

        String mensajeNoActivo = null;
        try {
            service.obtenerPerfilPublico(idNoActivo);
        } catch (ApiException ex) {
            mensajeNoActivo = ex.getMessage();
        }

        // Assert: ambos mensajes son exactamente iguales
        assertThat(mensajeInexistente).isNotNull();
        assertThat(mensajeNoActivo).isNotNull();
        assertThat(mensajeInexistente).isEqualTo(mensajeNoActivo);
        assertThat(mensajeInexistente).isEqualTo("El perfil solicitado no está disponible.");
    }
}
