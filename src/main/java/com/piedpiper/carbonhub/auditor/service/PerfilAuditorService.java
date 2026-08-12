package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import org.springframework.dao.DataIntegrityViolationException;
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
                .orElseGet(() -> crear(usuario));
    }

    private PerfilAuditor crear(Usuario usuario) {
        try {
            return perfilAuditorRepository.save(PerfilAuditor.builder().auditor(usuario).build());
        } catch (DataIntegrityViolationException e) {
            // uk_perfiles_auditor_auditor: otra llamada concurrente (mismo patron que
            // AuditorPerfilService.actualizar) ya creo el perfil entre el findByAuditorId y este save.
            return perfilAuditorRepository.findByAuditorId(usuario.getId())
                    .orElseThrow(() -> ApiException.errorInterno("No se pudo crear el perfil del auditor."));
        }
    }
}
