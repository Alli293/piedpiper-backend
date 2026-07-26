package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapperImpl;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionElectricidadResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarElectricidadRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.service.ImaCacheInvalidator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionElectricidadServiceTest {

    @Mock
    private ClimatiqClient climatiqClient;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Spy
    private EmisionElectricidadMapper emisionElectricidadMapper = new EmisionElectricidadMapperImpl();

    @Mock
    private ImaCacheInvalidator imaCacheInvalidator;

    @InjectMocks
    private EmisionElectricidadService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private RegistrarElectricidadRequestDTO requestValido() {
        return new RegistrarElectricidadRequestDTO(
                "Consumo oficina central", new BigDecimal("500"), UnidadElectricidad.KWH,
                LocalDate.now());
    }

    private Usuario usuario() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Usuario.builder().id(USUARIO_ID).empresa(empresa).build();
    }

    private Usuario usuarioSinEmpresa() {
        return Usuario.builder().id(USUARIO_ID).build();
    }

    private ClimatiqEstimateResponse estimacion(BigDecimal carbonKg) {
        ClimatiqEstimateResponse.EmissionFactor factor =
                new ClimatiqEstimateResponse.EmissionFactor(
                        "climatiq-factor-id", "electricity-supply_grid-source_supplier_mix-use_na", "CR", 2024);
        return new ClimatiqEstimateResponse(carbonKg, "kg", factor);
    }

    @Test
    void registroExitosoPersisteConCarbonKg() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenReturn(estimacion(new BigDecimal("27.85")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EmisionElectricidadResponseDTO response = service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<EmisionElectricidad> captor = ArgumentCaptor.forClass(EmisionElectricidad.class);
        verify(emisionRepository).save(captor.capture());
        EmisionElectricidad guardada = captor.getValue();

        assertThat(guardada.getCarbonKg()).isEqualByComparingTo("27.85");
        assertThat(guardada.getCarbonMt()).isEqualByComparingTo("0.028");
        assertThat(guardada.getFactorEmisionId()).isEqualTo("climatiq-factor-id");
        assertThat(guardada.getCreatedByUserId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(response.getCarbonKg()).isEqualByComparingTo("27.85");
        verify(imaCacheInvalidator).invalidar(EMPRESA_ID);
    }

    @Test
    void enviaSelectorConActivityIdDataVersionYRegionCorrectos() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenReturn(estimacion(new BigDecimal("27.85")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<ClimatiqEmissionFactorSelector> selectorCaptor =
                ArgumentCaptor.forClass(ClimatiqEmissionFactorSelector.class);
        verify(climatiqClient).estimar(selectorCaptor.capture(), any());
        ClimatiqEmissionFactorSelector selector = selectorCaptor.getValue();

        assertThat(selector.activityId()).isEqualTo("electricity-supply_grid-source_supplier_mix-use_na");
        assertThat(selector.dataVersion()).isEqualTo("^6");
        assertThat(selector.region()).isEqualTo("CR");
    }

    @Test
    void usuarioSinEmpresaNoPuedeRegistrar() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioSinEmpresa()));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void unidadDeCo2eDistintaDeKgNoInvocaGuardado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        ClimatiqEstimateResponse.EmissionFactor factor =
                new ClimatiqEstimateResponse.EmissionFactor(
                        "climatiq-factor-id", "electricity-supply_grid-source_supplier_mix-use_na", "CR", 2024);
        when(climatiqClient.estimar(any(), any()))
                .thenReturn(new ClimatiqEstimateResponse(new BigDecimal("27.85"), "t", factor));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void clienteLanzaExcepcionNoInvocaGuardado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenThrow(ApiException.calculoNoDisponible());

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void errorDeValidacionDelCalculoNoInvocaGuardado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any()))
                .thenThrow(ApiException.calculoInvalido("valor fuera de rango"));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(emisionRepository, never()).save(any());
    }
}
