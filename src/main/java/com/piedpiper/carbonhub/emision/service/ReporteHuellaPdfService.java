package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaComparacionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteHuellaPdfService {

    private static final BigDecimal KG_POR_TONELADA = new BigDecimal("1000");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal UMBRAL_CERCA = new BigDecimal("80.0");
    private static final BigDecimal UMBRAL_SUPERADO = new BigDecimal("100.0");

    private final EmisionRepository emisionRepository;
    private final LimiteEmisionesRepository limiteEmisionesRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteHuellaPdfGenerator pdfGenerator;

    public ReporteHuellaPdfService(EmisionRepository emisionRepository,
                                   LimiteEmisionesRepository limiteEmisionesRepository,
                                   UsuarioRepository usuarioRepository,
                                   ReporteHuellaPdfGenerator pdfGenerator) {
        this.emisionRepository = emisionRepository;
        this.limiteEmisionesRepository = limiteEmisionesRepository;
        this.usuarioRepository = usuarioRepository;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public byte[] generar(UUID usuarioId, Integer anio, Integer mes) {
        validarPeriodo(anio, mes);
        Empresa empresa = empresa(usuarioId);

        Map<CategoriaEmision, BigDecimal> totalesPorCategoria = totalesPorCategoria(empresa.getId(), anio, mes);
        BigDecimal totalKg = totalesPorCategoria.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalT = totalKg.divide(KG_POR_TONELADA, 4, RoundingMode.HALF_UP);

        ReporteHuellaPdfDTO reporte = new ReporteHuellaPdfDTO(
                empresa.getNombreEmpresa(),
                anio,
                mes,
                totalKg,
                totalT,
                categorias(totalesPorCategoria, totalKg),
                comparacion(empresa.getId(), anio, totalT),
                totalKg.compareTo(BigDecimal.ZERO) == 0,
                ZonedDateTime.now()
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

        List<Object[]> filas = mes == null
                ? emisionRepository.sumCarbonKgByCategoriaAndAnio(empresaId, anio)
                : emisionRepository.sumCarbonKgByCategoriaAndMes(empresaId, anio, mes);
        for (Object[] fila : filas) {
            CategoriaEmision categoria = CategoriaEmision.valueOf(String.valueOf(fila[0]));
            BigDecimal total = fila[1] instanceof BigDecimal decimal
                    ? decimal
                    : new BigDecimal(String.valueOf(fila[1]));
            totales.put(categoria, total);
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

    private ReporteHuellaComparacionDTO comparacion(UUID empresaId, Integer anio, BigDecimal totalT) {
        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anio)
                .map(limite -> {
                    BigDecimal limiteT = limite.getLimiteMt();
                    BigDecimal porcentaje = totalT.multiply(CIEN).divide(limiteT, 1, RoundingMode.HALF_UP);
                    return new ReporteHuellaComparacionDTO(totalT, limiteT, porcentaje, estado(porcentaje));
                })
                .orElseGet(() -> new ReporteHuellaComparacionDTO(totalT, null, null, "sin_limite"));
    }

    private String estado(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_SUPERADO) > 0) {
            return "superado";
        }
        if (porcentaje.compareTo(UMBRAL_CERCA) >= 0) {
            return "cerca";
        }
        return "dentro";
    }

    private void validarPeriodo(Integer anio, Integer mes) {
        if (anio == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Anio requerido.");
        }
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Anio invalido.");
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mes invalido.");
        }
    }

    private Empresa empresa(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null || empresa.getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa;
    }
}
