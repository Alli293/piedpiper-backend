package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.service.EmailService;
import com.piedpiper.carbonhub.storage.service.DocumentStorageService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

@Service
public class RegistroAuditorService {

    private static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    private static final Pattern NOMBRE = Pattern.compile("^[\\p{L} '-]+$");
    private static final Pattern CERTIFICACION = Pattern.compile("^[\\p{L}\\p{N}-]+$");

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final AuditorPersistence auditorPersistence;
    private final DocumentStorageService storage;
    private final EmailService emailService;

    public RegistroAuditorService(GoogleTokenVerifier googleTokenVerifier,
                                  UsuarioRepository usuarioRepository,
                                  AuditorPersistence auditorPersistence,
                                  DocumentStorageService storage,
                                  EmailService emailService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.auditorPersistence = auditorPersistence;
        this.storage = storage;
        this.emailService = emailService;
    }

    public String registrar(RegistroAuditorRequestDTO request,
                            MultipartFile docCertificado,
                            MultipartFile docIdentificacion) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());

        if (usuarioRepository.existsByGoogleSub(claims.getSub())
                || usuarioRepository.existsByEmail(claims.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        }
        validarCampos(request);
        validarPdf(docCertificado);
        validarPdf(docIdentificacion);

        String certificadoPath = storage.guardar(docCertificado);
        String identificacionPath = storage.guardar(docIdentificacion);
        try {
            auditorPersistence.persistir(request, claims, certificadoPath, identificacionPath);
        } catch (RuntimeException e) {
            storage.eliminar(certificadoPath);
            storage.eliminar(identificacionPath);
            if (e instanceof ApiException) {
                throw e;
            }
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar tu solicitud. Por favor, intenta nuevamente.");
        }

        emailService.enviarConfirmacionAuditor(
                claims.getEmail(), request.getNombreCompleto(), request.getNumeroCertificacion());
        return "Tu solicitud fue recibida y está en revisión. "
                + "Te notificaremos por correo cuando sea procesada.";
    }

    private void validarCampos(RegistroAuditorRequestDTO request) {
        if (!request.isAceptaTerminos()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Debes aceptar los Términos y Condiciones y la Política de Privacidad.");
        }
        String nombre = request.getNombreCompleto();
        if (nombre == null || nombre.length() < 2 || nombre.length() > 100
                || !NOMBRE.matcher(nombre).matches()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El nombre solo puede contener letras, espacios y guiones.");
        }
        String cert = request.getNumeroCertificacion();
        if (cert == null || cert.isEmpty() || cert.length() > 50
                || !CERTIFICACION.matcher(cert).matches()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El número de certificación solo puede contener letras, números y guiones.");
        }
        String entidad = request.getEntidadCertificadora();
        if (entidad == null || entidad.isBlank() || entidad.length() > 100) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Selecciona una entidad certificadora válida.");
        }
        Integer experiencia = request.getAniosExperiencia();
        if (experiencia == null || experiencia < 0 || experiencia > 60) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ingresa un número entre 0 y 60.");
        }
        if (request.getFechaVigenciaCert() == null
                || request.getFechaVigenciaCert().isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La certificación está vencida. Solo se aceptan certificaciones vigentes.");
        }
    }

    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Adjunta los documentos requeridos en formato PDF.");
        }
        if (!"application/pdf".equalsIgnoreCase(archivo.getContentType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solo se aceptan archivos en formato PDF.");
        }
        if (archivo.getSize() > MAX_PDF_BYTES) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El archivo no puede superar 10 MB.");
        }
    }
}
