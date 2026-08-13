package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ImaCacheInvalidator {

    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final EmpresaRepository empresaRepository;

    public ImaCacheInvalidator(ImaSnapshotRepository imaSnapshotRepository,
                               AgregadoSectorialRepository agregadoSectorialRepository,
                               EmpresaRepository empresaRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Invalida el snapshot de la empresa y el de sus pares de sector: el puntaje de
     * intensidad sectorial y el umbral de 5 empresas de cada par dependen de las
     * emisiones de esta empresa, así que un snapshot de un par calculado antes de este
     * cambio quedaría con datos obsoletos (p. ej. "sector sin suficientes empresas")
     * indefinidamente, ya que su propio caché solo se invalida cuando ese par registra
     * una emisión.
     */
    @Transactional
    public void invalidar(UUID empresaId) {
        imaSnapshotRepository.deleteAllByEmpresaId(empresaId);

        empresaRepository.findById(empresaId).ifPresent(empresa -> {
            List<UUID> paresDeSector = empresaRepository
                    .findBySectorIndustrial(empresa.getSectorIndustrial()).stream()
                    .map(Empresa::getId)
                    .filter(id -> !id.equals(empresaId))
                    .toList();
            if (!paresDeSector.isEmpty()) {
                imaSnapshotRepository.deleteAllByEmpresaIdIn(paresDeSector);
            }
        });

        agregadoSectorialRepository.deleteAll();
    }
}
