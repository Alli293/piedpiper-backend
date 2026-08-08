package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EvolucionHuellaPublicaDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EvolucionHuellaPublicaDTO.PuntoAnual;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilPublicoEvolucionService {

    private static final BigDecimal KG_A_TONELADAS = new BigDecimal("1000");

    private final SlugResolverService slugResolver;
    private final EmisionRepository emisionRepository;

    public PerfilPublicoEvolucionService(SlugResolverService slugResolver,
                                         EmisionRepository emisionRepository) {
        this.slugResolver = slugResolver;
        this.emisionRepository = emisionRepository;
    }

    @Transactional(readOnly = true)
    public EvolucionHuellaPublicaDTO obtenerEvolucionPorSlug(String slugOriginal) {
        Empresa empresa = slugResolver.resolver(slugOriginal);
        UUID empresaId = empresa.getId();

        List<Object[]> resultados = emisionRepository.sumarCarbonKgPorAnio(empresaId);

        List<PuntoAnual> serie = new ArrayList<>();
        for (Object[] fila : resultados) {
            int anio = ((Number) fila[0]).intValue();
            BigDecimal totalKg = (BigDecimal) fila[1];
            BigDecimal totalTco2e = totalKg.divide(KG_A_TONELADAS, 3, RoundingMode.HALF_UP);
            serie.add(new PuntoAnual(anio, totalTco2e));
        }

        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal variacion = null;

        if (!serie.isEmpty()) {
            totalActual = serie.get(serie.size() - 1).getTotalTco2e();
            if (serie.size() >= 2) {
                BigDecimal anterior = serie.get(serie.size() - 2).getTotalTco2e();
                if (anterior.compareTo(BigDecimal.ZERO) > 0) {
                    variacion = totalActual.subtract(anterior)
                            .divide(anterior, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(1, RoundingMode.HALF_UP);
                }
            }
        }

        return new EvolucionHuellaPublicaDTO(totalActual, variacion, serie);
    }
}
