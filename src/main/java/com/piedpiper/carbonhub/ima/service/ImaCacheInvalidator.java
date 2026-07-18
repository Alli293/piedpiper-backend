package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImaCacheInvalidator {

    private final ImaSnapshotRepository imaSnapshotRepository;

    public ImaCacheInvalidator(ImaSnapshotRepository imaSnapshotRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
    }

    @Transactional
    public void invalidar(UUID empresaId) {
        imaSnapshotRepository.deleteAllByEmpresaId(empresaId);
    }
}
