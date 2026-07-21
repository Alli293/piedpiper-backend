package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImaCacheInvalidator {

    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;

    public ImaCacheInvalidator(ImaSnapshotRepository imaSnapshotRepository,
                               AgregadoSectorialRepository agregadoSectorialRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
    }

    @Transactional
    public void invalidar(UUID empresaId) {
        imaSnapshotRepository.deleteAllByEmpresaId(empresaId);
        agregadoSectorialRepository.deleteAll();
    }
}
