package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmisionComparacionService {

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

        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anioComparar)
                .map(limite -> compararConLimite(anioComparar, huellaT, limite))
                .orElseGet(() -> new ComparacionEmisionesResponseDTO(
                        anioComparar,
                        huellaT,
                        null,
                        null,
                        "sin_limite",
                        "No se ha declarado un lÃ­mite para " + anioComparar + "."
                ));
    }

    private ComparacionEmisionesResponseDTO compararConLimite(
            Integer anio,
            BigDecimal huellaT,
            LimiteEmisiones limite) {
        BigDecimal limiteT = limite.getLimiteMt();
        BigDecimal porcentaje = EmisionComparacionHelper.porcentajeConsumido(huellaT, limiteT);

        return new ComparacionEmisionesResponseDTO(
                anio,
                huellaT,
                limiteT,
                porcentaje,
                EmisionComparacionHelper.estado(porcentaje),
                null
        );
    }

    private void validarAnio(Integer anio) {
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw ApiException.periodoInvalido("AÃ±o invÃ¡lido.");
        }
    }
}
