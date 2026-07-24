package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import com.piedpiper.carbonhub.ecoruta.repository.InsigniaUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EcoRutaInsigniaRegistroService {

    private final InsigniaUsuarioRepository insigniaUsuarioRepository;

    public EcoRutaInsigniaRegistroService(InsigniaUsuarioRepository insigniaUsuarioRepository) {
        this.insigniaUsuarioRepository = insigniaUsuarioRepository;
    }

    @Transactional
    public void registrar(InsigniaUsuario insigniaUsuario) {
        insigniaUsuarioRepository.saveAndFlush(insigniaUsuario);
    }
}
