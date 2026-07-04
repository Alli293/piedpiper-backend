package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.RegistroEmpresaRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.empresa.Empresa;
import com.piedpiper.carbonhub.empresa.EmpresaRepository;
import com.piedpiper.carbonhub.empresa.EstadoEmpresa;
import com.piedpiper.carbonhub.user.EstadoUsuario;
import com.piedpiper.carbonhub.user.MetodoAuth;
import com.piedpiper.carbonhub.user.Rol;
import com.piedpiper.carbonhub.user.Usuario;
import com.piedpiper.carbonhub.user.UsuarioRepository;
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
    public AuthResponse registrar(RegistroEmpresaRequest request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.idToken());

        if (usuarioRepository.existsByGoogleSub(claims.sub())
                || usuarioRepository.existsByEmail(claims.email())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        }
        if (empresaRepository.existsByCorreoCorporativoIgnoreCase(request.correoCorporativo())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con este correo corporativo. "
                            + "Si crees que es un error, contacta a soporte@carbonhub.cr.");
        }

        Empresa empresa = Empresa.builder()
                .nombre(request.nombreEmpresa())
                .sectorIndustrial(request.sectorIndustrial())
                .pais(request.pais().toUpperCase())
                .cantidadEmpleados(request.cantidadEmpleados())
                .correoCorporativo(request.correoCorporativo())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
        empresa = empresaRepository.save(empresa);

        Usuario admin = Usuario.builder()
                .googleSub(claims.sub())
                .email(claims.email())
                .nombre(claims.name())
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .empresa(empresa)
                .fechaRegistro(Instant.now())
                .build();
        admin = usuarioRepository.save(admin);

        String token = jwtService.generar(admin);
        return new AuthResponse(token, admin.getRol().name(), admin.getEstado().name(),
                "/empresa/configuracion-inicial");
    }
}
