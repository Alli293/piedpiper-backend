package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionIaTexto;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionRenovacionResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba únicamente la orquestación (delegar a la IA, armar el DTO final,
 * cachear por día) — la consulta a base de datos se mockea a través de
 * {@link RecomendacionRenovacionConsultaService}, ya probada por su cuenta
 * en {@code RecomendacionRenovacionConsultaServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class DashboardRecomendacionServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private static final CertAlertaDTO PRIORITARIA = new CertAlertaDTO(
            UUID.randomUUID(), "GHG Protocol — Corporate Standard",
            LocalDate.now().plusDays(5), 5, new BigDecimal("120.5000"));

    @Mock
    private RecomendacionRenovacionConsultaService consultaService;
    @Mock
    private RecomendacionRenovacionIaService iaService;

    private DashboardRecomendacionService service;

    @BeforeEach
    void setUp() {
        service = new DashboardRecomendacionService(consultaService, iaService);
    }

    @Test
    void sinCertificacionPrioritariaNoHayRecomendacion() {
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.empty());

        assertThat(service.obtenerRecomendacion(USUARIO_ID)).isEmpty();
        verify(iaService, never()).generar(any());
    }

    @Test
    void delegaLaJustificacionALaIaYArmaElDtoFinal() {
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.of(PRIORITARIA));
        when(iaService.generar(PRIORITARIA)).thenReturn(Optional.of(new RecomendacionIaTexto(
                "Vence en 5 días y tiene un impacto relevante.", "Renovarla esta semana.")));

        Optional<RecomendacionRenovacionResponseDTO> recomendacion = service.obtenerRecomendacion(USUARIO_ID);

        assertThat(recomendacion).isPresent();
        assertThat(recomendacion.get().getNombreCertificacion()).isEqualTo(PRIORITARIA.getNombreCertificacion());
        assertThat(recomendacion.get().getDiasRestantes()).isEqualTo(PRIORITARIA.getDiasRestantes());
        assertThat(recomendacion.get().getImpactoHuellaT()).isEqualByComparingTo(PRIORITARIA.getImpactoHuellaT());
        assertThat(recomendacion.get().getJustificacion())
                .isEqualTo("Vence en 5 días y tiene un impacto relevante.");
        assertThat(recomendacion.get().getSugerenciaAccion()).isEqualTo("Renovarla esta semana.");
    }

    @Test
    void siLaIaNoEstaDisponibleLaRecomendacionQuedaConJustificacionNula() {
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.of(PRIORITARIA));
        when(iaService.generar(PRIORITARIA)).thenReturn(Optional.empty());

        Optional<RecomendacionRenovacionResponseDTO> recomendacion = service.obtenerRecomendacion(USUARIO_ID);

        assertThat(recomendacion).isPresent();
        assertThat(recomendacion.get().getJustificacion()).isNull();
        assertThat(recomendacion.get().getSugerenciaAccion()).isNull();
    }

    @Test
    void unaSegundaLlamadaElMismoDiaUsaElCacheSinConsultarDeNuevo() {
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.of(PRIORITARIA));
        when(iaService.generar(PRIORITARIA)).thenReturn(Optional.empty());

        Optional<RecomendacionRenovacionResponseDTO> primera = service.obtenerRecomendacion(USUARIO_ID);
        Optional<RecomendacionRenovacionResponseDTO> segunda = service.obtenerRecomendacion(USUARIO_ID);

        assertThat(segunda).isEqualTo(primera);
        verify(consultaService, times(1)).obtenerCertificacionPrioritaria(USUARIO_ID);
        verify(iaService, times(1)).generar(PRIORITARIA);
    }

    @Test
    void elCacheTambienAplicaCuandoNoHayRecomendacion() {
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.empty());

        service.obtenerRecomendacion(USUARIO_ID);
        service.obtenerRecomendacion(USUARIO_ID);

        verify(consultaService, times(1)).obtenerCertificacionPrioritaria(USUARIO_ID);
    }

    @Test
    void usuariosDistintosNoCompartenCache() {
        UUID otroUsuario = UUID.randomUUID();
        when(consultaService.obtenerCertificacionPrioritaria(USUARIO_ID)).thenReturn(Optional.of(PRIORITARIA));
        when(consultaService.obtenerCertificacionPrioritaria(otroUsuario)).thenReturn(Optional.empty());
        when(iaService.generar(PRIORITARIA)).thenReturn(Optional.empty());

        service.obtenerRecomendacion(USUARIO_ID);
        service.obtenerRecomendacion(otroUsuario);

        verify(consultaService).obtenerCertificacionPrioritaria(USUARIO_ID);
        verify(consultaService).obtenerCertificacionPrioritaria(otroUsuario);
    }
}
