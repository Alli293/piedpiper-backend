package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.common.RangosPeriodoDashboard;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.enums.PeriodoDashboard;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardHuellaService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final EmisionRepository emisionRepository;
    private final EmisionEmpresaService emisionEmpresaService;

    public DashboardHuellaService(EmisionRepository emisionRepository,
                                  EmisionEmpresaService emisionEmpresaService) {
        this.emisionRepository = emisionRepository;
        this.emisionEmpresaService = emisionEmpresaService;
    }

    @Transactional(readOnly = true)
    public ResumenHuellaDashboardResponseDTO obtenerResumen(UUID usuarioId, String periodo, Integer anio) {
        PeriodoDashboard periodoNormalizado = PeriodoDashboard.desde(periodo)
                .orElse(PeriodoDashboard.POR_DEFECTO);
        int anioConsultar = anio == null ? Year.now(ZoneId.systemDefault()).getValue() : anio;
        validarAnio(anioConsultar);
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        RangoPeriodo rango = rangoActual(periodoNormalizado, anioConsultar, LocalDate.now(ZoneId.systemDefault()));

        TotalPeriodo actual = totalPeriodo(empresaId, rango);
        BigDecimal huellaTotalT = HuellasCarbono.toneladasDesdeKg(actual.carbonKg());
        BigDecimal variacion = actual.tieneDatos()
                ? variacionPorcentual(empresaId, rango, actual.carbonKg())
                : null;

        return new ResumenHuellaDashboardResponseDTO(
                periodoNormalizado.getValor(),
                huellaTotalT,
                variacion,
                actual.tieneDatos()
        );
    }

    private void validarAnio(Integer anio) {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();
        if (anio < 1900 || anio > anioActual + 1) {
            throw ApiException.anioInvalido();
        }
    }

    private RangoPeriodo rangoActual(PeriodoDashboard periodo, Integer anio, LocalDate hoy) {
        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(periodo, anio, hoy);
        return new RangoPeriodo(rango.inicio(), rango.fin(), periodo);
    }

    private BigDecimal variacionPorcentual(UUID empresaId, RangoPeriodo rango, BigDecimal actualKg) {
        TotalPeriodo anterior = totalPeriodo(empresaId, rango.anterior());
        if (!anterior.tieneDatos()) {
            return null;
        }

        BigDecimal anteriorKg = anterior.carbonKg();
        if (anteriorKg.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return actualKg.subtract(anteriorKg)
                .multiply(CIEN)
                .divide(anteriorKg, 1, RoundingMode.HALF_UP);
    }

    private TotalPeriodo totalPeriodo(UUID empresaId, RangoPeriodo rango) {
        BigDecimal carbonKg = emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                empresaId,
                rango.inicio(),
                rango.fin());
        return new TotalPeriodo(Optional.ofNullable(carbonKg).orElse(BigDecimal.ZERO), carbonKg != null);
    }

    private record TotalPeriodo(BigDecimal carbonKg, boolean tieneDatos) {
    }

    private record RangoPeriodo(LocalDate inicio, LocalDate fin, PeriodoDashboard periodo) {
        private RangoPeriodo anterior() {
            if (PeriodoDashboard.TRIMESTRE == periodo) {
                return new RangoPeriodo(inicio.minusMonths(3), inicio, periodo);
            }
            if (PeriodoDashboard.ANIO == periodo) {
                return new RangoPeriodo(inicio.minusYears(1), inicio, periodo);
            }
            return new RangoPeriodo(inicio.minusMonths(1), inicio, periodo);
        }
    }
}
