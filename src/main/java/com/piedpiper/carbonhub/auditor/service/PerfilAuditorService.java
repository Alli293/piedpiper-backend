package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilAuditorService {

    private final PerfilAuditorRepository perfilAuditorRepository;

    public PerfilAuditorService(PerfilAuditorRepository perfilAuditorRepository) {
        this.perfilAuditorRepository = perfilAuditorRepository;
    }

    @Transactional
    public PerfilAuditor asegurarPerfil(Usuario usuario) {
        if (usuario.getRol() != Rol.AUDITOR_CERTIFICADO) {
            return null;
        }
        return perfilAuditorRepository.findByAuditorId(usuario.getId())
                .orElseGet(() -> perfilAuditorRepository.save(PerfilAuditor.builder().auditor(usuario).build()));
    }
}
