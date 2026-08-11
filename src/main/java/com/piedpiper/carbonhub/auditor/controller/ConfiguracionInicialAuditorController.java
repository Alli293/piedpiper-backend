package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.CompletarConfiguracionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.service.ConfiguracionInicialAuditorService;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.common.Autenticaciones;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auditor/configuracion-inicial")
@PreAuthorize("hasRole('AUDITOR_CERTIFICADO')")
public class ConfiguracionInicialAuditorController {

    private final ConfiguracionInicialAuditorService configuracionInicialAuditorService;

    public ConfiguracionInicialAuditorController(
            ConfiguracionInicialAuditorService configuracionInicialAuditorService) {
        this.configuracionInicialAuditorService = configuracionInicialAuditorService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensajeResponseDTO> completar(
            @RequestPart("datos") @Valid CompletarConfiguracionAuditorRequestDTO datos,
            @RequestPart("documentos") List<MultipartFile> documentos,
            Authentication authentication) {
        MensajeResponseDTO respuesta = configuracionInicialAuditorService.completar(
                Autenticaciones.usuarioId(authentication), datos, documentos);
        return ResponseEntity.ok(respuesta);
    }
}
