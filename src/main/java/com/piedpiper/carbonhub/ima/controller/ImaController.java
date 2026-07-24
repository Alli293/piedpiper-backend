package com.piedpiper.carbonhub.ima.controller;

import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.BenchmarkSectorialResponseDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.service.ImaBenchmarkService;
import com.piedpiper.carbonhub.ima.service.ImaService;
import com.piedpiper.carbonhub.ima.service.ImaTendenciaService;

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
    private final ImaTendenciaService imaTendenciaService;
    private final ImaBenchmarkService imaBenchmarkService;

    public ImaController(ImaService imaService, ImaTendenciaService imaTendenciaService,
                          ImaBenchmarkService imaBenchmarkService) {
        this.imaService = imaService;
        this.imaTendenciaService = imaTendenciaService;
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
            throw ApiException.periodoImaInvalido(
                    "El año debe estar entre 2000 y " + hoy.getYear() + ".");
        }
        if (mes < 1 || mes > 12) {
            throw ApiException.periodoImaInvalido("El mes debe estar entre 1 y 12.");
        }

        LocalDate periodoSolicitado = LocalDate.of(anio, mes, 1);
        LocalDate periodoActual = LocalDate.of(hoy.getYear(), hoy.getMonthValue(), 1);
        if (periodoSolicitado.isAfter(periodoActual)) {
            throw ApiException.periodoImaInvalido("El período no puede ser futuro.");
        }

        return new Periodo(anio, mes);
    }

    private record Periodo(int anio, int mes) {
    }

    @GetMapping("/tendencia")
    public ResponseEntity<ImaTendenciaResponseDTO> obtenerTendencia(
            Authentication authentication,
            @RequestParam(required = false) Integer mesesAtras) {

        if (mesesAtras != null
                && (mesesAtras < 1 || mesesAtras > ImaTendenciaService.MESES_VENTANA_MAXIMA)) {
            throw ApiException.periodoImaInvalido(
                    "La ventana debe estar entre 1 y " + ImaTendenciaService.MESES_VENTANA_MAXIMA + " meses.");
        }

        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        return ResponseEntity.ok(imaTendenciaService.obtenerTendencia(mesesAtras, usuarioId));
    }
}
