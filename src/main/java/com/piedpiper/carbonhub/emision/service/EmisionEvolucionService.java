package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO.PuntoMensual;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmisionEvolucionService {

    private final EmisionRepository emisionRepository;
    private final EmisionEmpresaService emisionEmpresaService;

    public EmisionEvolucionService(EmisionRepository emisionRepository,
                                   EmisionEmpresaService emisionEmpresaService) {
        this.emisionRepository = emisionRepository;
        this.emisionEmpresaService = emisionEmpresaService;
    }

    @Transactional(readOnly = true)
    public EvolucionMensualResponseDTO obtenerEvolucion(Integer anio, UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);

        int anioEfectivo = anio != null ? anio : Year.now(ZoneId.systemDefault()).getValue();
        validarAnio(anioEfectivo);

        List<Object[]> resultados = emisionRepository.sumarCarbonKgPorMes(empresaId, anioEfectivo);

        Map<Integer, BigDecimal> porMes = new HashMap<>();
        for (Object[] fila : resultados) {
            int mes = ((Number) fila[0]).intValue();
            BigDecimal total = (BigDecimal) fila[1];
            porMes.put(mes, total);
        }

        List<PuntoMensual> serie = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            serie.add(new PuntoMensual(mes, porMes.getOrDefault(mes, BigDecimal.ZERO)));
        }

        return new EvolucionMensualResponseDTO(anioEfectivo, serie);
    }

    private void validarAnio(int anio) {
        int anioActual = Year.now(ZoneId.systemDefault()).getValue();
        if (anio < 1900 || anio > anioActual + 1) {
            throw ApiException.anioInvalido();
        }
    }
}
