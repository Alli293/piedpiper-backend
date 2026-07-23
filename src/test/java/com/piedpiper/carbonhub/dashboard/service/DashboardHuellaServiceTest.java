package com.piedpiper.carbonhub.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(new BigDecimal("5236"));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMesAnterior, inicioMes))
                .thenReturn(new BigDecimal("4000"));

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "mes_actual", null);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("mes_actual");
        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("5.2360");
        assertThat(response.getVariacionPorcentual()).isEqualByComparingTo("30.9");
        assertThat(response.isTieneDatos()).isTrue();
    }

    @Test
    void resumenTrimestreSumaPeriodoSeleccionadoYCalculaVariacion() {
        LocalDate hoy = LocalDate.now();
        int mesInicial = (((hoy.getMonthValue() - 1) / 3) * 3) + 1;
        LocalDate inicioTrimestre = LocalDate.of(2021, mesInicial, 1);
        LocalDate finTrimestre = inicioTrimestre.plusMonths(3);
        LocalDate inicioTrimestreAnterior = inicioTrimestre.minusMonths(3);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                inicioTrimestre,
                finTrimestre))
                .thenReturn(new BigDecimal("9000"));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                inicioTrimestreAnterior,
                inicioTrimestre))
                .thenReturn(new BigDecimal("6000"));

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "trimestre", 2021);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("trimestre");
        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("9.0000");
        assertThat(response.getVariacionPorcentual()).isEqualByComparingTo("50.0");
        assertThat(response.isTieneDatos()).isTrue();
    }

    @Test
    void resumenAnioSumaPeriodoSeleccionadoYCalculaVariacion() {
        LocalDate inicioAnio = LocalDate.of(2021, 1, 1);
        LocalDate finAnio = inicioAnio.plusYears(1);
        LocalDate inicioAnioAnterior = inicioAnio.minusYears(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioAnio, finAnio))
                .thenReturn(new BigDecimal("12000"));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                inicioAnioAnterior,
                inicioAnio))
                .thenReturn(new BigDecimal("15000"));

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "año", 2021);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("año");
        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("12.0000");
        assertThat(response.getVariacionPorcentual()).isEqualByComparingTo("-20.0");
        assertThat(response.isTieneDatos()).isTrue();
    }

    @Test
    void periodoSinDatosDevuelveCeroSinVariacion() {
        LocalDate inicioMes = YearMonth.now().atDay(1);
        LocalDate finMes = inicioMes.plusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(null);

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
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(new BigDecimal("2500"));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMesAnterior, inicioMes))
                .thenReturn(null);

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
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMes, finMes))
                .thenReturn(null);

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(USUARIO_ID, "semana", null);

        assertThat(response.getPeriodoSeleccionado()).isEqualTo("mes_actual");
        verify(emisionRepository).sumCarbonKgByEmpresaIdAndFechaActividadEntre(EMPRESA_ID, inicioMes, finMes);
    }

    @Test
    void resumenMesActualUsaAnioSeleccionado() {
        int anioSeleccionado = 2021;
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMesSeleccionado = LocalDate.of(anioSeleccionado, hoy.getMonth(), 1);
        LocalDate finMesSeleccionado = inicioMesSeleccionado.plusMonths(1);

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                inicioMesSeleccionado,
                finMesSeleccionado))
                .thenReturn(null);

        ResumenHuellaDashboardResponseDTO response = service.obtenerResumen(
                USUARIO_ID,
                "mes_actual",
                anioSeleccionado);

        assertThat(response.getHuellaTotalT()).isEqualByComparingTo("0.0000");
        assertThat(response.isTieneDatos()).isFalse();
        verify(emisionRepository).sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                inicioMesSeleccionado,
                finMesSeleccionado);
    }

    @Test
    void anioInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.obtenerResumen(USUARIO_ID, "mes_actual", 1899))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anioFuturoFueraDeRangoDevuelve400() {
        int anioInvalido = Year.now().getValue() + 2;

        assertThatThrownBy(() -> service.obtenerResumen(USUARIO_ID, "mes_actual", anioInvalido))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
