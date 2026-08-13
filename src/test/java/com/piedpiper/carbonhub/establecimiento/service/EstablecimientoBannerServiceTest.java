package com.piedpiper.carbonhub.establecimiento.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;
import com.piedpiper.carbonhub.establecimiento.models.dtos.EstablecimientoBannerResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablecimientoBannerServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CountriesDevClient countriesDevClient;

    private EstablecimientoBannerService service;

    private UUID empresaId;

    @BeforeEach
    void setUp() {
        service = new EstablecimientoBannerService(empresaRepository, countriesDevClient);
        empresaId = UUID.randomUUID();
    }

    private Empresa empresaConPais(String pais) {
        return Empresa.builder().id(empresaId).nombreEmpresa("Hotel Capitán Suizo").pais(pais).build();
    }

    @Test
    void obtenerBannerRetornaElDtoConLosCamposCorrectosCuandoElClienteResuelveElPais() {
        BannerOrigenDTO banner = new BannerOrigenDTO("Costa Rica", "\uD83C\uDDE8\uD83C\uDDF7",
                "https://flagcdn.com/cr.svg", "CR");
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaConPais("CR")));
        when(countriesDevClient.consultarPais("CR")).thenReturn(Optional.of(banner));

        EstablecimientoBannerResponseDTO resultado = service.obtenerBanner(empresaId);

        assertThat(resultado.getBanner()).isEqualTo(banner);
        assertThat(resultado.getBanner().getNombrePais()).isEqualTo("Costa Rica");
        assertThat(resultado.getBanner().getCodigoIso()).isEqualTo("CR");
    }

    @Test
    void obtenerBannerConsultaElClienteConElPaisDeLaEmpresa() {
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaConPais("PA")));
        when(countriesDevClient.consultarPais("PA")).thenReturn(Optional.empty());

        service.obtenerBanner(empresaId);

        verify(countriesDevClient).consultarPais(eq("PA"));
    }

    @Test
    void obtenerBannerRetornaBannerNuloSinLanzarCuandoElClienteNoResuelveElPais() {
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaConPais("CR")));
        when(countriesDevClient.consultarPais("CR")).thenReturn(Optional.empty());

        EstablecimientoBannerResponseDTO resultado = service.obtenerBanner(empresaId);

        assertThat(resultado.getBanner()).isNull();
    }

    @Test
    void obtenerBannerLanza404CuandoLaEmpresaNoExiste() {
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerBanner(empresaId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(countriesDevClient, never()).consultarPais(org.mockito.ArgumentMatchers.any());
    }
}
