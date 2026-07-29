package com.piedpiper.carbonhub.dashboard.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
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
    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    public DashboardController(DashboardHuellaService dashboardHuellaService,
                               InsigniaEmpresaConsultaService insigniaEmpresaConsultaService) {
        this.dashboardHuellaService = dashboardHuellaService;
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
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

    @GetMapping("/insignias")
    public ResponseEntity<List<InsigniaEmpresaResponseDTO>> listarInsignias(
            Authentication authentication) {
        return ResponseEntity.ok(insigniaEmpresaConsultaService.listarParaDashboard(
                Autenticaciones.usuarioId(authentication)));
    }
}
