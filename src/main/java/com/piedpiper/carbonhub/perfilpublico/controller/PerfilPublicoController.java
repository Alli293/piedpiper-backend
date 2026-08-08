package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.BusquedaPerfilPublicoDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EvolucionHuellaPublicaDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;
import com.piedpiper.carbonhub.perfilpublico.service.EnlacePerfilService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoEvolucionService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Perfil publico de una empresa, identificado por su slug. Sin autenticacion
 * a proposito: lo consulta cualquier visitante de la pagina publica de la
 * empresa, no un usuario con sesion. Las rutas permitidas viven en
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/perfil-publico")
public class PerfilPublicoController {

    private final PerfilPublicoConsultaService service;
    private final PerfilPublicoCertificacionesService perfilPublicoCertificacionesService;
    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;
    private final EnlacePerfilService enlacePerfilService;
    private final PerfilPublicoEvolucionService evolucionService;

    public PerfilPublicoController(
            PerfilPublicoConsultaService service,
            PerfilPublicoCertificacionesService perfilPublicoCertificacionesService,
            InsigniaEmpresaConsultaService insigniaEmpresaConsultaService,
            EnlacePerfilService enlacePerfilService,
            PerfilPublicoEvolucionService evolucionService) {
        this.service = service;
        this.perfilPublicoCertificacionesService = perfilPublicoCertificacionesService;
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
        this.enlacePerfilService = enlacePerfilService;
        this.evolucionService = evolucionService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PerfilPublicoResponseDTO> obtener(@PathVariable String slug) {
        return ResponseEntity.ok(service.obtenerPorSlug(slug));
    }

    @GetMapping("/{slug}/certificaciones")
    public ResponseEntity<List<CertificacionPublicaResponseDTO>> certificaciones(
            @PathVariable String slug) {
        return ResponseEntity.ok(perfilPublicoCertificacionesService.listarPorSlug(slug));
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<BusquedaPerfilPublicoDTO>> buscar(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.buscarPorNombre(nombre, page, size));
    }

    @GetMapping("/{slug}/insignias")
    public ResponseEntity<List<InsigniaEmpresaResponseDTO>> insignias(@PathVariable String slug) {
        return ResponseEntity.ok(insigniaEmpresaConsultaService.listarPorSlug(slug));
    }

    @GetMapping("/{slug}/compartir")
    public ResponseEntity<EnlacePerfilDTO> compartir(@PathVariable String slug) {
        EnlacePerfilDTO enlace = enlacePerfilService.obtenerEnlacePerfil(slug);
        return ResponseEntity.ok(enlace);
    }

    @GetMapping("/{slug}/evolucion-huella")
    public ResponseEntity<EvolucionHuellaPublicaDTO> evolucionHuella(@PathVariable String slug) {
        return ResponseEntity.ok(evolucionService.obtenerEvolucionPorSlug(slug));
    }
}
