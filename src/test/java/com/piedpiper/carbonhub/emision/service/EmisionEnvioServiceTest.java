package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarEnvioRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionEnvioServiceTest {

    @Mock
    private ClimatiqClient climatiqClient;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmisionEnvioMapper emisionEnvioMapper;

    @InjectMocks
    private EmisionEnvioService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private RegistrarEnvioRequestDTO requestValido() {
        return new RegistrarEnvioRequestDTO(
                "Envío de mercancía", new BigDecimal("200"), UnidadPeso.KG,
                new BigDecimal("500"), UnidadDistancia.KM, MetodoTransporte.TRUCK,
                LocalDate.now());
    }

    private Usuario usuario() {
        com.piedpiper.carbonhub.empresa.models.entities.Empresa empresa =
                com.piedpiper.carbonhub.empresa.models.entities.Empresa.builder()
                        .id(UUID.randomUUID())
                        .build();
        return Usuario.builder().id(USUARIO_ID).empresa(empresa).build();
    }

    private ClimatiqEstimateResponse estimacion(BigDecimal co2e) {
        ClimatiqEstimateResponse.EmissionFactor factor =
                new ClimatiqEstimateResponse.EmissionFactor("climatiq-factor-id", "freight_vehicle", "CR", 2023);
        return new ClimatiqEstimateResponse(co2e, "kg", factor);
    }

    @Test
    void registroExitosoPersisteConCarbonKgDerivadoDeCo2e() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(ClimatiqEmissionFactorSelector.class), any(Map.class)))
                .thenReturn(estimacion(new BigDecimal("35.500")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EmisionEnvioResponseDTO responseEsperado = new EmisionEnvioResponseDTO();
        responseEsperado.setCarbonKg(new BigDecimal("35.500"));
        when(emisionEnvioMapper.toDto(any())).thenReturn(responseEsperado);

        EmisionEnvioResponseDTO response = service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<EmisionEnvio> captor = ArgumentCaptor.forClass(EmisionEnvio.class);
        verify(emisionRepository).save(captor.capture());
        EmisionEnvio guardada = captor.getValue();

        assertThat(guardada.getCarbonKg()).isEqualByComparingTo("35.500");
        // carbonMt se deriva de carbonKg / 1000
        assertThat(guardada.getCarbonMt()).isEqualByComparingTo("0.036");
        assertThat(guardada.getFactorEmisionId()).isEqualTo("climatiq-factor-id");
        assertThat(guardada.getCreatedByUserId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getWeightValue()).isEqualByComparingTo("200");
        assertThat(guardada.getWeightUnit()).isEqualTo(UnidadPeso.KG);
        assertThat(guardada.getDistanceValue()).isEqualByComparingTo("500");
        assertThat(guardada.getDistanceUnit()).isEqualTo(UnidadDistancia.KM);
        assertThat(guardada.getTransportMethod()).isEqualTo(MetodoTransporte.TRUCK);
        assertThat(guardada.getEstimatedAt()).isNotNull();
        assertThat(response.getCarbonKg()).isEqualByComparingTo("35.500");
    }

    @Test
    void usuarioNoExistenteLanzaExcepcion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(climatiqClient, never()).estimar(any(), any());
        verify(emisionRepository, never()).save(any());
    }

    @Test
    void usuarioSinEmpresaLanza422() {
        Usuario sinEmpresa = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(sinEmpresa));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(climatiqClient, never()).estimar(any(), any());
        verify(emisionRepository, never()).save(any());
    }

    @Test
    void climatiqLanzaExcepcionNoInvocaGuardado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(ClimatiqEmissionFactorSelector.class), any(Map.class)))
                .thenThrow(ApiException.calculoNoDisponible());

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void co2eNuloLanzaExcepcion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(ClimatiqEmissionFactorSelector.class), any(Map.class)))
                .thenReturn(estimacion(null));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiEx = (ApiException) e;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(apiEx.getMessage()).contains("no devolvió un resultado válido");
                });

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void carbonMtEsDerivadoDeCarbonKgNoDependeDelProveedor() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        // co2e = 1234.567 kg -> carbonMt = 1.235 (rounded HALF_UP)
        when(climatiqClient.estimar(any(ClimatiqEmissionFactorSelector.class), any(Map.class)))
                .thenReturn(estimacion(new BigDecimal("1234.567")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionEnvioMapper.toDto(any())).thenReturn(new EmisionEnvioResponseDTO());

        service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<EmisionEnvio> captor = ArgumentCaptor.forClass(EmisionEnvio.class);
        verify(emisionRepository).save(captor.capture());
        EmisionEnvio guardada = captor.getValue();

        assertThat(guardada.getCarbonKg()).isEqualByComparingTo("1234.567");
        assertThat(guardada.getCarbonMt()).isEqualByComparingTo("1.235");
    }

    @Test
    void cadaMetodoTransporteUsaActivityIdDistinto() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(ClimatiqEmissionFactorSelector.class), any(Map.class)))
                .thenReturn(estimacion(new BigDecimal("10")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionEnvioMapper.toDto(any())).thenReturn(new EmisionEnvioResponseDTO());

        ArgumentCaptor<ClimatiqEmissionFactorSelector> selectorCaptor =
                ArgumentCaptor.forClass(ClimatiqEmissionFactorSelector.class);

        for (MetodoTransporte metodo : MetodoTransporte.values()) {
            RegistrarEnvioRequestDTO request = new RegistrarEnvioRequestDTO(
                    "Envío " + metodo.name(), new BigDecimal("100"), UnidadPeso.KG,
                    new BigDecimal("200"), UnidadDistancia.KM, metodo, LocalDate.now());

            service.registrar(request, USUARIO_ID);
        }

        verify(climatiqClient, org.mockito.Mockito.times(4))
                .estimar(selectorCaptor.capture(), any(Map.class));

        List<String> activityIds = selectorCaptor.getAllValues().stream()
                .map(ClimatiqEmissionFactorSelector::activityId)
                .toList();

        // All 4 methods should produce distinct activity IDs
        assertThat(activityIds).hasSize(4);
        assertThat(activityIds).doesNotHaveDuplicates();
        assertThat(activityIds).anyMatch(id -> id.contains("hgv"));      // TRUCK
        assertThat(activityIds).anyMatch(id -> id.contains("vessel"));   // SHIP
        assertThat(activityIds).anyMatch(id -> id.contains("train"));    // TRAIN
        assertThat(activityIds).anyMatch(id -> id.contains("flight"));   // PLANE
    }
}
