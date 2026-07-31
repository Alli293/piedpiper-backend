package com.piedpiper.carbonhub.dashboard.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.DashboardCertificacionesService;
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
    private final DashboardCertificacionesService dashboardCertificacionesService;

    public DashboardController(
            DashboardHuellaService dashboardHuellaService,
            DashboardCertificacionesService dashboardCertificacionesService) {
        this.dashboardHuellaService = dashboardHuellaService;
        this.dashboardCertificacionesService = dashboardCertificacionesService;
    }

    @GetMapping("/huella")
    public ResponseEntity<ResumenHuellaDashboardResponseDTO> obtenerHuella(
            Authentication authentication,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(dashboardHuellaService.obtenerResumen(
                Autenticaciones.usuarioId(authentication),
                periodo,
                anio));
    }

    @GetMapping("/certificaciones")
    public ResponseEntity<ResumenCertificacionesDashboardResponseDTO> obtenerCertificaciones(
            Authentication authentication) {
        return ResponseEntity.ok(dashboardCertificacionesService.obtenerResumen(
                Autenticaciones.usuarioId(authentication)));
    }
}
