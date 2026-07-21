package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmisionComparacionService {
    private static final BigDecimal KG_POR_TONELADA = new BigDecimal("1000");
    private static final BigDecimal UMBRAL_CERCA = new BigDecimal("80.0");
    private static final BigDecimal UMBRAL_SUPERADO = new BigDecimal("100.0");

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
                emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(empresaId, inicioAnio, finAnio)
        ).orElse(BigDecimal.ZERO);
        BigDecimal huellaT = huellaKg.divide(KG_POR_TONELADA, 4, RoundingMode.HALF_UP);

        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anioComparar)
                .map(limite -> compararConLimite(anioComparar, huellaT, limite))
                .orElseGet(() -> new ComparacionEmisionesResponseDTO(
                        anioComparar,
                        huellaT,
                        null,
                        null,
                        "sin_limite",
                        "No se ha declarado un límite para " + anioComparar + "."
                ));
    }

    private ComparacionEmisionesResponseDTO compararConLimite(
            Integer anio,
            BigDecimal huellaT,
            LimiteEmisiones limite) {
        BigDecimal limiteT = limite.getLimiteMt();
        BigDecimal porcentaje = huellaT
                .multiply(new BigDecimal("100"))
                .divide(limiteT, 1, RoundingMode.HALF_UP);

        return new ComparacionEmisionesResponseDTO(
                anio,
                huellaT,
                limiteT,
                porcentaje,
                estado(porcentaje),
                null
        );
    }

    private String estado(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_SUPERADO) > 0) {
            return "superado";
        }
        if (porcentaje.compareTo(UMBRAL_SUPERADO) == 0) {
            return "alcanzado";
        }
        if (porcentaje.compareTo(UMBRAL_CERCA) >= 0) {
            return "cerca";
        }
        return "dentro";
    }

    private void validarAnio(Integer anio) {
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw ApiException.anioInvalido();
        }
    }
}
