package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre {@code listarActivasPublicasPorEmpresa} (el metodo por el que
 * {@code perfilpublico} consulta certificaciones sin depender directamente
 * del repositorio ni del mapper de este dominio) y el calculo de
 * {@code vigente}, que no se persiste y se recalcula en cada consulta
 * comparando {@code fechaVencimiento} contra hoy.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaCertificacionServiceTest {

    private static final UUID ID_USUARIO = UUID.randomUUID();
    private static final UUID ID_EMPRESA = UUID.randomUUID();
    private static final UUID ID_CERTIFICACION = UUID.randomUUID();

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CertificacionMapper certificacionMapper;
    @Mock
    private GeneradorCredencialOpenBadges generadorCredencialOpenBadges;

    // El catalogo es la instancia real: es datos de referencia, no colaborador.
    private final CatalogoTiposCertificacion catalogo = new CatalogoTiposCertificacion();

    private ConsultaCertificacionService service;

    @BeforeEach
    void prepararServicio() {
        service = new ConsultaCertificacionService(certificacionRepository, usuarioRepository, catalogo,
                certificacionMapper, generadorCredencialOpenBadges);
    }

    private Certificacion certificacion(LocalDate fechaVencimiento) {
        return Certificacion.builder()
                .id(ID_CERTIFICACION)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .fechaEmision(Instant.parse("2026-01-15T00:00:00Z"))
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoCertificacion.ACTIVA)
                .auditor(Usuario.builder().id(UUID.randomUUID()).nombre("Ana").apellidos("Mora").email("ana@auditor.cr").build())
                .build();
    }

    private void mockearUsuarioDeLaEmpresa() {
        Usuario usuario = Usuario.builder().id(ID_USUARIO)
                .empresa(Empresa.builder().id(ID_EMPRESA).build())
                .build();
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
    }

    @Test
    void listarActivasPublicasPorEmpresaConsultaSoloCertificacionesActivasYVigentesDeEsaEmpresa() {
        Certificacion certificacion = certificacion(LocalDate.of(2027, 1, 15));
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        ID_EMPRESA, EstadoCertificacion.ACTIVA, LocalDate.now()))
                .thenReturn(List.of(certificacion));
        CertificacionPublicaResponseDTO dtoMapeado = new CertificacionPublicaResponseDTO();
        dtoMapeado.setId(certificacion.getId());
        dtoMapeado.setTipo("CARBONO_NEUTRAL");
        dtoMapeado.setFechaEmision(certificacion.getFechaEmision());
        dtoMapeado.setFechaVencimiento(certificacion.getFechaVencimiento());
        dtoMapeado.setEstado("ACTIVA");
        when(certificacionMapper.toPublicaDto(certificacion)).thenReturn(dtoMapeado);

        List<CertificacionPublicaResponseDTO> resultado =
                service.listarActivasPublicasPorEmpresa(ID_EMPRESA);

        assertThat(resultado).hasSize(1);
        CertificacionPublicaResponseDTO dto = resultado.get(0);
        assertThat(dto.getId()).isEqualTo(certificacion.getId());
        assertThat(dto.getNombreCertificacion()).isEqualTo("Carbono Neutral");
    }

    @Test
    void listarActivasPublicasPorEmpresaRetornaListaVaciaSinCertificacionesActivasYVigentes() {
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        any(), any(), any()))
                .thenReturn(List.of());

        List<CertificacionPublicaResponseDTO> resultado =
                service.listarActivasPublicasPorEmpresa(ID_EMPRESA);

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarMarcaVigenteFalsoParaUnaCertificacionVencida() {
        mockearUsuarioDeLaEmpresa();
        Certificacion vencida = certificacion(LocalDate.now().minusDays(1));
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(ID_EMPRESA))
                .thenReturn(List.of(vencida));
        when(certificacionMapper.toResumenDto(vencida)).thenReturn(new CertificacionResumenResponseDTO());

        List<CertificacionResumenResponseDTO> resultado = service.listar(ID_USUARIO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isVigente()).isFalse();
    }

    @Test
    void listarMarcaVigenteVerdaderoParaUnaCertificacionNoVencida() {
        mockearUsuarioDeLaEmpresa();
        Certificacion vigente = certificacion(LocalDate.now().plusDays(1));
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(ID_EMPRESA))
                .thenReturn(List.of(vigente));
        when(certificacionMapper.toResumenDto(vigente)).thenReturn(new CertificacionResumenResponseDTO());

        List<CertificacionResumenResponseDTO> resultado = service.listar(ID_USUARIO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isVigente()).isTrue();
    }

    @Test
    void detalleMarcaVigenteFalsoElMismoDiaDeLaFechaDeVencimiento() {
        mockearUsuarioDeLaEmpresa();
        Certificacion venceHoy = certificacion(LocalDate.now());
        when(certificacionRepository.findByIdAndEmpresaId(ID_CERTIFICACION, ID_EMPRESA))
                .thenReturn(Optional.of(venceHoy));
        when(certificacionMapper.toDto(venceHoy)).thenReturn(new CertificacionResponseDTO());

        CertificacionResponseDTO resultado = service.detalle(ID_USUARIO, ID_CERTIFICACION);

        // El VC-JWT vence a medianoche UTC del dia de fechaVencimiento: ese
        // mismo dia ya no es vigente.
        assertThat(resultado.isVigente()).isFalse();
    }

    @Test
    void descargarJsonLdDevuelveElDocumentoDecodificadoDeLaEmpresaDelUsuario() {
        mockearUsuarioDeLaEmpresa();
        Certificacion certificacion = certificacion(LocalDate.of(2027, 1, 15));
        certificacion.setCredencialJwt("jwt.firmado.aqui");
        when(certificacionRepository.findByIdAndEmpresaId(ID_CERTIFICACION, ID_EMPRESA))
                .thenReturn(Optional.of(certificacion));
        Map<String, Object> documento = Map.of("id", "urn:uuid:algo");
        when(generadorCredencialOpenBadges.decodificar("jwt.firmado.aqui")).thenReturn(documento);

        Map<String, Object> resultado = service.descargarJsonLd(ID_USUARIO, ID_CERTIFICACION);

        assertThat(resultado).isEqualTo(documento);
    }

    @Test
    void descargarJsonLdDeOtraEmpresaLanzaRecursoNoEncontrado() {
        mockearUsuarioDeLaEmpresa();
        when(certificacionRepository.findByIdAndEmpresaId(ID_CERTIFICACION, ID_EMPRESA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargarJsonLd(ID_USUARIO, ID_CERTIFICACION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void verificarPublicaDevuelveElDocumentoSinImportarLaEmpresa() {
        Certificacion certificacion = certificacion(LocalDate.of(2027, 1, 15));
        certificacion.setCredencialJwt("jwt.firmado.aqui");
        when(certificacionRepository.findById(ID_CERTIFICACION)).thenReturn(Optional.of(certificacion));
        Map<String, Object> documento = Map.of("id", "urn:uuid:algo");
        when(generadorCredencialOpenBadges.decodificar("jwt.firmado.aqui")).thenReturn(documento);

        Map<String, Object> resultado = service.verificarPublica(ID_CERTIFICACION);

        assertThat(resultado).isEqualTo(documento);
    }

    @Test
    void verificarPublicaDeUnIdInexistenteLanzaRecursoNoEncontrado() {
        when(certificacionRepository.findById(ID_CERTIFICACION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificarPublica(ID_CERTIFICACION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void verificacionJwtDevuelveElJwtFirmadoSinDecodificar() {
        Certificacion certificacion = certificacion(LocalDate.of(2027, 1, 15));
        certificacion.setCredencialJwt("cabecera.payload.firma");
        when(certificacionRepository.findById(ID_CERTIFICACION)).thenReturn(Optional.of(certificacion));

        String resultado = service.verificacionJwt(ID_CERTIFICACION);

        assertThat(resultado).isEqualTo("cabecera.payload.firma");
        org.mockito.Mockito.verifyNoInteractions(generadorCredencialOpenBadges);
    }

    @Test
    void verificacionJwtDeUnIdInexistenteLanzaRecursoNoEncontrado() {
        when(certificacionRepository.findById(ID_CERTIFICACION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificacionJwt(ID_CERTIFICACION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
