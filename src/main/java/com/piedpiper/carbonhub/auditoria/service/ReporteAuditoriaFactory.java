package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.ReporteAuditoria;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Component
public class ReporteAuditoriaFactory {

    public ReporteAuditoria crear(MultipartFile archivo, Instant fechaCarga) {
        return ReporteAuditoria.builder()
                .nombreArchivo(nombreArchivo(archivo))
                .tipoContenido(archivo.getContentType())
                .tamanioBytes(archivo.getSize())
                .fechaCarga(fechaCarga)
                .build();
    }

    private String nombreArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return nombre == null || nombre.isBlank() ? "reporte-auditoria.pdf" : nombre;
    }
}
