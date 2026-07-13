package com.piedpiper.carbonhub.limite.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaAutenticadaService {
    private final UsuarioRepository usuarioRepository;

    public EmpresaAutenticadaService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UUID obtenerEmpresaId(String usuarioId) {
        UUID id = parsearUsuarioId(usuarioId);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Usuario autenticado no encontrado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El usuario no tiene una empresa asociada.");
        }
        return empresa.getId();
    }

    private UUID parsearUsuarioId(String usuarioId) {
        try {
            return UUID.fromString(usuarioId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuario autenticado invalido.");
        }
    }
}
