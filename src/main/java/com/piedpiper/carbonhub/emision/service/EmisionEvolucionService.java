package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO.PuntoMensual;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmisionEvolucionService {

    private final EmisionRepository emisionRepository;
    private final UsuarioRepository usuarioRepository;

    public EmisionEvolucionService(EmisionRepository emisionRepository,
                                   UsuarioRepository usuarioRepository) {
        this.emisionRepository = emisionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public EvolucionMensualDTO obtenerEvolucion(Integer anio, UUID usuarioId) {
        UUID empresaId = empresaId(usuarioId);

        int anioEfectivo = anio != null ? anio : Year.now().getValue();

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

        return new EvolucionMensualDTO(anioEfectivo, serie);
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
