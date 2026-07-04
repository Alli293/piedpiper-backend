package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.RegistroAuditorRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.notification.EmailService;
import com.piedpiper.carbonhub.storage.DocumentStorageService;
import com.piedpiper.carbonhub.user.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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

    public String registrar(RegistroAuditorRequest request,
                            MultipartFile docCertificado,
                            MultipartFile docIdentificacion) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.idToken());

        if (usuarioRepository.existsByGoogleSub(claims.sub())
                || usuarioRepository.existsByEmail(claims.email())) {
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
                claims.email(), request.nombreCompleto(), request.numeroCertificacion());
        return "Tu solicitud fue recibida y está en revisión. "
                + "Te notificaremos por correo cuando sea procesada.";
    }

    private void validarCampos(RegistroAuditorRequest request) {
        if (!request.aceptaTerminos()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Debes aceptar los Términos y Condiciones y la Política de Privacidad.");
        }
        String nombre = request.nombreCompleto();
        if (nombre == null || nombre.length() < 2 || nombre.length() > 100
                || !NOMBRE.matcher(nombre).matches()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El nombre solo puede contener letras, espacios y guiones.");
        }
        String cert = request.numeroCertificacion();
        if (cert == null || cert.isEmpty() || cert.length() > 50
                || !CERTIFICACION.matcher(cert).matches()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El número de certificación solo puede contener letras, números y guiones.");
        }
        String entidad = request.entidadCertificadora();
        if (entidad == null || entidad.isBlank() || entidad.length() > 100) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Selecciona una entidad certificadora válida.");
        }
        Integer experiencia = request.aniosExperiencia();
        if (experiencia == null || experiencia < 0 || experiencia > 60) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ingresa un número entre 0 y 60.");
        }
        if (request.fechaVigenciaCert() == null
                || request.fechaVigenciaCert().isBefore(LocalDate.now())) {
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
