package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.service.LoginService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroInvitacionService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioCorreoService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioService;
import com.piedpiper.carbonhub.auth.service.RestablecerContrasenaService;
import com.piedpiper.carbonhub.auth.service.VerificarCorreoService;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.LoginRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ReenviarVerificacionRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RestablecerContrasenaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.SolicitarResetContrasenaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ValidarTokenResetResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroUsuarioService registroUsuarioService;
    private final RegistroEmpresaService registroEmpresaService;
    private final RegistroAuditorService registroAuditorService;
    private final RegistroAuditorCorreoService registroAuditorCorreoService;
    private final LoginService loginService;
    private final RegistroUsuarioCorreoService registroUsuarioCorreoService;
    private final RegistroEmpresaCorreoService registroEmpresaCorreoService;
    private final VerificarCorreoService verificarCorreoService;
    private final RegistroInvitacionService registroInvitacionService;
    private final RestablecerContrasenaService restablecerContrasenaService;
    private final RegistroInvitacionCorreoService registroInvitacionCorreoService;

    public AuthController(RegistroUsuarioService registroUsuarioService,
                          RegistroEmpresaService registroEmpresaService,
                          RegistroAuditorService registroAuditorService,
                          RegistroAuditorCorreoService registroAuditorCorreoService,
                          LoginService loginService,
                          RegistroUsuarioCorreoService registroUsuarioCorreoService,
                          RegistroEmpresaCorreoService registroEmpresaCorreoService,
                          VerificarCorreoService verificarCorreoService,
                          RegistroInvitacionService registroInvitacionService,
                          RestablecerContrasenaService restablecerContrasenaService,
                          RegistroInvitacionCorreoService registroInvitacionCorreoService) {
        this.registroUsuarioService = registroUsuarioService;
        this.registroEmpresaService = registroEmpresaService;
        this.registroAuditorService = registroAuditorService;
        this.registroAuditorCorreoService = registroAuditorCorreoService;
        this.loginService = loginService;
        this.registroUsuarioCorreoService = registroUsuarioCorreoService;
        this.registroEmpresaCorreoService = registroEmpresaCorreoService;
        this.verificarCorreoService = verificarCorreoService;
        this.registroInvitacionService = registroInvitacionService;
        this.restablecerContrasenaService = restablecerContrasenaService;
        this.registroInvitacionCorreoService = registroInvitacionCorreoService;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(loginService.login(request));
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

    @GetMapping("/verificar-correo")
    public ResponseEntity<MensajeResponseDTO> verificarCorreo(@RequestParam String token) {
        return ResponseEntity.ok(verificarCorreoService.verificar(token));
    }

    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<MensajeResponseDTO> reenviarVerificacion(
            @Valid @RequestBody ReenviarVerificacionRequestDTO request) {
        return ResponseEntity.ok(verificarCorreoService.reenviar(request.getEmail()));
    }

    @PostMapping("/solicitar-reset-contrasena")
    public ResponseEntity<MensajeResponseDTO> solicitarResetContrasena(
            @Valid @RequestBody SolicitarResetContrasenaRequestDTO request) {
        return ResponseEntity.ok(restablecerContrasenaService.solicitar(request.getEmail()));
    }

    @GetMapping("/reset-contrasena")
    public ResponseEntity<ValidarTokenResetResponseDTO> validarTokenReset(@RequestParam String token) {
        return ResponseEntity.ok(restablecerContrasenaService.validarToken(token));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<MensajeResponseDTO> restablecerContrasena(
            @Valid @RequestBody RestablecerContrasenaRequestDTO request) {
        return ResponseEntity.ok(restablecerContrasenaService.restablecer(
                request.getToken(), request.getNuevaContrasena()));
    }
}
