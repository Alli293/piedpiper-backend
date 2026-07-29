package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Perfil publico de una empresa, identificado por su slug. Sin autenticacion
 * a proposito: lo consulta cualquier visitante de la pagina publica de la
 * empresa, no un usuario con sesion. Las rutas permitidas viven en
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/perfil-publico/{slug}")
public class PerfilPublicoController {

    private final PerfilPublicoCertificacionesService perfilPublicoCertificacionesService;
    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    public PerfilPublicoController(
            PerfilPublicoCertificacionesService perfilPublicoCertificacionesService,
            InsigniaEmpresaConsultaService insigniaEmpresaConsultaService) {
        this.perfilPublicoCertificacionesService = perfilPublicoCertificacionesService;
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
    }

    @GetMapping("/certificaciones")
    public ResponseEntity<List<CertificacionPublicaResponseDTO>> certificaciones(
            @PathVariable String slug) {
        return ResponseEntity.ok(perfilPublicoCertificacionesService.listarPorSlug(slug));
    }

    @GetMapping("/insignias")
    public ResponseEntity<List<InsigniaEmpresaResponseDTO>> insignias(@PathVariable String slug) {
        return ResponseEntity.ok(insigniaEmpresaConsultaService.listarPorSlug(slug));
    }
}
