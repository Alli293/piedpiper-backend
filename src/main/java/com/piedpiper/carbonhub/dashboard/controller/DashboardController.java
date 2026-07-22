package com.piedpiper.carbonhub.dashboard.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class DashboardController {

    private final DashboardHuellaService dashboardHuellaService;

    public DashboardController(DashboardHuellaService dashboardHuellaService) {
        this.dashboardHuellaService = dashboardHuellaService;
    }

    @GetMapping("/huella")
    public ResponseEntity<ResumenHuellaDashboardResponseDTO> obtenerHuella(
            Authentication authentication,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(dashboardHuellaService.obtenerResumen(
                Autenticaciones.usuarioId(authentication),
                periodo));
    }
}
