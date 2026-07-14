package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarFlotaRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.TipoVehiculoResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEmissionFactorSelector;
import com.piedpiper.carbonhub.emision.models.dtos.climatiq.ClimatiqEstimateResponse;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionFlotaServiceTest {

    @Mock
    private ClimatiqClient climatiqClient;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmisionFlotaMapper emisionFlotaMapper;

    @InjectMocks
    private EmisionFlotaService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private RegistrarFlotaRequestDTO requestValido() {
        return new RegistrarFlotaRequestDTO(
                "Recorrido Toyota Corolla", TipoVehiculo.AUTOMOVIL, Combustible.GASOLINA,
                new BigDecimal("100"), UnidadDistancia.KM, LocalDate.now());
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
                        "climatiq-factor-id",
                        "passenger_vehicle-vehicle_type_car-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                                + "-vehicle_weight_na",
                        "DE", 2024);
        return new ClimatiqEstimateResponse(carbonKg, "kg", factor);
    }

    @Test
    void registroExitosoPersisteConCarbonKg() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenReturn(estimacion(new BigDecimal("22.85")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EmisionFlotaResponseDTO responseEsperado = new EmisionFlotaResponseDTO();
        responseEsperado.setCarbonKg(new BigDecimal("22.85"));
        when(emisionFlotaMapper.toDto(any())).thenReturn(responseEsperado);

        EmisionFlotaResponseDTO response = service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<EmisionFlota> captor = ArgumentCaptor.forClass(EmisionFlota.class);
        verify(emisionRepository).save(captor.capture());
        EmisionFlota guardada = captor.getValue();

        assertThat(guardada.getCarbonKg()).isEqualByComparingTo("22.85");
        assertThat(guardada.getCarbonMt()).isEqualByComparingTo("0.023");
        assertThat(guardada.getFactorEmisionId()).isEqualTo("climatiq-factor-id");
        assertThat(guardada.getTipoVehiculo()).isEqualTo(TipoVehiculo.AUTOMOVIL);
        assertThat(guardada.getCombustible()).isEqualTo(Combustible.GASOLINA);
        assertThat(guardada.getDistanceValue()).isEqualByComparingTo("100");
        assertThat(guardada.getDistanceUnit()).isEqualTo(UnidadDistancia.KM);
        assertThat(guardada.getCreatedByUserId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(response.getCarbonKg()).isEqualByComparingTo("22.85");
    }

    @Test
    void enviaSelectorConActivityIdFijoSegunTipoYCombustible() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenReturn(estimacion(new BigDecimal("22.85")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionFlotaMapper.toDto(any())).thenReturn(new EmisionFlotaResponseDTO());

        service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<ClimatiqEmissionFactorSelector> selectorCaptor =
                ArgumentCaptor.forClass(ClimatiqEmissionFactorSelector.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parametersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(climatiqClient).estimar(selectorCaptor.capture(), parametersCaptor.capture());
        ClimatiqEmissionFactorSelector selector = selectorCaptor.getValue();

        assertThat(selector.activityId()).isEqualTo(
                "passenger_vehicle-vehicle_type_car-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                        + "-vehicle_weight_na");
        assertThat(selector.dataVersion()).isEqualTo("^6");
        assertThat(selector.region()).isEqualTo("DE");
        assertThat(parametersCaptor.getValue())
                .containsEntry("distance", new BigDecimal("100"))
                .containsEntry("distance_unit", "km");
    }

    @Test
    void enviaDistanceUnitMiCuandoSeSeleccionaMillas() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(climatiqClient.estimar(any(), any())).thenReturn(estimacion(new BigDecimal("22.85")));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionFlotaMapper.toDto(any())).thenReturn(new EmisionFlotaResponseDTO());

        RegistrarFlotaRequestDTO request = new RegistrarFlotaRequestDTO(
                "Recorrido Toyota Corolla", TipoVehiculo.AUTOMOVIL, Combustible.GASOLINA,
                new BigDecimal("100"), UnidadDistancia.MI, LocalDate.now());

        service.registrar(request, USUARIO_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parametersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(climatiqClient).estimar(any(), parametersCaptor.capture());
        assertThat(parametersCaptor.getValue())
                .containsEntry("distance", new BigDecimal("100"))
                .containsEntry("distance_unit", "mi");
    }

    @Test
    void combinacionTipoVehiculoCombustibleInvalidaNoInvocaClimatiqNiGuardado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        RegistrarFlotaRequestDTO request = new RegistrarFlotaRequestDTO(
                "Recorrido camión", TipoVehiculo.CAMION_PESADO, Combustible.DIESEL,
                new BigDecimal("100"), UnidadDistancia.KM, LocalDate.now());

        assertThatThrownBy(() -> service.registrar(request, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(climatiqClient, never()).estimar(any(), any());
        verify(emisionRepository, never()).save(any());
    }

    @Test
    void listarTiposVehiculoDevuelveLosCombustiblesValidosPorTipo() {
        List<TipoVehiculoResponseDTO> tipos = service.listarTiposVehiculo();

        assertThat(tipos).hasSize(TipoVehiculo.values().length);
        TipoVehiculoResponseDTO camionPesado = tipos.stream()
                .filter(t -> t.getId().equals("CAMION_PESADO"))
                .findFirst().orElseThrow();
        assertThat(camionPesado.getCombustibles()).hasSize(1);
        assertThat(camionPesado.getCombustibles().get(0).getId()).isEqualTo("PROMEDIO");

        TipoVehiculoResponseDTO automovil = tipos.stream()
                .filter(t -> t.getId().equals("AUTOMOVIL"))
                .findFirst().orElseThrow();
        assertThat(automovil.getCombustibles()).extracting(c -> c.getId())
                .contains("PROMEDIO", "GASOLINA", "DIESEL", "PHEV", "BEV");
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
                        "climatiq-factor-id",
                        "passenger_vehicle-vehicle_type_car-fuel_source_gasoline-engine_size_na-vehicle_age_na"
                                + "-vehicle_weight_na",
                        "DE", 2024);
        when(climatiqClient.estimar(any(), any()))
                .thenReturn(new ClimatiqEstimateResponse(new BigDecimal("22.85"), "t", factor));

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
                .thenThrow(ApiException.calculoInvalido("vehículo fuera de catálogo"));

        assertThatThrownBy(() -> service.registrar(requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(emisionRepository, never()).save(any());
    }
}
