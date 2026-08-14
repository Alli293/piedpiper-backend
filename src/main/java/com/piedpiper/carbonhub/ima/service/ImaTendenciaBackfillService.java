package com.piedpiper.carbonhub.ima.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Completa en segundo plano los snapshots de IMA que /tendencia detecta como faltantes
 * (empresa con emisiones registradas en un mes que nadie consultó puntualmente vía /api/ima).
 * Corre fuera de la petición HTTP que la disparó: ImaTendenciaService responde de inmediato con
 * lo que ya está cacheado y marca completando=true, y el cliente vuelve a consultar más tarde.
 */
@Service
public class ImaTendenciaBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ImaTendenciaBackfillService.class);

    private final ImaService imaService;

    public ImaTendenciaBackfillService(ImaService imaService) {
        this.imaService = imaService;
    }

    @Async
    public void completarMesesPendientes(UUID usuarioId, List<YearMonth> mesesPendientes) {
        for (YearMonth periodo : mesesPendientes) {
            try {
                imaService.obtenerIma(periodo.getYear(), periodo.getMonthValue(), usuarioId);
            } catch (Exception e) {
                log.warn("No se pudo completar el snapshot de tendencia para {} (usuario {}): {}",
                        periodo, usuarioId, e.getMessage());
            }
        }
    }
}
