package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoRespaldoDescargaServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID DOCUMENTO_ID = UUID.fromString("0b1c2d3e-4f50-4162-8374-859607182930");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID ADMIN_EMPRESA_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID AJENO_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private DocumentoRespaldoDescargaService service;

    @BeforeEach
    void configurar() {
        service = new DocumentoRespaldoDescargaService(
                solicitudAuditoriaRepository,
                usuarioRepository,
                new AccesoSolicitudAuditoria(transicionEstadoAuditoriaRepository));

        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud()));
        when(transicionEstadoAuditoriaRepository.idsAuditoresConHistorial(SOLICITUD_ID))
                .thenReturn(List.of());
        when(usuarioRepository.findById(ADMIN_EMPRESA_ID)).thenReturn(Optional.of(
                usuario(ADMIN_EMPRESA_ID, Rol.ADMINISTRADOR_EMPRESA, EMPRESA_ID)));
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(
                usuario(AUDITOR_ID, Rol.AUDITOR_CERTIFICADO, null)));
        when(usuarioRepository.findById(AJENO_ID)).thenReturn(Optional.of(
                usuario(AJENO_ID, Rol.ADMINISTRADOR_EMPRESA,
                        UUID.fromString("11111111-2222-3333-4444-555555555555"))));
    }

    @Test
    void laEmpresaDuenaDescargaSuDocumento() {
        DocumentoRespaldo documento = service.obtener(SOLICITUD_ID, DOCUMENTO_ID, ADMIN_EMPRESA_ID);

        assertThat(documento.getNombreArchivo()).isEqualTo("inventario-2025.pdf");
        assertThat(documento.getContenido()).isNotEmpty();
    }

    @Test
    void elAuditorAsignadoDescargaElDocumentoParaRevisarlo() {
        assertThat(service.obtener(SOLICITUD_ID, DOCUMENTO_ID, AUDITOR_ID)).isNotNull();
    }

    @Test
    void unUsuarioSinRelacionConLaSolicitudRecibe403() {
        assertThatThrownBy(() -> service.obtener(SOLICITUD_ID, DOCUMENTO_ID, AJENO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * El documento se busca dentro de la solicitud y no por su id suelto: si se consultara directo
     * por id, un identificador de otra solicitud pasaria la validacion de acceso de la solicitud
     * indicada y devolveria un documento que no le pertenece.
     */
    @Test
    void unDocumentoQueNoPerteneceALaSolicitudDevuelve404() {
        assertThatThrownBy(() -> service.obtener(SOLICITUD_ID, UUID.randomUUID(), ADMIN_EMPRESA_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unaSolicitudInexistenteDevuelve404() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(SOLICITUD_ID, DOCUMENTO_ID, ADMIN_EMPRESA_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static SolicitudAuditoria solicitud() {
        SolicitudAuditoria solicitud = SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .auditor(Usuario.builder().id(AUDITOR_ID).nombre("Ana").build())
                .documentos(new ArrayList<>())
                .build();
        solicitud.agregarDocumento(DocumentoRespaldo.builder()
                .id(DOCUMENTO_ID)
                .nombreArchivo("inventario-2025.pdf")
                .tipoContenido("application/pdf")
                .contenido("%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8))
                .build());
        return solicitud;
    }

    private static Usuario usuario(UUID id, Rol rol, UUID empresaId) {
        return Usuario.builder()
                .id(id)
                .rol(rol)
                .empresa(empresaId == null ? null : Empresa.builder().id(empresaId).build())
                .build();
    }
}
