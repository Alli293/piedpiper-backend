package com.piedpiper.carbonhub.reconocimiento.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoReconocimientoResponseDTO;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.RegistrarEventoReconocimientoRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconocimiento/eventos")
@PreAuthorize("hasRole('USUARIO_INDIVIDUAL')")
public class EventoReconocimientoController {

    private final EventoReconocimientoService eventoReconocimientoService;

    public EventoReconocimientoController(EventoReconocimientoService eventoReconocimientoService) {
        this.eventoReconocimientoService = eventoReconocimientoService;
    }

    @PostMapping
    public ResponseEntity<EventoReconocimientoResponseDTO> registrar(
            Authentication authentication,
            @Valid @RequestBody RegistrarEventoReconocimientoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoReconocimientoService.registrar(
                request, Autenticaciones.usuarioId(authentication)));
    }
}
