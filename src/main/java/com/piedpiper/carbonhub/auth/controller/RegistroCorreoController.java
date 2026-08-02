package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.service.RegistroAuditorCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioCorreoService;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioCorreoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro con contraseña que queda pendiente hasta verificar el correo. */
@RestController
@RequestMapping("/api/auth")
public class RegistroCorreoController {

    private final RegistroUsuarioCorreoService registroUsuarioCorreoService;
    private final RegistroEmpresaCorreoService registroEmpresaCorreoService;
    private final RegistroAuditorCorreoService registroAuditorCorreoService;
    private final RegistroInvitacionCorreoService registroInvitacionCorreoService;

    public RegistroCorreoController(RegistroUsuarioCorreoService registroUsuarioCorreoService,
                                    RegistroEmpresaCorreoService registroEmpresaCorreoService,
                                    RegistroAuditorCorreoService registroAuditorCorreoService,
                                    RegistroInvitacionCorreoService registroInvitacionCorreoService) {
        this.registroUsuarioCorreoService = registroUsuarioCorreoService;
        this.registroEmpresaCorreoService = registroEmpresaCorreoService;
        this.registroAuditorCorreoService = registroAuditorCorreoService;
        this.registroInvitacionCorreoService = registroInvitacionCorreoService;
    }

    @PostMapping("/registro/usuario/correo")
    public ResponseEntity<RegistroPendienteResponseDTO> registrarUsuarioCorreo(
            @Valid @RequestBody RegistroUsuarioCorreoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroUsuarioCorreoService.registrar(request));
    }

    @PostMapping("/registro/empresa/correo")
    public ResponseEntity<RegistroPendienteResponseDTO> registrarEmpresaCorreo(
            @Valid @RequestBody RegistroEmpresaCorreoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroEmpresaCorreoService.registrar(request));
    }

    @PostMapping("/registro/auditor/correo")
    public ResponseEntity<RegistroPendienteResponseDTO> registrarAuditorCorreo(
            @Valid @RequestBody RegistroAuditorCorreoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroAuditorCorreoService.registrar(request));
    }

    @PostMapping("/registro/invitacion/correo")
    public ResponseEntity<AuthResponseDTO> registrarPorInvitacionCorreo(
            @Valid @RequestBody RegistroInvitacionCorreoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroInvitacionCorreoService.registrar(request));
    }
}
