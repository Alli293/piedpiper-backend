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

        BigDecimal total = totalKg;
        List<ResumenCategoriaDTO> categorias = ORDEN_CATEGORIAS.stream()
                .map(categoria -> ResumenCategoriaDTO.builder()
                        .categoria(categoria)
                        .totalKg(subtotales.getOrDefault(categoria, BigDecimal.ZERO))
                        .porcentaje(porcentaje(subtotales.getOrDefault(categoria, BigDecimal.ZERO), total))
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

    private BigDecimal porcentaje(BigDecimal subtotal, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return subtotal.multiply(CIEN).divide(total, 1, RoundingMode.HALF_UP);
    }

    private UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.accesoDenegado("El usuario autenticado no pertenece a una empresa.");
        }
        return usuario.getEmpresa().getId();
    }
}
