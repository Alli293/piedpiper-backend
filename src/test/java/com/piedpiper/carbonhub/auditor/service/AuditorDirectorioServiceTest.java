package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapperImpl;
import com.piedpiper.carbonhub.auditor.models.dtos.FiltrarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditorDirectorioServiceTest {

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor
    private ArgumentCaptor<Set<EspecialidadAuditor>> especialidadesCaptor;

    private AuditorDirectorioService servicio() {
        return new AuditorDirectorioService(perfilAuditorRepository, new PerfilAuditorMapperImpl());
    }

    private FiltrarAuditoresRequestDTO filtros() {
        FiltrarAuditoresRequestDTO f = new FiltrarAuditoresRequestDTO();
        f.setPagina(0);
        f.setTamanioPagina(12);
        f.setOrdenamiento("CALIFICACION");
        return f;
    }

    private void prepararRepositorio() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void listaSoloAuditoresActivosYMapeaElResumen() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(perfil())));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setTerminoBusqueda("Ana");

        PaginaAuditoresResponseDTO resultado = servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO), eq("Ana"),
                isNull(), isNull(), eq(false), eq(false), any(), any(Pageable.class));
        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getNombre()).isEqualTo("Ana Mora");
        assertThat(resultado.getContenido().get(0).getEspecialidadesPrincipales()).hasSize(3);
        assertThat(resultado.getTotalResultados()).isEqualTo(1);
    }

    @Test
    void terminoDeBusquedaConUnCaracterSeIgnora() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setTerminoBusqueda("a");

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO), isNull(),
                any(), any(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    void terminoDeBusquedaConMasDeCienCaracteresSeIgnora() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setTerminoBusqueda("a".repeat(101));

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), isNull(), any(), any(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    void losComodinesDeLikeSeEliminanDelTermino() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setTerminoBusqueda("%An_a%");

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), eq("Ana"), any(), any(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    void unTerminoDeSoloComodinesQuedaIgnorado() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setTerminoBusqueda("%%%");

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), isNull(), any(), any(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    void tamanioDePaginaFueraDeRangoUsaElValorPorDefecto() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setPagina(2);
        f.setTamanioPagina(500);

        servicio().listar(f);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(12);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
    }

    @Test
    void ordenamientoPorAuditoriasCompletadasSeTraduceASortDescendente() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setOrdenamiento("AUDITORIAS_COMPLETADAS");

        servicio().listar(f);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("auditoriasCompletadas");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void ordenamientoPorTiempoDeRespuestaEsAscendenteConNulosAlFinal() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setOrdenamiento("TIEMPO_RESPUESTA");

        servicio().listar(f);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("tiempoRespuestaHoras");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orden.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void ordenamientoPorDefectoEsCalificacionDescendente() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setOrdenamiento(null);

        servicio().listar(f);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("calificacionPromedio");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void ordenamientoInvalidoLanza400() {
        FiltrarAuditoresRequestDTO f = filtros();
        f.setOrdenamiento("POR_PRECIO");

        assertThatThrownBy(() -> servicio().listar(f)).isInstanceOf(ApiException.class);
    }

    @Test
    void filtroDeZonaSoloSePasaAlRepositorio() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setZonaGeografica("SAN_JOSE");

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), any(), eq(ProvinciaCR.SAN_JOSE), isNull(),
                eq(false), eq(false), any(), any());
    }

    @Test
    void filtroDeCalificacionSoloSePasaAlRepositorio() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setCalificacionMinima(new BigDecimal("4.0"));

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), any(), isNull(), eq(new BigDecimal("4.0")),
                eq(false), eq(false), any(), any());
    }

    @Test
    void filtroDeSoloDisponiblesSePasaAlRepositorio() {
        prepararRepositorio();
        FiltrarAuditoresRequestDTO f = filtros();
        f.setSoloDisponibles(true);

        servicio().listar(f);

        verify(perfilAuditorRepository).buscarDirectorio(
                any(), any(), any(), isNull(), isNull(), eq(true), eq(false), any(), any());
    }

    @Test
    void filtrosCombinadosSePasanAlRepositorioConOperadorAnd() {
        when(perfilAuditorRepository.buscarDirectorio(
                any(), any(), any(), eq(ProvinciaCR.SAN_JOSE), eq(new BigDecimal("4.0")),
                eq(true), eq(true), especialidadesCaptor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        FiltrarAuditoresRequestDTO f = filtros();
        f.setZonaGeografica("SAN_JOSE");
        f.setCalificacionMinima(new BigDecimal("4.0"));
        f.setSoloDisponibles(true);
        f.setEspecialidades(List.of("AGROINDUSTRIA", "MANUFACTURA"));

        servicio().listar(f);

        assertThat(especialidadesCaptor.getValue())
                .containsExactlyInAnyOrder(EspecialidadAuditor.AGROINDUSTRIA, EspecialidadAuditor.MANUFACTURA);
    }

    @Test
    void calificacionMinimaFueraDeRangoLanza400() {
        FiltrarAuditoresRequestDTO f = filtros();
        f.setCalificacionMinima(new BigDecimal("6.0"));

        assertThatThrownBy(() -> servicio().listar(f)).isInstanceOf(ApiException.class);
    }

    @Test
    void zonaInvalidaLanza400() {
        FiltrarAuditoresRequestDTO f = filtros();
        f.setZonaGeografica("MARTE");

        assertThatThrownBy(() -> servicio().listar(f)).isInstanceOf(ApiException.class);
    }

    @Test
    void especialidadInvalidaLanza400() {
        FiltrarAuditoresRequestDTO f = filtros();
        f.setEspecialidades(List.of("NUCLEAR"));

        assertThatThrownBy(() -> servicio().listar(f)).isInstanceOf(ApiException.class);
    }

    private PerfilAuditor perfil() {
        Usuario auditor = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Ana")
                .apellidos("Mora")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .build();
        return PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .disponible(true)
                .auditoriasCompletadas(42)
                .calificacionPromedio(new BigDecimal("4.5"))
                .totalResenas(30)
                .especialidades(Set.of(
                        EspecialidadAuditor.AGROINDUSTRIA,
                        EspecialidadAuditor.ENERGIA_RENOVABLE,
                        EspecialidadAuditor.LOGISTICA_TRANSPORTE,
                        EspecialidadAuditor.MANUFACTURA,
                        EspecialidadAuditor.TURISMO_SOSTENIBLE))
                .build();
    }
}
