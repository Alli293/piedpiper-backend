package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.FiltrosDirectorioDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.service.DirectorioAuditoresService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/auditores")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL', 'AUDITOR_CERTIFICADO')")
public class AuditorController {

    private final DirectorioAuditoresService directorioAuditoresService;

    public AuditorController(DirectorioAuditoresService directorioAuditoresService) {
        this.directorioAuditoresService = directorioAuditoresService;
    }

    @GetMapping
    public ResponseEntity<PaginaAuditoresResponseDTO> listar(
            @RequestParam(required = false) String terminoBusqueda,
            @RequestParam(required = false) List<String> especialidades,
            @RequestParam(required = false) String zonaGeografica,
            @RequestParam(required = false) BigDecimal calificacionMinima,
            @RequestParam(required = false) Boolean soloDisponibles,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) Integer tamanioPagina,
            @RequestParam(required = false) String ordenamiento) {
        FiltrosDirectorioDTO filtros = new FiltrosDirectorioDTO(
                terminoBusqueda, especialidades, zonaGeografica, calificacionMinima,
                soloDisponibles, pagina, tamanioPagina, ordenamiento);
        return ResponseEntity.ok(directorioAuditoresService.listar(filtros));
    }
}
