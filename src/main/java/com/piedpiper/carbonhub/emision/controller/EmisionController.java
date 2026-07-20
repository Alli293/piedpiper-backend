package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionElectricidadResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarElectricidadRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarEnvioRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarFlotaRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarVueloRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionConsultaService;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;
import com.piedpiper.carbonhub.emision.service.EmisionEnvioService;
import com.piedpiper.carbonhub.emision.service.EmisionFlotaService;
import com.piedpiper.carbonhub.emision.service.EmisionVueloService;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emisiones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class EmisionController {

    private final EmisionElectricidadService emisionElectricidadService;
    private final EmisionFlotaService emisionFlotaService;
    private final EmisionEnvioService emisionEnvioService;
    private final EmisionVueloService emisionVueloService;
    private final EmisionConsultaService emisionConsultaService;

    public EmisionController(EmisionElectricidadService emisionElectricidadService,
                             EmisionFlotaService emisionFlotaService,
                             EmisionEnvioService emisionEnvioService,
                             EmisionVueloService emisionVueloService,
                             EmisionConsultaService emisionConsultaService) {
        this.emisionElectricidadService = emisionElectricidadService;
        this.emisionFlotaService = emisionFlotaService;
        this.emisionEnvioService = emisionEnvioService;
        this.emisionVueloService = emisionVueloService;
        this.emisionConsultaService = emisionConsultaService;
    }

    @GetMapping
    public ResponseEntity<List<EmisionResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(emisionConsultaService.listar(Autenticaciones.usuarioId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmisionResponseDTO> obtener(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(emisionConsultaService.obtener(id, Autenticaciones.usuarioId(authentication)));
    }

    @PostMapping("/electricidad")
    public ResponseEntity<EmisionElectricidadResponseDTO> registrarElectricidad(
            Authentication authentication,
            @Valid @RequestBody RegistrarElectricidadRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        EmisionElectricidadResponseDTO response = emisionElectricidadService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/flota/tipos-vehiculo")
    public ResponseEntity<List<TipoVehiculoResponseDTO>> listarTiposVehiculo() {
        return ResponseEntity.ok(emisionFlotaService.listarTiposVehiculo());
    }

    @PostMapping("/flota")
    public ResponseEntity<EmisionFlotaResponseDTO> registrarFlota(
            Authentication authentication,
            @Valid @RequestBody RegistrarFlotaRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        EmisionFlotaResponseDTO response = emisionFlotaService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/envio")
    public ResponseEntity<EmisionEnvioResponseDTO> registrarEnvio(
            Authentication authentication,
            @Valid @RequestBody RegistrarEnvioRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        EmisionEnvioResponseDTO response = emisionEnvioService.registrar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/vuelo")
    public ResponseEntity<EmisionResponseDTO> registrarVuelo(
            Authentication authentication,
            @Valid @RequestBody RegistrarVueloRequestDTO request) {
        EmisionResponseDTO response = emisionVueloService.registrar(request, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/vuelo/{id}")
    public ResponseEntity<EmisionResponseDTO> actualizarVuelo(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarVueloRequestDTO request) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(emisionVueloService.actualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID id) {
        emisionConsultaService.eliminar(id, Autenticaciones.usuarioId(authentication));
        return ResponseEntity.noContent().build();
    }
}
