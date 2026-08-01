package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CalendarioVencimientosResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertificacionVencimientoDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioVencimientosServiceTest {

    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private CertificacionRepository certificacionRepository;

    @InjectMocks
    private CalendarioVencimientosService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.now(ZonasHorarias.COSTA_RICA);

    @Test
    void agrupaCertificacionesPorFechaDeVencimiento() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        YearMonth mes = YearMonth.from(HOY);
        LocalDate diaA = mes.atDay(3);
        LocalDate diaB = mes.atDay(18);
        Certificacion certA1 = certificacion(diaA, TipoCertificacion.CARBONO_NEUTRAL);
        Certificacion certA2 = certificacion(diaA, TipoCertificacion.INVENTARIO_GEI);
        Certificacion certB = certificacion(diaB, TipoCertificacion.HUELLA_PRODUCTO);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(
                EMPRESA_ID, mes.atDay(1), mes.atEndOfMonth()))
                .thenReturn(List.of(certA1, certA2, certB));

        CalendarioVencimientosResponseDTO resultado = service.obtenerCalendario(USUARIO_ID, mes.toString());

        assertThat(resultado.getMesVisualizado()).isEqualTo(mes.toString());
        Map<String, List<CertificacionVencimientoDTO>> porFecha = resultado.getVencimientosPorFecha();
        assertThat(porFecha.get(diaA.toString())).hasSize(2);
        assertThat(porFecha.get(diaB.toString())).hasSize(1);
        assertThat(porFecha).hasSize(2);
    }

    @Test
    void mesSinVencimientosDevuelveMapaVacio() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(any(), any(), any()))
                .thenReturn(List.of());

        CalendarioVencimientosResponseDTO resultado = service.obtenerCalendario(USUARIO_ID, "2026-07");

        assertThat(resultado.getVencimientosPorFecha()).isEmpty();
    }

    @Test
    void mesInvalidoCaeEnElMesActual() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(any(), any(), any()))
                .thenReturn(List.of());

        CalendarioVencimientosResponseDTO resultado = service.obtenerCalendario(USUARIO_ID, "no-es-un-mes");

        assertThat(resultado.getMesVisualizado()).isEqualTo(YearMonth.from(HOY).toString());
    }

    @Test
    void mesOmitidoCaeEnElMesActual() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(any(), any(), any()))
                .thenReturn(List.of());

        CalendarioVencimientosResponseDTO resultado = service.obtenerCalendario(USUARIO_ID, null);

        assertThat(resultado.getMesVisualizado()).isEqualTo(YearMonth.from(HOY).toString());
    }

    @Test
    void certificacionVencidaOMuyProximaQuedaEnLaUrgenciaMasAlta() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        Certificacion vencidaHaceDias = certificacion(HOY.minusDays(5), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(any(), any(), any()))
                .thenReturn(List.of(vencidaHaceDias));

        CalendarioVencimientosResponseDTO resultado =
                service.obtenerCalendario(USUARIO_ID, YearMonth.from(HOY).toString());

        CertificacionVencimientoDTO dto = resultado.getVencimientosPorFecha()
                .get(HOY.minusDays(5).toString())
                .get(0);
        assertThat(dto.getUrgencia()).isEqualTo("7_dias");
        assertThat(dto.getNombre()).isEqualTo("Carbono Neutral");
    }

    @Test
    void certificacionAMasDe90DiasCaeEnElUmbralMasLaxo() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        YearMonth mesLejano = YearMonth.from(HOY.plusMonths(6));
        LocalDate fechaLejana = mesLejano.atDay(10);
        Certificacion lejana = certificacion(fechaLejana, TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(
                EMPRESA_ID, mesLejano.atDay(1), mesLejano.atEndOfMonth()))
                .thenReturn(List.of(lejana));

        CalendarioVencimientosResponseDTO resultado =
                service.obtenerCalendario(USUARIO_ID, mesLejano.toString());

        CertificacionVencimientoDTO dto = resultado.getVencimientosPorFecha().get(fechaLejana.toString()).get(0);
        assertThat(dto.getUrgencia()).isEqualTo("90_dias");
    }

    @Test
    void consultaElRangoDelMesSolicitadoParaLaEmpresaAutenticada() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdAndFechaVencimientoBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.obtenerCalendario(USUARIO_ID, "2026-02");

        ArgumentCaptor<LocalDate> desdeCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> hastaCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(certificacionRepository).findByEmpresaIdAndFechaVencimientoBetween(
                eq(EMPRESA_ID), desdeCaptor.capture(), hastaCaptor.capture());
        assertThat(desdeCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(hastaCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    private static Certificacion certificacion(LocalDate fechaVencimiento, TipoCertificacion tipo) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .tipo(tipo)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(fechaVencimiento)
                .build();
    }
}
