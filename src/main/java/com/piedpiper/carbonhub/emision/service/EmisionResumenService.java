package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO.ResumenCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmisionResumenService {

    private static final List<CategoriaEmision> ORDEN_CATEGORIAS = List.of(
            CategoriaEmision.ELECTRICIDAD,
            CategoriaEmision.FLOTA,
            CategoriaEmision.VUELO,
            CategoriaEmision.ENVIO);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal KG_POR_TONELADA = BigDecimal.valueOf(1000);
    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = 2100;

    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;

    public EmisionResumenService(EmisionRepository emisionRepository,
                                 UsuarioRepository usuarioRepository) {
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public EmisionResumenResponseDTO resumen(Integer anio, Integer mes, UUID usuarioId) {
        validarPeriodo(anio, mes);

        LocalDate desde = mes == null ? LocalDate.of(anio, 1, 1) : LocalDate.of(anio, mes, 1);
        LocalDate hasta = mes == null
                ? LocalDate.of(anio, 12, 31)
                : desde.with(TemporalAdjusters.lastDayOfMonth());

        List<Emision> emisiones = emisionRepository
                .findAllByEmpresaIdAndFechaActividadBetween(empresaId(usuarioId), desde, hasta);

        Map<CategoriaEmision, BigDecimal> subtotales = new EnumMap<>(CategoriaEmision.class);
        BigDecimal totalKg = BigDecimal.ZERO;
        for (Emision emision : emisiones) {
            BigDecimal carbonKg = emision.getCarbonKg() == null ? BigDecimal.ZERO : emision.getCarbonKg();
            subtotales.merge(emision.getCategoria(), carbonKg, BigDecimal::add);
            totalKg = totalKg.add(carbonKg);
        }

        Map<CategoriaEmision, BigDecimal> porcentajes = porcentajesConMayorResto(subtotales, totalKg);
        List<ResumenCategoriaDTO> categorias = ORDEN_CATEGORIAS.stream()
                .map(categoria -> ResumenCategoriaDTO.builder()
                        .categoria(categoria)
                        .totalKg(subtotales.getOrDefault(categoria, BigDecimal.ZERO))
                        .porcentaje(porcentajes.get(categoria))
                        .build())
                .toList();

        return EmisionResumenResponseDTO.builder()
                .anio(anio)
                .mes(mes)
                .totalKg(totalKg)
                .totalT(totalKg.divide(KG_POR_TONELADA, 3, RoundingMode.HALF_UP))
                .categorias(categorias)
                .build();
    }

    private void validarPeriodo(Integer anio, Integer mes) {
        if (anio == null || anio < ANIO_MINIMO || anio > ANIO_MAXIMO) {
            throw ApiException.anioConsultaInvalido();
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            throw ApiException.mesConsultaInvalido();
        }
    }

    /**
     * Calcula el porcentaje de cada categoría con ajuste de mayor resto (largest remainder),
     * de modo que la suma de los porcentajes redondeados a 1 decimal sea siempre exactamente 100.0%
     * (cuando total > 0).
     */
    private Map<CategoriaEmision, BigDecimal> porcentajesConMayorResto(
            Map<CategoriaEmision, BigDecimal> subtotales, BigDecimal total) {

        Map<CategoriaEmision, BigDecimal> resultado = new EnumMap<>(CategoriaEmision.class);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            ORDEN_CATEGORIAS.forEach(categoria -> resultado.put(categoria, BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)));
            return resultado;
        }

        BigDecimal decimas = BigDecimal.TEN;
        Map<CategoriaEmision, BigDecimal> exactoEnDecimas = new EnumMap<>(CategoriaEmision.class);
        Map<CategoriaEmision, BigDecimal> pisoEnDecimas = new EnumMap<>(CategoriaEmision.class);
        int sumaPiso = 0;

        for (CategoriaEmision categoria : ORDEN_CATEGORIAS) {
            BigDecimal subtotal = subtotales.getOrDefault(categoria, BigDecimal.ZERO);
            // porcentaje expresado en décimas (ej. 33.3% -> 333), con precisión completa
            BigDecimal exacto = subtotal.multiply(CIEN).multiply(decimas)
                    .divide(total, 6, RoundingMode.HALF_UP);
            BigDecimal piso = exacto.setScale(0, RoundingMode.DOWN);
            exactoEnDecimas.put(categoria, exacto);
            pisoEnDecimas.put(categoria, piso);
            sumaPiso += piso.intValue();
        }

        int faltante = 1000 - sumaPiso; // 1000 décimas = 100.0%

        List<CategoriaEmision> porMayorResto = ORDEN_CATEGORIAS.stream()
                .sorted((a, b) -> exactoEnDecimas.get(b).subtract(pisoEnDecimas.get(b))
                        .compareTo(exactoEnDecimas.get(a).subtract(pisoEnDecimas.get(a))))
                .toList();

        for (int i = 0; i < porMayorResto.size(); i++) {
            CategoriaEmision categoria = porMayorResto.get(i);
            BigDecimal piso = pisoEnDecimas.get(categoria);
            BigDecimal ajustado = i < faltante ? piso.add(BigDecimal.ONE) : piso;
            resultado.put(categoria, ajustado.divide(decimas, 1, RoundingMode.HALF_UP));
        }

        return resultado;
    }

    private UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
    }
}
