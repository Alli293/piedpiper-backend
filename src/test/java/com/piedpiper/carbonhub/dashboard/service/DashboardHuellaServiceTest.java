package com.piedpiper.carbonhub.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardHuellaServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EMPRESA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private EmisionRepository emisionRepository;

    @Mock
    private EmisionEmpresaService emisionEmpresaService;

    @InjectMocks
    private DashboardHuellaService service;

    @Test
    void resumenMesActualSumaCarbonKgYCalculaVariacion() {
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes = inicioMes.plusMonths(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(List.of(emision("3000"), emision("2236")));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMesAnterior, inicioMes))
                .thenReturn(List.of(emision("4000")));

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "mes_actual", null);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("mes_actual");
        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("5.2360");
        assertThat(response.getVariacionPorcentual()).isEqualByComparingTo("30.9");
        assertThat(response.isTieneDatos()).isTrue();
    }

    @Test
    void periodoSinDatosDevuelveCeroSinVariacion() {
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes = inicioMes.plusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(List.of());

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "mes_actual", null);

        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("0.0000");
        assertThat(response.getVariacionPorcentual()).isNull();
        assertThat(response.isTieneDatos()).isFalse();
    }

    @Test
    void periodoAnteriorSinDatosOmiteVariacion() {
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes = inicioMes.plusMonths(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(List.of(emision("2500")));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMesAnterior, inicioMes))
                .thenReturn(List.of());

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "mes_actual", null);

        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("2.5000");
        assertThat(response.getVariacionPorcentual()).isNull();
        assertThat(response.isTieneDatos()).isTrue();
    }

    @Test
    void periodoInvalidoUsaMesActualPorDefecto() {
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes = inicioMes.plusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(eq(EMPRESA_ID), eq(inicioMes), eq(finMes)))
                .thenReturn(List.of());

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "semana", null);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("mes_actual");
        verify(emisionRepository).findAllByEmpresaIdAndPeriodo(EMPRESA_ID, inicioMes, finMes);
    }

    @Test
    void resumenMesActualUsaAnioSeleccionado() {
        int anioSeleccionado = 2021;
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMesSeleccionado = LocalDate.of(anioSeleccionado, hoy.getMonth(), 1);
        LocalDate finMesSeleccionado = inicioMesSeleccionado.plusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID,
                inicioMesSeleccionado,
                finMesSeleccionado))
                .thenReturn(List.of());

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(
                USUARIO_ID,
                "mes_actual",
                anioSeleccionado);

        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("0.0000");
        assertThat(response.isTieneDatos()).isFalse();
        verify(emisionRepository).findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID,
                inicioMesSeleccionado,
                finMesSeleccionado);
    }

    private Emision emision(String carbonKg) {
        return EmisionElectricidad.builder()
                .carbonKg(new BigDecimal(carbonKg))
                .carbonMt(BigDecimal.ZERO)
                .titulo("Consumo")
                .fechaActividad(LocalDate.now())
                .factorEmisionId("factor")
                .build();
    }
}
