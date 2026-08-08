package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.dtos.AsignarAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.CrearSolicitudAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.service.CargaReporteAuditoriaService;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaDetalleService;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaListadoService;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class SolicitudAuditoriaController {

    private final SolicitudAuditoriaService solicitudAuditoriaService;
    private final SolicitudAuditoriaDetalleService solicitudAuditoriaDetalleService;
    private final SolicitudAuditoriaListadoService solicitudAuditoriaListadoService;
    private final CargaReporteAuditoriaService cargaReporteAuditoriaService;

    public SolicitudAuditoriaController(SolicitudAuditoriaService solicitudAuditoriaService,
                                        SolicitudAuditoriaDetalleService solicitudAuditoriaDetalleService,
                                        SolicitudAuditoriaListadoService solicitudAuditoriaListadoService,
                                        CargaReporteAuditoriaService cargaReporteAuditoriaService) {
        this.solicitudAuditoriaService = solicitudAuditoriaService;
        this.solicitudAuditoriaDetalleService = solicitudAuditoriaDetalleService;
        this.solicitudAuditoriaListadoService = solicitudAuditoriaListadoService;
        this.cargaReporteAuditoriaService = cargaReporteAuditoriaService;
    }

    /**
     * Listado de las solicitudes de la empresa autenticada. No recibe el id de la empresa por
     * parametro: sale del usuario, asi que no hay forma de pedir el listado de otra.
     */
    @GetMapping
    public ResponseEntity<List<SolicitudAuditoriaResumenResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(solicitudAuditoriaListadoService.listarDeMiEmpresa(
                Autenticaciones.usuarioId(authentication)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitudAuditoriaResponseDTO> crear(
            @RequestPart("datos") @Valid CrearSolicitudAuditoriaRequestDTO datos,
            @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos,
            Authentication authentication) {
        SolicitudAuditoriaResponseDTO respuesta = solicitudAuditoriaService.crear(
                datos, documentos, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * El rol se amplia respecto del resto del controlador porque el detalle no es una gestion de la
     * empresa sino una consulta de seguimiento: el auditor asignado tiene que poder verlo, y el
     * administrador de plataforma tiene que poder auditarlo. Quien puede ver cual solicitud lo
     * decide el servicio; el rol solo dice quien puede llegar a preguntar.
     */
    @GetMapping("/{idSolicitud}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'AUDITOR_CERTIFICADO', 'ADMINISTRADOR_PLATAFORMA')")
    public ResponseEntity<SolicitudAuditoriaDetalleResponseDTO> obtener(
            @PathVariable UUID idSolicitud,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudAuditoriaDetalleService.obtenerDetalle(
                idSolicitud, Autenticaciones.usuarioId(authentication)));
    }

    @PostMapping("/{idSolicitud}/auditor")
    public ResponseEntity<SolicitudAuditoriaResponseDTO> asignarAuditor(
            @PathVariable UUID idSolicitud,
            @Valid @RequestBody AsignarAuditorRequestDTO datos,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudAuditoriaService.asignarAuditor(
                idSolicitud, datos, Autenticaciones.usuarioId(authentication)));
    }

    @PostMapping(value = "/{idSolicitud}/reporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUDITOR_CERTIFICADO')")
    public ResponseEntity<SolicitudAuditoriaDetalleResponseDTO> cargarReporte(
            @PathVariable UUID idSolicitud,
            @RequestPart("reporteAuditoria") MultipartFile reporteAuditoria,
            @RequestParam("fechaAuditoriaRealizada")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaAuditoriaRealizada,
            Authentication authentication) {
        return ResponseEntity.ok(cargaReporteAuditoriaService.cargar(
                idSolicitud,
                reporteAuditoria,
                fechaAuditoriaRealizada,
                Autenticaciones.usuarioId(authentication)));
    }
}
