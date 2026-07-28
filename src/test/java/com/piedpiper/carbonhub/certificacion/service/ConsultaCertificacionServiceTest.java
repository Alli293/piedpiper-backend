package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre {@code listarActivasPublicasPorEmpresa}: el metodo por el que
 * {@code perfilpublico} consulta certificaciones sin depender directamente
 * del repositorio ni del mapper de este dominio.
 */
@ExtendWith(MockitoExtension.class)
class ConsultaCertificacionServiceTest {

    private static final UUID ID_EMPRESA = UUID.randomUUID();

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CertificacionMapper certificacionMapper;

    // El catalogo es la instancia real: es datos de referencia, no colaborador.
    private final CatalogoTiposCertificacion catalogo = new CatalogoTiposCertificacion();

    private ConsultaCertificacionService service;

    @BeforeEach
    void prepararServicio() {
        service = new ConsultaCertificacionService(
                certificacionRepository, usuarioRepository, catalogo, certificacionMapper);
    }

    private Certificacion certificacion() {
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .fechaEmision(Instant.parse("2026-01-15T00:00:00Z"))
                .fechaVencimiento(LocalDate.of(2027, 1, 15))
                .estado(EstadoCertificacion.ACTIVA)
                .build();
    }

    @Test
    void listarActivasPublicasPorEmpresaConsultaSoloCertificacionesActivasDeEsaEmpresa() {
        Certificacion certificacion = certificacion();
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(
                ID_EMPRESA, EstadoCertificacion.ACTIVA))
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
    void listarActivasPublicasPorEmpresaRetornaListaVaciaSinCertificacionesActivas() {
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(
                any(), any())).thenReturn(List.of());

        List<CertificacionPublicaResponseDTO> resultado =
                service.listarActivasPublicasPorEmpresa(ID_EMPRESA);

        assertThat(resultado).isEmpty();
    }
}
