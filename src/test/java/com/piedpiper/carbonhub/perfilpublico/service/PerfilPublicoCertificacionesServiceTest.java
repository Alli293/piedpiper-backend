package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
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
    private ConsultaCertificacionService consultaCertificacionService;

    private PerfilPublicoCertificacionesService service;

    @BeforeEach
    void prepararServicio() {
        service = new PerfilPublicoCertificacionesService(
                empresaRepository, consultaCertificacionService);
    }

    private CertificacionPublicaResponseDTO certificacionPublica() {
        CertificacionPublicaResponseDTO dto = new CertificacionPublicaResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setTipo("CARBONO_NEUTRAL");
        dto.setNombreCertificacion("Carbono Neutral");
        dto.setFechaEmision(Instant.parse("2026-01-15T00:00:00Z"));
        dto.setFechaVencimiento(LocalDate.of(2027, 1, 15));
        dto.setEstado("ACTIVA");
        return dto;
    }

    @Test
    void listarPorSlugDelegaEnConsultaCertificacionServiceConLaEmpresaResuelta() {
        Empresa empresa = Empresa.builder().id(ID_EMPRESA).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        CertificacionPublicaResponseDTO dto = certificacionPublica();
        when(consultaCertificacionService.listarActivasPublicasPorEmpresa(ID_EMPRESA))
                .thenReturn(List.of(dto));

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).containsExactly(dto);
        verify(empresaRepository).findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO);
        verify(consultaCertificacionService).listarActivasPublicasPorEmpresa(ID_EMPRESA);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaNoExiste() {
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(consultaCertificacionService);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaEstaInactiva() {
        // findBySlugAndEstado ya filtra por ACTIVO en la consulta: una empresa
        // INACTIVA simplemente no matchea, exactamente igual que un slug inexistente.
        // Este test documenta esa decision; el filtro en si lo prueba
        // EmpresaRepository (Spring Data), no este servicio.
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
        when(consultaCertificacionService.listarActivasPublicasPorEmpresa(ID_EMPRESA))
                .thenReturn(List.of());

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).isEmpty();
    }
}
