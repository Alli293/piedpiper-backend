package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapperImpl;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
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

    private AuditorDirectorioService servicio() {
        return new AuditorDirectorioService(perfilAuditorRepository, new PerfilAuditorMapperImpl());
    }

    @Test
    void listaSoloAuditoresActivosYMapeaElResumen() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(perfil())));

        PaginaAuditoresResponseDTO resultado = servicio().listar("Ana", 0, 12, "CALIFICACION");

        verify(perfilAuditorRepository).buscarDirectorio(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO), eq("Ana"), any(Pageable.class));
        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getNombre()).isEqualTo("Ana Mora");
        assertThat(resultado.getContenido().get(0).getEspecialidadesPrincipales()).hasSize(3);
        assertThat(resultado.getTotalResultados()).isEqualTo(1);
    }

    @Test
    void terminoDeBusquedaConUnCaracterSeIgnora() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar("a", 0, 12, null);

        verify(perfilAuditorRepository).buscarDirectorio(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO), isNull(), any(Pageable.class));
    }

    @Test
    void terminoDeBusquedaConMasDeCienCaracteresSeIgnora() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar("a".repeat(101), 0, 12, null);

        verify(perfilAuditorRepository).buscarDirectorio(any(), any(), isNull(), any());
    }

    @Test
    void losComodinesDeLikeSeEliminanDelTermino() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar("%An_a%", 0, 12, null);

        verify(perfilAuditorRepository).buscarDirectorio(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO), eq("Ana"), any(Pageable.class));
    }

    @Test
    void unTerminoDeSoloComodinesQuedaIgnorado() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar("%%%", 0, 12, null);

        verify(perfilAuditorRepository).buscarDirectorio(any(), any(), isNull(), any());
    }

    @Test
    void tamanioDePaginaFueraDeRangoUsaElValorPorDefecto() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar(null, 2, 500, null);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(12);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
    }

    @Test
    void ordenamientoPorAuditoriasCompletadasSeTraduceASortDescendente() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar(null, 0, 12, "AUDITORIAS_COMPLETADAS");

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("auditoriasCompletadas");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void ordenamientoPorTiempoDeRespuestaEsAscendenteConNulosAlFinal() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar(null, 0, 12, "TIEMPO_RESPUESTA");

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("tiempoRespuestaHoras");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orden.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void ordenamientoPorDefectoEsCalificacionDescendente() {
        when(perfilAuditorRepository.buscarDirectorio(any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio().listar(null, 0, 12, null);

        Sort.Order orden = pageableCaptor.getValue().getSort().getOrderFor("calificacionPromedio");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void ordenamientoInvalidoLanza400() {
        assertThatThrownBy(() -> servicio().listar(null, 0, 12, "POR_PRECIO"))
                .isInstanceOf(ApiException.class);
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
