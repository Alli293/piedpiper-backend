package com.piedpiper.carbonhub.dashboard.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.dashboard.models.dtos.AlertaVencimientoResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.CalendarioVencimientosResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionRenovacionResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.CalendarioVencimientosService;
import com.piedpiper.carbonhub.dashboard.service.DashboardAlertasService;
import com.piedpiper.carbonhub.dashboard.service.DashboardCertificacionesService;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import com.piedpiper.carbonhub.dashboard.service.DashboardRecomendacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class DashboardController {

    private final DashboardHuellaService dashboardHuellaService;
    private final DashboardCertificacionesService dashboardCertificacionesService;
    private final CalendarioVencimientosService calendarioVencimientosService;
    private final DashboardAlertasService dashboardAlertasService;
    private final DashboardRecomendacionService dashboardRecomendacionService;

    public DashboardController(
            DashboardHuellaService dashboardHuellaService,
            DashboardCertificacionesService dashboardCertificacionesService,
            CalendarioVencimientosService calendarioVencimientosService,
            DashboardAlertasService dashboardAlertasService,
            DashboardRecomendacionService dashboardRecomendacionService) {
        this.dashboardHuellaService = dashboardHuellaService;
        this.dashboardCertificacionesService = dashboardCertificacionesService;
        this.calendarioVencimientosService = calendarioVencimientosService;
        this.dashboardAlertasService = dashboardAlertasService;
        this.dashboardRecomendacionService = dashboardRecomendacionService;
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

    @GetMapping("/calendario")
    public ResponseEntity<CalendarioVencimientosResponseDTO> obtenerCalendario(
            Authentication authentication,
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(calendarioVencimientosService.obtenerCalendario(
                Autenticaciones.usuarioId(authentication), mes));
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaVencimientoResponseDTO>> obtenerAlertas(Authentication authentication) {
        return ResponseEntity.ok(dashboardAlertasService.obtenerAlertas(
                Autenticaciones.usuarioId(authentication)));
    }

    @GetMapping("/recomendacion")
    public ResponseEntity<RecomendacionRenovacionResponseDTO> obtenerRecomendacion(
            Authentication authentication) {
        return dashboardRecomendacionService
                .obtenerRecomendacion(Autenticaciones.usuarioId(authentication))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}