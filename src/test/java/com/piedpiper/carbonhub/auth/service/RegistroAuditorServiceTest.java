package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.service.EmailService;
import com.piedpiper.carbonhub.storage.service.DocumentStorageService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroAuditorServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditorPersistence auditorPersistence;
    @Mock
    private DocumentStorageService storage;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistroAuditorService service;

    private RegistroAuditorRequestDTO request(LocalDate vigencia) {
        return new RegistroAuditorRequestDTO("token", "Ana Perez", "CERT-123", "IEC",
                vigencia, 5, true);
    }

    private MultipartFile pdf(String nombre) {
        return new MockMultipartFile(nombre, nombre + ".pdf", "application/pdf",
                new byte[]{1, 2, 3});
    }

    @Test
    void solicitudExitosaPersisteYEnviaCorreo() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);
        when(storage.guardar(any())).thenReturn("/uploads/cert.pdf", "/uploads/id.pdf");

        service.registrar(request(LocalDate.now().plusYears(1)), pdf("cert"), pdf("id"));

        verify(auditorPersistence).persistir(any(), any(), anyString(), anyString());
        verify(emailService).enviarConfirmacionAuditor("ana@gmail.com", "Ana Perez", "CERT-123");
    }

    @Test
    void certificacionVencidaLanza422YNoGuardaArchivos() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);

        assertThatThrownBy(() ->
                service.registrar(request(LocalDate.now().minusDays(1)), pdf("cert"), pdf("id")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(storage, never()).guardar(any());
        verify(auditorPersistence, never()).persistir(any(), any(), any(), any());
    }

    @Test
    void subDuplicadoLanza409() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(true);

        assertThatThrownBy(() ->
                service.registrar(request(LocalDate.now().plusYears(1)), pdf("cert"), pdf("id")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(storage, never()).guardar(any());
    }

    @Test
    void falloAlPersistirRevierteYBorraArchivos() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);
        when(storage.guardar(any())).thenReturn("/uploads/cert.pdf", "/uploads/id.pdf");
        doThrow(new RuntimeException("db")).when(auditorPersistence)
                .persistir(any(), any(), anyString(), anyString());

        assertThatThrownBy(() ->
                service.registrar(request(LocalDate.now().plusYears(1)), pdf("cert"), pdf("id")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(storage, times(2)).eliminar(anyString());
        verify(emailService, never()).enviarConfirmacionAuditor(any(), any(), any());
    }
}
