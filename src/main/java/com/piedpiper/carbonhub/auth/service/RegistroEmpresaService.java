package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroEmpresaService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public RegistroEmpresaService(GoogleTokenVerifier googleTokenVerifier,
                                  EmpresaRepository empresaRepository,
                                  UsuarioRepository usuarioRepository,
                                  JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroEmpresaRequestDTO request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());

        if (usuarioRepository.existsByGoogleSub(claims.getSub())
                || usuarioRepository.existsByEmail(claims.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        }
        if (empresaRepository.existsByCorreoCorporativoIgnoreCase(request.getCorreoCorporativo())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con este correo corporativo. "
                            + "Si crees que es un error, contacta a soporte@carbonhub.cr.");
        }

        Empresa empresa = Empresa.builder()
                .nombre(request.getNombreEmpresa())
                .sectorIndustrial(request.getSectorIndustrial())
                .pais(request.getPais().toUpperCase())
                .cantidadEmpleados(request.getCantidadEmpleados())
                .correoCorporativo(request.getCorreoCorporativo())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
        empresa = empresaRepository.save(empresa);

        Usuario admin = Usuario.builder()
                .googleSub(claims.getSub())
                .email(claims.getEmail())
                .nombre(Usuario.recortarNombre(claims.getGivenName()))
                .apellidos(Usuario.recortarNombre(claims.getFamilyName()))
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .empresa(empresa)
                .fechaRegistro(Instant.now())
                .build();
        admin = usuarioRepository.save(admin);

        String token = jwtService.generar(admin);
        return new AuthResponseDTO(token, admin.getRol().name(), admin.getEstado().name(),
                "/empresa/configuracion-inicial");
    }
}
