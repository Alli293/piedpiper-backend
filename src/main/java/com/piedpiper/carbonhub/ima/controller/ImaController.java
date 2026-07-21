package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.BenchmarkSectorialResponseDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.service.ImaBenchmarkService;
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
    private final ImaBenchmarkService imaBenchmarkService;

    public ImaController(ImaService imaService, ImaBenchmarkService imaBenchmarkService) {
        this.imaService = imaService;
        this.imaBenchmarkService = imaBenchmarkService;
    }

    @GetMapping
    public ResponseEntity<ImaResponseDTO> obtenerIma(
            Authentication authentication,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {

        Periodo periodo = resolverPeriodo(anio, mes);
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        ImaResponseDTO response = imaService.obtenerIma(periodo.anio(), periodo.mes(), usuarioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/benchmark")
    public ResponseEntity<BenchmarkSectorialResponseDTO> obtenerBenchmark(
            Authentication authentication,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {

        Periodo periodo = resolverPeriodo(anio, mes);
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        BenchmarkSectorialResponseDTO response =
                imaBenchmarkService.obtenerBenchmark(periodo.anio(), periodo.mes(), usuarioId);
        return ResponseEntity.ok(response);
    }

    private Periodo resolverPeriodo(Integer anio, Integer mes) {
        LocalDate hoy = LocalDate.now();

        if (anio == null) {
            anio = hoy.getYear();
        }
        if (mes == null) {
            mes = hoy.getMonthValue();
        }

        if (anio < 2000 || anio > hoy.getYear()) {
            throw ApiException.anioFueraDeRango(hoy.getYear());
        }
        if (mes < 1 || mes > 12) {
            throw ApiException.mesInvalido();
        }

        LocalDate periodoSolicitado = LocalDate.of(anio, mes, 1);
        LocalDate periodoActual = LocalDate.of(hoy.getYear(), hoy.getMonthValue(), 1);
        if (periodoSolicitado.isAfter(periodoActual)) {
            throw ApiException.periodoFuturo();
        }

        return new Periodo(anio, mes);
    }

    private record Periodo(int anio, int mes) {
    }
}
