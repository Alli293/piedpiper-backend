package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.mappers.EmpresaMapper;
import com.piedpiper.carbonhub.empresa.models.dtos.EmpresaReporteDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteHuellaPdfService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final ZoneId ZONA_COSTA_RICA = ZoneId.of("America/Costa_Rica");

    private final EmisionRepository emisionRepository;
    private final LimiteEmisionesRepository limiteEmisionesRepository;
    private final EmpresaMapper empresaMapper;
    private final UsuarioRepository usuarioRepository;
    private final ReporteHuellaPdfGenerator pdfGenerator;

    public ReporteHuellaPdfService(EmisionRepository emisionRepository,
                                   LimiteEmisionesRepository limiteEmisionesRepository,
                                   EmpresaMapper empresaMapper,
                                   UsuarioRepository usuarioRepository,
                                   ReporteHuellaPdfGenerator pdfGenerator) {
        this.emisionRepository = emisionRepository;
        this.limiteEmisionesRepository = limiteEmisionesRepository;
        this.empresaMapper = empresaMapper;
        this.usuarioRepository = usuarioRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public byte[] generar(UUID usuarioId, Integer anio, Integer mes) {
        validarPeriodo(anio, mes);
        EmpresaReporteDTO empresa = empresa(usuarioId);
        String nombreEmpresa = nombreEmpresa(empresa);

        Map<CategoriaEmision, BigDecimal> totalesPorCategoria = totalesPorCategoria(empresa.id(), anio, mes);
        BigDecimal totalKg = totalesPorCategoria.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalT = EmisionComparacionHelper.toneladasDesdeKg(totalKg);

        ReporteHuellaPdfDTO reporte = new ReporteHuellaPdfDTO(
                nombreEmpresa,
                anio,
                mes,
                totalKg,
                totalT,
                categorias(totalesPorCategoria, totalKg),
                comparacion(empresa.id(), anio),
                totalKg.compareTo(BigDecimal.ZERO) == 0,
                ZonedDateTime.now(ZONA_COSTA_RICA)
        );

        try {
            return pdfGenerator.generar(reporte);
        } catch (RuntimeException ex) {
            throw ApiException.errorInterno("No se pudo generar el reporte PDF. Intente nuevamente.");
        }
    }

    public String nombreArchivo(Integer anio, Integer mes) {
        return "reporte-huella-" + anio + (mes == null ? "" : "-" + String.format("%02d", mes)) + ".pdf";
    }

    private Map<CategoriaEmision, BigDecimal> totalesPorCategoria(UUID empresaId, Integer anio, Integer mes) {
        Map<CategoriaEmision, BigDecimal> totales = new EnumMap<>(CategoriaEmision.class);
        Arrays.stream(CategoriaEmision.values()).forEach(categoria -> totales.put(categoria, BigDecimal.ZERO));

        LocalDate inicio = mes == null
                ? LocalDate.of(anio, 1, 1)
                : LocalDate.of(anio, mes, 1);
        LocalDate fin = mes == null ? inicio.plusYears(1) : inicio.plusMonths(1);

        for (Emision emision : emisionRepository.findAllByEmpresaIdAndPeriodo(empresaId, inicio, fin)) {
            totales.merge(emision.getCategoria(), emision.getCarbonKg(), BigDecimal::add);
        }
        return totales;
    }

    private List<ReporteHuellaCategoriaDTO> categorias(
            Map<CategoriaEmision, BigDecimal> totalesPorCategoria,
            BigDecimal totalKg) {
        return Arrays.stream(CategoriaEmision.values())
                .map(categoria -> {
                    BigDecimal carbonKg = totalesPorCategoria.get(categoria);
                    BigDecimal porcentaje = totalKg.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : carbonKg.multiply(CIEN).divide(totalKg, 1, RoundingMode.HALF_UP);
                    return new ReporteHuellaCategoriaDTO(categoria, carbonKg, porcentaje);
                })
                .toList();
    }

    private ReporteHuellaComparacionDTO comparacion(UUID empresaId, Integer anio) {
        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anio)
                .map(limite -> {
                    BigDecimal acumuladoAnualKg = Optional.ofNullable(
                            emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                                    empresaId,
                                    LocalDate.of(anio, 1, 1),
                                    LocalDate.of(anio + 1, 1, 1))
                    ).orElse(BigDecimal.ZERO);
                    BigDecimal acumuladoAnualT = EmisionComparacionHelper.toneladasDesdeKg(acumuladoAnualKg);
                    BigDecimal limiteT = limite.getLimiteMt();
                    BigDecimal porcentaje = EmisionComparacionHelper.porcentajeConsumido(acumuladoAnualT, limiteT);
                    return new ReporteHuellaComparacionDTO(
                            acumuladoAnualT,
                            limiteT,
                            porcentaje,
                            EmisionComparacionHelper.estado(porcentaje)
                    );
                })
                .orElseGet(() -> new ReporteHuellaComparacionDTO(BigDecimal.ZERO, null, null, "sin_limite"));
    }

    private void validarPeriodo(Integer anio, Integer mes) {
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw ApiException.periodoInvalido("Año inválido.");
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            throw ApiException.periodoInvalido("Mes inválido.");
        }
    }

    private EmpresaReporteDTO empresa(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = empresa(usuario);
        return empresaMapper.toReporteDto(empresa);
    }

    private String nombreEmpresa(EmpresaReporteDTO empresa) {
        if (empresa.nombreEmpresa() == null || empresa.nombreEmpresa().isBlank()) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa.nombreEmpresa();
    }

    private Empresa empresa(Usuario usuario) {
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null || empresa.getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa;
    }
}
