package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmisionEmpresaService {
    private final UsuarioRepository usuarioRepository;

    public EmisionEmpresaService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null || empresa.getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa.getId();
    }
}
