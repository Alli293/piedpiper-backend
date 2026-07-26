package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.service.LoginService;
import com.piedpiper.carbonhub.auth.service.RestablecerContrasenaService;
import com.piedpiper.carbonhub.auth.service.VerificarCorreoService;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.LoginRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ReenviarVerificacionRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RestablecerContrasenaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.SolicitarResetContrasenaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ValidarTokenResetResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Login, verificación de correo y recuperación de contraseña de una cuenta ya existente. */
@RestController
@RequestMapping("/api/auth")
public class SesionController {

    private final LoginService loginService;
    private final VerificarCorreoService verificarCorreoService;
    private final RestablecerContrasenaService restablecerContrasenaService;

    public SesionController(LoginService loginService,
                            VerificarCorreoService verificarCorreoService,
                            RestablecerContrasenaService restablecerContrasenaService) {
        this.loginService = loginService;
        this.verificarCorreoService = verificarCorreoService;
        this.restablecerContrasenaService = restablecerContrasenaService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(loginService.login(request));
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
