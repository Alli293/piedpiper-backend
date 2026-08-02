package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsigniaEmpresaRegistroService {

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;

    public InsigniaEmpresaRegistroService(InsigniaEmpresaRepository insigniaEmpresaRepository) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
    }

    @Transactional
    public void registrar(InsigniaEmpresa insigniaEmpresa) {
        insigniaEmpresaRepository.saveAndFlush(insigniaEmpresa);
    }
}
