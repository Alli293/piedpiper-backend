package com.piedpiper.carbonhub.calificacion.controller;

import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.EditarCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.calificacion.service.CalificacionCreacionService;
import com.piedpiper.carbonhub.calificacion.service.CalificacionEdicionService;
import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/calificaciones")
@PreAuthorize("hasRole('ADMINISTRADOR_EMPRESA')")
public class CalificacionController {

    private final CalificacionCreacionService calificacionCreacionService;
    private final CalificacionEdicionService calificacionEdicionService;
    private final CalificacionRepository calificacionRepository;
    private final CalificacionMapper calificacionMapper;
    private final UsuarioRepository usuarioRepository;

    public CalificacionController(CalificacionCreacionService calificacionCreacionService,
                                  CalificacionEdicionService calificacionEdicionService,
                                  CalificacionRepository calificacionRepository,
                                  CalificacionMapper calificacionMapper,
                                  UsuarioRepository usuarioRepository) {
        this.calificacionCreacionService = calificacionCreacionService;
        this.calificacionEdicionService = calificacionEdicionService;
        this.calificacionRepository = calificacionRepository;
        this.calificacionMapper = calificacionMapper;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<CalificacionResponseDTO> crear(
            @Valid @RequestBody CrearCalificacionRequestDTO request,
            Authentication authentication) {
        CalificacionResponseDTO response = calificacionCreacionService.crear(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{calificacionId}")
    public ResponseEntity<CalificacionResponseDTO> editar(
            @PathVariable UUID calificacionId,
            @Valid @RequestBody EditarCalificacionRequestDTO request,
            Authentication authentication) {
        CalificacionResponseDTO response = calificacionEdicionService.editar(calificacionId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auditoria/{auditoriaId}")
    public ResponseEntity<CalificacionResponseDTO> obtenerPorAuditoria(
            @PathVariable UUID auditoriaId,
            Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado("No tiene permiso."));
        UUID empresaId = usuario.getEmpresa().getId();
        return calificacionRepository.findByAuditoriaIdAndEmpresaId(auditoriaId, empresaId)
                .map(calificacionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No se encontró una calificación para esta auditoría."));
    }
}
