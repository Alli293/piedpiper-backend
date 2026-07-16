package com.piedpiper.carbonhub.limite.service;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaAutenticadaService {
    private final UsuarioRepository usuarioRepository;

    public EmpresaAutenticadaService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UUID obtenerEmpresaId(Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa.getId();
    }
}
