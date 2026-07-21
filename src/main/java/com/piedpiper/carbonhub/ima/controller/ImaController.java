package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.service.ImaService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/ima")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class ImaController {

    private final ImaService imaService;

    public ImaController(ImaService imaService) {
        this.imaService = imaService;
    }

    @GetMapping
    public ResponseEntity<ImaResponseDTO> obtenerIma(
            Authentication authentication,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {

        LocalDate hoy = LocalDate.now();

        if (anio == null) {
            anio = hoy.getYear();
        }
        if (mes == null) {
            mes = hoy.getMonthValue();
        }

        if (anio < 2000 || anio > hoy.getYear()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El año debe estar entre 2000 y " + hoy.getYear() + ".");
        }
        if (mes < 1 || mes > 12) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El mes debe estar entre 1 y 12.");
        }

        LocalDate periodoSolicitado = LocalDate.of(anio, mes, 1);
        LocalDate periodoActual = LocalDate.of(hoy.getYear(), hoy.getMonthValue(), 1);
        if (periodoSolicitado.isAfter(periodoActual)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El período no puede ser futuro.");
        }

        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ImaResponseDTO response = imaService.obtenerIma(anio, mes, usuarioId);
        return ResponseEntity.ok(response);
    }
}
