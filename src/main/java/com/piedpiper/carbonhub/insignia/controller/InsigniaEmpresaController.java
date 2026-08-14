package com.piedpiper.carbonhub.insignia.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaOpenBadgesService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InsigniaEmpresaController {

    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;
    private final InsigniaEmpresaOpenBadgesService insigniaEmpresaOpenBadgesService;

    public InsigniaEmpresaController(InsigniaEmpresaConsultaService insigniaEmpresaConsultaService,
                                     InsigniaEmpresaOpenBadgesService
                                             insigniaEmpresaOpenBadgesService) {
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
        this.insigniaEmpresaOpenBadgesService = insigniaEmpresaOpenBadgesService;
    }

    @GetMapping("/empresas/insignias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
    public ResponseEntity<List<InsigniaEmpresaResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(insigniaEmpresaConsultaService.listarParaEmpresaAutenticada(
                Autenticaciones.usuarioId(authentication)));
    }

    @GetMapping(value = "/insignias/{idInsigniaEmpresa}/jsonld", produces = "application/ld+json")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
    public ResponseEntity<Map<String, Object>> descargarJsonLd(
            @PathVariable UUID idInsigniaEmpresa,
            Authentication authentication) {
        InsigniaEmpresaOpenBadgesService.DocumentoInsigniaOpenBadges documento =
                insigniaEmpresaOpenBadgesService.generarParaEmpresaAutenticada(
                        Autenticaciones.usuarioId(authentication), idInsigniaEmpresa);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/ld+json"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(documento.nombreArchivo())
                        .build()
                        .toString())
                .body(documento.contenido());
    }

    @GetMapping(value = "/insignias/{idInsigniaEmpresa}/verificacion", produces = "application/ld+json")
    public ResponseEntity<Map<String, Object>> verificacionPublica(
            @PathVariable UUID idInsigniaEmpresa) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/ld+json"))
                .body(insigniaEmpresaOpenBadgesService.generarPublica(idInsigniaEmpresa));
    }

    @GetMapping(value = "/insignias/{idInsigniaEmpresa}/verificacion.jwt",
            produces = "application/vc+ld+json+jwt")
    public ResponseEntity<String> verificacionJwt(@PathVariable UUID idInsigniaEmpresa) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vc+ld+json+jwt"))
                .body(insigniaEmpresaOpenBadgesService.generarJwtPublico(idInsigniaEmpresa));
    }

    @GetMapping(value = "/insignias/logros/{idInsignia}/{nivelInsignia}",
            produces = "application/ld+json")
    public ResponseEntity<Map<String, Object>> logroPublico(@PathVariable Long idInsignia,
                                                            @PathVariable String nivelInsignia) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/ld+json"))
                .body(insigniaEmpresaOpenBadgesService.generarLogro(idInsignia, nivelInsignia));
    }
}
