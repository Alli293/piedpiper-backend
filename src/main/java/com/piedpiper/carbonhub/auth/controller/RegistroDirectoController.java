package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.service.RegistroAuditorService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioService;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro que autentica inmediatamente (id token de Google), sin paso de verificación por correo. */
@RestController
@RequestMapping("/api/auth")
public class RegistroDirectoController {

    private final RegistroUsuarioService registroUsuarioService;
    private final RegistroEmpresaService registroEmpresaService;
    private final RegistroAuditorService registroAuditorService;
    private final RegistroInvitacionService registroInvitacionService;

    public RegistroDirectoController(RegistroUsuarioService registroUsuarioService,
                                     RegistroEmpresaService registroEmpresaService,
                                     RegistroAuditorService registroAuditorService,
                                     RegistroInvitacionService registroInvitacionService) {
        this.registroUsuarioService = registroUsuarioService;
        this.registroEmpresaService = registroEmpresaService;
        this.registroAuditorService = registroAuditorService;
        this.registroInvitacionService = registroInvitacionService;
    }

    @PostMapping("/registro/usuario")
    public ResponseEntity<AuthResponseDTO> registrarUsuario(
            @Valid @RequestBody RegistroUsuarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroUsuarioService.registrar(request));
    }

    @PostMapping("/registro/empresa")
    public ResponseEntity<AuthResponseDTO> registrarEmpresa(
            @Valid @RequestBody RegistroEmpresaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroEmpresaService.registrar(request));
    }

    @PostMapping("/registro/auditor")
    public ResponseEntity<AuthResponseDTO> registrarAuditor(
            @Valid @RequestBody RegistroAuditorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroAuditorService.registrar(request));
    }

    @PostMapping("/registro/invitacion")
    public ResponseEntity<AuthResponseDTO> registrarPorInvitacion(
            @Valid @RequestBody RegistroInvitacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroInvitacionService.registrar(request));
    }
}
