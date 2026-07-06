package com.piedpiper.carbonhub.auth.controller;

import com.piedpiper.carbonhub.auth.service.LoginService;
import com.piedpiper.carbonhub.auth.service.RegistroAuditorService;
import com.piedpiper.carbonhub.auth.service.RegistroEmpresaService;
import com.piedpiper.carbonhub.auth.service.RegistroUsuarioService;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.LoginRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroUsuarioService registroUsuarioService;
    private final RegistroEmpresaService registroEmpresaService;
    private final RegistroAuditorService registroAuditorService;
    private final LoginService loginService;

    public AuthController(RegistroUsuarioService registroUsuarioService,
                          RegistroEmpresaService registroEmpresaService,
                          RegistroAuditorService registroAuditorService,
                          LoginService loginService) {
        this.registroUsuarioService = registroUsuarioService;
        this.registroEmpresaService = registroEmpresaService;
        this.registroAuditorService = registroAuditorService;
        this.loginService = loginService;
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

    @PostMapping(value = "/registro/auditor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensajeResponseDTO> registrarAuditor(
            @RequestParam("idToken") String idToken,
            @RequestParam("nombreCompleto") String nombreCompleto,
            @RequestParam("numeroCertificacion") String numeroCertificacion,
            @RequestParam("entidadCertificadora") String entidadCertificadora,
            @RequestParam("fechaVigenciaCert") LocalDate fechaVigenciaCert,
            @RequestParam("aniosExperiencia") Integer aniosExperiencia,
            @RequestParam("aceptaTerminos") boolean aceptaTerminos,
            @RequestPart("docCertificado") MultipartFile docCertificado,
            @RequestPart("docIdentificacion") MultipartFile docIdentificacion) {
        RegistroAuditorRequestDTO request = new RegistroAuditorRequestDTO(idToken, nombreCompleto,
                numeroCertificacion, entidadCertificadora, fechaVigenciaCert, aniosExperiencia,
                aceptaTerminos);
        String mensaje = registroAuditorService.registrar(request, docCertificado, docIdentificacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MensajeResponseDTO(mensaje));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(loginService.login(request));
    }
}
