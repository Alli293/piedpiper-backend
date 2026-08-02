package com.piedpiper.carbonhub.insignia.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/insignias")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class InsigniaEmpresaController {

    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    public InsigniaEmpresaController(InsigniaEmpresaConsultaService insigniaEmpresaConsultaService) {
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
    }

    @GetMapping
    public ResponseEntity<List<InsigniaEmpresaResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(insigniaEmpresaConsultaService.listarParaEmpresaAutenticada(
                Autenticaciones.usuarioId(authentication)));
    }
}
