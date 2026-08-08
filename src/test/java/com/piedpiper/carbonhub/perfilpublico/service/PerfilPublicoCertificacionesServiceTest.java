package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicoCertificacionesServiceTest {

    private static final String SLUG = "empresa-verde";
    private static final UUID ID_EMPRESA = UUID.randomUUID();

    @Mock
    private SlugResolverService slugResolver;
    @Mock
    private ConsultaCertificacionService consultaCertificacionService;

    private PerfilPublicoCertificacionesService service;

    @BeforeEach
    void prepararServicio() {
        service = new PerfilPublicoCertificacionesService(
                slugResolver, consultaCertificacionService);
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
        when(slugResolver.resolver(SLUG)).thenReturn(empresa);
        CertificacionPublicaResponseDTO dto = certificacionPublica();
        when(consultaCertificacionService.listarActivasPublicasPorEmpresa(ID_EMPRESA))
                .thenReturn(List.of(dto));

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).containsExactly(dto);
        verify(slugResolver).resolver(SLUG);
        verify(consultaCertificacionService).listarActivasPublicasPorEmpresa(ID_EMPRESA);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaNoExiste() {
        when(slugResolver.resolver(SLUG))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        verifyNoInteractions(consultaCertificacionService);
    }

    @Test
    void listarPorSlugLanza404CuandoLaEmpresaEstaInactiva() {
        // SlugResolverService ya filtra por ACTIVO: una empresa INACTIVA
        // simplemente no matchea, y el resolver lanza PerfilNoEncontradoException.
        when(slugResolver.resolver(SLUG))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        verify(slugResolver).resolver(SLUG);
    }

    @Test
    void listarPorSlugRetornaListaVaciaCuandoLaEmpresaNoTieneCertificacionesActivas() {
        Empresa empresa = Empresa.builder().id(ID_EMPRESA).build();
        when(slugResolver.resolver(SLUG)).thenReturn(empresa);
        when(consultaCertificacionService.listarActivasPublicasPorEmpresa(ID_EMPRESA))
                .thenReturn(List.of());

        List<CertificacionPublicaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).isEmpty();
    }
}
