package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.auditor.models.enums.Especialidad;
import com.piedpiper.carbonhub.auditor.models.enums.ZonaCobertura;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    @GetMapping("/especialidades")
    public ResponseEntity<List<String>> listarEspecialidades() {
        List<String> valores = Arrays.stream(Especialidad.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(valores);
    }

    @GetMapping("/zonas-cobertura")
    public ResponseEntity<List<String>> listarZonasCobertura() {
        List<String> valores = Arrays.stream(ZonaCobertura.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(valores);
    }
}
