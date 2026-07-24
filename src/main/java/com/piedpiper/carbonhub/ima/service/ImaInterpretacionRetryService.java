package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tarea programada que reintenta la generación de interpretación IA
 * para snapshots que quedaron con "No disponible".
 */
@Service
public class ImaInterpretacionRetryService {

    private static final Logger log = LoggerFactory.getLogger(ImaInterpretacionRetryService.class);

    private final ImaSnapshotRepository imaSnapshotRepository;
    private final ImaInterpretacionService interpretacionService;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final EmpresaRepository empresaRepository;

    public ImaInterpretacionRetryService(ImaSnapshotRepository imaSnapshotRepository,
                                         ImaInterpretacionService interpretacionService,
                                         AgregadoSectorialRepository agregadoSectorialRepository,
                                         EmpresaRepository empresaRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.interpretacionService = interpretacionService;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Cada 5 minutos (configurable), busca snapshots con interpretación "No disponible"
     * e intenta regenerar la interpretación IA.
     */
    @Scheduled(fixedDelayString = "${ima.reintento-intervalo-ms:300000}")
    public void reintentarInterpretacionesFallidas() {
        List<ImaSnapshot> pendientes = imaSnapshotRepository
                .findByInterpretacion("No disponible");

        if (pendientes.isEmpty()) return;

        log.info("Reintentando interpretación IA para {} snapshots", pendientes.size());

        for (ImaSnapshot snapshot : pendientes) {
            try {
                Empresa empresa = empresaRepository.findById(snapshot.getEmpresaId()).orElse(null);
                if (empresa == null) continue;

                SectorIndustrial sector = empresa.getSectorIndustrial();
                AgregadoSectorial agregado = agregadoSectorialRepository
                        .findBySectorAndAnioAndMes(sector, snapshot.getAnio(), snapshot.getMes())
                        .orElse(null);
                if (agregado == null) continue;

                String tendencia = calcularTendencia(snapshot);

                interpretacionService.generarInterpretacion(
                        snapshot, sector.name(), agregado, tendencia);
            } catch (Exception e) {
                log.warn("Reintento fallido para snapshot {}: {}", snapshot.getId(), e.getMessage());
            }
        }
    }

    private String calcularTendencia(ImaSnapshot snapshot) {
        int prevMes = snapshot.getMes() == 1 ? 12 : snapshot.getMes() - 1;
        int prevAnio = snapshot.getMes() == 1 ? snapshot.getAnio() - 1 : snapshot.getAnio();
        return imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(
                        snapshot.getEmpresaId(), prevAnio, prevMes)
                .map(prev -> {
                    int cmp = snapshot.getIma().compareTo(prev.getIma());
                    if (cmp > 0) return "Subió";
                    if (cmp < 0) return "Bajó";
                    return "Estable";
                })
                .orElse("Sin datos previos");
    }
}
