package com.piedpiper.carbonhub.storage;

import com.piedpiper.carbonhub.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path rootDir;

    public DocumentStorageService(@Value("${app.storage.dir}") String dir) {
        this.rootDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        try {
            Files.createDirectories(rootDir);
            String nombre = UUID.randomUUID() + "-" + sanitizar(archivo.getOriginalFilename());
            Path destino = rootDir.resolve(nombre);
            archivo.transferTo(destino);
            return destino.toString();
        } catch (IOException e) {
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar tu solicitud. Por favor, intenta nuevamente.");
        }
    }

    public void eliminar(String ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(ruta));
        } catch (IOException ignored) {
        }
    }

    private String sanitizar(String nombre) {
        if (nombre == null) {
            return "documento.pdf";
        }
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
