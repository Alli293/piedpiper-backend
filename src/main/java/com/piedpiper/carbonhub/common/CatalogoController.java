package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    @GetMapping("/especialidades")
    public ResponseEntity<List<CatalogoItemDTO>> listarEspecialidades() {
        List<CatalogoItemDTO> items = Arrays.stream(EspecialidadAuditor.values())
                .map(e -> new CatalogoItemDTO(e.name(), CatalogoItemDTO.etiquetaDesdeEnum(e.name())))
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/zonas")
    public ResponseEntity<List<CatalogoItemDTO>> listarZonasCobertura() {
        List<CatalogoItemDTO> items = Arrays.stream(ProvinciaCR.values())
                .map(e -> new CatalogoItemDTO(e.name(), CatalogoItemDTO.etiquetaDesdeEnum(e.name())))
                .toList();
        return ResponseEntity.ok(items);
    }
}
