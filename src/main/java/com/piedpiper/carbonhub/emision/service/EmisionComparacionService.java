package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionCategoriaEmisionDTO;
import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmisionComparacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final List<CategoriaEmision> ORDEN_CATEGORIAS = List.of(
            CategoriaEmision.ELECTRICIDAD,
            CategoriaEmision.FLOTA,
            CategoriaEmision.VUELO,
            CategoriaEmision.ENVIO
    );

    private final EmisionRepository emisionRepository;
    private final LimiteEmisionesRepository limiteEmisionesRepository;
    private final EmisionEmpresaService emisionEmpresaService;

    public EmisionComparacionService(EmisionRepository emisionRepository,
                                     LimiteEmisionesRepository limiteEmisionesRepository,
                                     EmisionEmpresaService emisionEmpresaService) {
        this.emisionRepository = emisionRepository;
        this.limiteEmisionesRepository = limiteEmisionesRepository;
        this.emisionEmpresaService = emisionEmpresaService;
    }

    @Transactional(readOnly = true)
    public ComparacionEmisionesResponseDTO comparar(UUID usuarioId, Integer anio) {
        Integer anioComparar = anio == null ? Year.now().getValue() : anio;
        validarAnio(anioComparar);

        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        LocalDate inicioAnio = LocalDate.of(anioComparar, 1, 1);
        LocalDate finAnio = inicioAnio.plusYears(1);
        BigDecimal huellaKg = Optional.ofNullable(
                emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                        empresaId,
                        inicioAnio,
                        finAnio)
        ).orElse(BigDecimal.ZERO);
        BigDecimal huellaT = EmisionComparacionHelper.toneladasDesdeKg(huellaKg);
        List<ComparacionCategoriaEmisionDTO> categorias = categorias(
                empresaId,
                inicioAnio,
                finAnio,
                huellaKg
        );

        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anioComparar)
                .map(limite -> compararConLimite(anioComparar, huellaT, limite, categorias))
                .orElseGet(() -> new ComparacionEmisionesResponseDTO(
                        anioComparar,
                        huellaT,
                        null,
                        null,
                        "sin_limite",
                        "No se ha declarado un límite para " + anioComparar + ".",
                        categorias
                ));
    }

    private ComparacionEmisionesResponseDTO compararConLimite(
            Integer anio,
            BigDecimal huellaT,
            LimiteEmisiones limite,
            List<ComparacionCategoriaEmisionDTO> categorias) {
        BigDecimal limiteT = limite.getLimiteMt();
        BigDecimal porcentaje = EmisionComparacionHelper.porcentajeConsumido(huellaT, limiteT);

        return new ComparacionEmisionesResponseDTO(
                anio,
                huellaT,
                limiteT,
                porcentaje,
                EmisionComparacionHelper.estado(porcentaje),
                null,
                categorias
        );
    }

    private List<ComparacionCategoriaEmisionDTO> categorias(
            UUID empresaId,
            LocalDate inicio,
            LocalDate fin,
            BigDecimal huellaTotalKg) {
        Map<CategoriaEmision, BigDecimal> totales = new EnumMap<>(CategoriaEmision.class);
        ORDEN_CATEGORIAS.forEach(categoria -> totales.put(categoria, BigDecimal.ZERO));

        List<Emision> emisiones = Optional.ofNullable(
                emisionRepository.findAllByEmpresaIdAndPeriodo(empresaId, inicio, fin)
        ).orElse(List.of());

        emisiones.forEach(emision -> totales.merge(
                emision.getCategoria(),
                emision.getCarbonKg(),
                BigDecimal::add
        ));

        return ORDEN_CATEGORIAS.stream()
                .map(categoria -> {
                    BigDecimal categoriaKg = totales.getOrDefault(categoria, BigDecimal.ZERO);
                    return new ComparacionCategoriaEmisionDTO(
                            categoria,
                            EmisionComparacionHelper.toneladasDesdeKg(categoriaKg),
                            porcentajeCategoria(categoriaKg, huellaTotalKg)
                    );
                })
                .toList();
    }

    private BigDecimal porcentajeCategoria(BigDecimal categoriaKg, BigDecimal huellaTotalKg) {
        if (huellaTotalKg.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return categoriaKg.multiply(CIEN).divide(huellaTotalKg, 1, RoundingMode.HALF_UP);
    }

    private void validarAnio(Integer anio) {
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw ApiException.anioInvalido();
        }
    }
}
