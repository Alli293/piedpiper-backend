package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.LoginRequest;
import com.piedpiper.carbonhub.auth.dto.MensajeResponse;
import com.piedpiper.carbonhub.auth.dto.RegistroAuditorRequest;
import com.piedpiper.carbonhub.auth.dto.RegistroEmpresaRequest;
import com.piedpiper.carbonhub.auth.dto.RegistroUsuarioRequest;
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
    public ResponseEntity<AuthResponse> registrarUsuario(
            @Valid @RequestBody RegistroUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroUsuarioService.registrar(request));
    }

    @PostMapping("/registro/empresa")
    public ResponseEntity<AuthResponse> registrarEmpresa(
            @Valid @RequestBody RegistroEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroEmpresaService.registrar(request));
    }

    @PostMapping(value = "/registro/auditor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensajeResponse> registrarAuditor(
            @RequestParam("idToken") String idToken,
            @RequestParam("nombreCompleto") String nombreCompleto,
            @RequestParam("numeroCertificacion") String numeroCertificacion,
            @RequestParam("entidadCertificadora") String entidadCertificadora,
            @RequestParam("fechaVigenciaCert") LocalDate fechaVigenciaCert,
            @RequestParam("aniosExperiencia") Integer aniosExperiencia,
            @RequestParam("aceptaTerminos") boolean aceptaTerminos,
            @RequestPart("docCertificado") MultipartFile docCertificado,
            @RequestPart("docIdentificacion") MultipartFile docIdentificacion) {
        RegistroAuditorRequest request = new RegistroAuditorRequest(idToken, nombreCompleto,
                numeroCertificacion, entidadCertificadora, fechaVigenciaCert, aniosExperiencia,
                aceptaTerminos);
        String mensaje = registroAuditorService.registrar(request, docCertificado, docIdentificacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MensajeResponse(mensaje));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }
}
