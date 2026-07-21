package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.CatalogoItemResponseDTO;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL', 'AUDITOR_CERTIFICADO')")
public class CatalogosAuditorController {

    @GetMapping("/especialidades")
    public ResponseEntity<List<CatalogoItemResponseDTO>> especialidades() {
        return ResponseEntity.ok(Arrays.stream(EspecialidadAuditor.values())
                .map(especialidad -> new CatalogoItemResponseDTO(especialidad.name(), especialidad.etiqueta()))
                .toList());
    }

    @GetMapping("/zonas")
    public ResponseEntity<List<CatalogoItemResponseDTO>> zonas() {
        return ResponseEntity.ok(Arrays.stream(ProvinciaCR.values())
                .map(zona -> new CatalogoItemResponseDTO(zona.name(), zona.etiqueta()))
                .toList());
    }
}
