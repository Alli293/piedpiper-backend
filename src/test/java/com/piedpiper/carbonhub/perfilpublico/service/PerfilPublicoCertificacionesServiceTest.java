package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicoCertificacionesServiceTest {

    private static final String SLUG = "empresa-verde";
    private static final UUID ID_EMPRESA = UUID.randomUUID();

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private CertificacionMapper certificacionMapper;

    // El catalogo es la instancia real: es datos de referencia, no colaborador.
    private final CatalogoTiposCertificacion catalogo = new CatalogoTiposCertificacion();

    private PerfilPublicoCertificacionesService service;

    @BeforeEach
    void prepararServicio() {
        service = new PerfilPublicoCertificacionesService(
                empresaRepository, certificacionRepository, catalogo, certificacionMapper);
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
    void listarPorSlugRetornaCertificacionesActivasCuandoLaEmpresaEsActiva() {
        Empresa empresa = Empresa.builder().id(ID_EMPRESA).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));

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

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).hasSize(1);
        CertificacionPublicaResponseDTO dto = resultado.get(0);
        assertThat(dto.getId()).isEqualTo(certificacion.getId());
        assertThat(dto.getTipo()).isEqualTo("CARBONO_NEUTRAL");
        assertThat(dto.getNombreCertificacion()).isEqualTo("Carbono Neutral");
        assertThat(dto.getFechaEmision()).isEqualTo(certificacion.getFechaEmision());
        assertThat(dto.getFechaVencimiento()).isEqualTo(certificacion.getFechaVencimiento());
        assertThat(dto.getEstado()).isEqualTo("ACTIVA");

        verify(empresaRepository).findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO);
        verify(certificacionRepository)
                .findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(ID_EMPRESA, EstadoCertificacion.ACTIVA);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaNoExiste() {
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(certificacionRepository);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaEstaInactiva() {
        // findBySlugAndEstado ya filtra por ACTIVO en la consulta: una empresa
        // INACTIVA simplemente no matchea, exactamente igual que un slug inexistente.
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(empresaRepository).findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO);
    }

    @Test
    void listarPorSlugRetornaListaVaciaCuandoLaEmpresaNoTieneCertificacionesActivas() {
        Empresa empresa = Empresa.builder().id(ID_EMPRESA).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(
                ID_EMPRESA, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).isEmpty();
    }
}
