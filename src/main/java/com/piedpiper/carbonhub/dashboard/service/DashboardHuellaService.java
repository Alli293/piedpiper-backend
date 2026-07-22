package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardHuellaService {

    private static final BigDecimal KG_POR_TONELADA = new BigDecimal("1000");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final String PERIODO_MES_ACTUAL = "mes_actual";
    private static final String PERIODO_TRIMESTRE = "trimestre";
    private static final String PERIODO_ANIO = "a\u00f1o";

    private final EmisionRepository emisionRepository;
    private final EmisionEmpresaService emisionEmpresaService;

    public DashboardHuellaService(EmisionRepository emisionRepository,
                                  EmisionEmpresaService emisionEmpresaService) {
        this.emisionRepository = emisionRepository;
        this.emisionEmpresaService = emisionEmpresaService;
    }

    @Transactional(readOnly = true)
    public ResumenHuellaDashboardResponseDTO obtenerResumen(UUID usuarioId, String periodo) {
        String periodoNormalizado = normalizarPeriodo(periodo);
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        RangoPeriodo rango = rangoActual(periodoNormalizado, LocalDate.now());

        List<Emision> emisionesActuales = emisiones(empresaId, rango.inicio(), rango.fin());
        BigDecimal huellaActualKg = totalKg(emisionesActuales);
        BigDecimal huellaTotalT = toneladasDesdeKg(huellaActualKg);
        BigDecimal variacion = emisionesActuales.isEmpty() ? null : variacionPorcentual(empresaId, rango, huellaActualKg);

        return new ResumenHuellaDashboardResponseDTO(
                periodoNormalizado,
                huellaTotalT,
                variacion,
                !emisionesActuales.isEmpty()
        );
    }

    private String normalizarPeriodo(String periodo) {
        if (PERIODO_TRIMESTRE.equals(periodo) || PERIODO_ANIO.equals(periodo)) {
            return periodo;
        }
        return PERIODO_MES_ACTUAL;
    }

    private RangoPeriodo rangoActual(String periodo, LocalDate hoy) {
        if (PERIODO_TRIMESTRE.equals(periodo)) {
            int mesInicial = (((hoy.getMonthValue() - 1) / 3) * 3) + 1;
            LocalDate inicio = LocalDate.of(hoy.getYear(), mesInicial, 1);
            return new RangoPeriodo(inicio, inicio.plusMonths(3), periodo);
        }
        if (PERIODO_ANIO.equals(periodo)) {
            LocalDate inicio = Year.of(hoy.getYear()).atDay(1);
            return new RangoPeriodo(inicio, inicio.plusYears(1), periodo);
        }

        LocalDate inicio = YearMonth.from(hoy).atDay(1);
        return new RangoPeriodo(inicio, inicio.plusMonths(1), periodo);
    }

    private BigDecimal variacionPorcentual(UUID empresaId, RangoPeriodo rango, BigDecimal actualKg) {
        RangoPeriodo anterior = rango.anterior();
        List<Emision> emisionesAnteriores = emisiones(empresaId, anterior.inicio(), anterior.fin());
        if (emisionesAnteriores.isEmpty()) {
            return null;
        }

        BigDecimal anteriorKg = totalKg(emisionesAnteriores);
        if (anteriorKg.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return actualKg.subtract(anteriorKg)
                .multiply(CIEN)
                .divide(anteriorKg, 1, RoundingMode.HALF_UP);
    }

    private List<Emision> emisiones(UUID empresaId, LocalDate inicio, LocalDate fin) {
        List<Emision> emisiones = emisionRepository.findAllByEmpresaIdAndPeriodo(empresaId, inicio, fin);
        return emisiones == null ? List.of() : emisiones;
    }

    private BigDecimal totalKg(List<Emision> emisiones) {
        return emisiones.stream()
                .map(Emision::getCarbonKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal toneladasDesdeKg(BigDecimal kg) {
        return kg.divide(KG_POR_TONELADA, 4, RoundingMode.HALF_UP);
    }

    private record RangoPeriodo(LocalDate inicio, LocalDate fin, String periodo) {
        private RangoPeriodo anterior() {
            if (PERIODO_TRIMESTRE.equals(periodo)) {
                return new RangoPeriodo(inicio.minusMonths(3), inicio, periodo);
            }
            if (PERIODO_ANIO.equals(periodo)) {
                return new RangoPeriodo(inicio.minusYears(1), inicio, periodo);
            }
            return new RangoPeriodo(inicio.minusMonths(1), inicio, periodo);
        }
    }
}
