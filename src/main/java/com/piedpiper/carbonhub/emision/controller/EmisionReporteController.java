package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionComparacionService;
import com.piedpiper.carbonhub.emision.service.EmisionEvolucionService;
import com.piedpiper.carbonhub.emision.service.EmisionResumenService;
import com.piedpiper.carbonhub.emision.service.ReporteHuellaPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes y agregados sobre las emisiones: resumen, comparación, evolución y PDF. */
@RestController
@RequestMapping("/api/emisiones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class EmisionReporteController {

    private final EmisionResumenService emisionResumenService;
    private final EmisionComparacionService emisionComparacionService;
    private final EmisionEvolucionService emisionEvolucionService;
    private final ReporteHuellaPdfService reporteHuellaPdfService;

    public EmisionReporteController(EmisionResumenService emisionResumenService,
                                    EmisionComparacionService emisionComparacionService,
                                    EmisionEvolucionService emisionEvolucionService,
                                    ReporteHuellaPdfService reporteHuellaPdfService) {
        this.emisionResumenService = emisionResumenService;
        this.emisionComparacionService = emisionComparacionService;
        this.emisionEvolucionService = emisionEvolucionService;
        this.reporteHuellaPdfService = reporteHuellaPdfService;
    }

    @GetMapping("/comparacion")
    public ResponseEntity<ComparacionEmisionesResponseDTO> comparar(
            Authentication authentication,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(emisionComparacionService.comparar(
                Autenticaciones.usuarioId(authentication),
                anio));
    }

    @GetMapping("/reporte/pdf")
    public ResponseEntity<byte[]> exportarReportePdf(
            Authentication authentication,
            @RequestParam Integer anio,
            @RequestParam(required = false) Integer mes) {
        byte[] pdf = reporteHuellaPdfService.generar(Autenticaciones.usuarioId(authentication), anio, mes);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(reporteHuellaPdfService.nombreArchivo(anio, mes))
                        .build()
                        .toString())
                .body(pdf);
    }

    @GetMapping("/resumen")
    public ResponseEntity<EmisionResumenResponseDTO> resumen(
            @RequestParam Integer anio,
            @RequestParam(required = false) Integer mes,
            Authentication authentication) {
        return ResponseEntity.ok(
                emisionResumenService.resumen(anio, mes, Autenticaciones.usuarioId(authentication)));
    }

    @GetMapping("/evolucion")
    public ResponseEntity<EvolucionMensualResponseDTO> evolucion(
            Authentication authentication,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(emisionEvolucionService.obtenerEvolucion(anio, Autenticaciones.usuarioId(authentication)));
    }
}
