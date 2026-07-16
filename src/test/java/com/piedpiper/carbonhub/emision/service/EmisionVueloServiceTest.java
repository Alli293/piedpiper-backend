package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.RegistrarVueloRequestDTO;
import com.piedpiper.carbonhub.emision.models.dtos.VueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.models.enums.CabinClass;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionVueloServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Spy
    private EmisionVueloLocalCalculator calculator = new EmisionVueloLocalCalculator();
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmisionVueloMapper emisionVueloMapper;

    @InjectMocks
    private EmisionVueloService service;

    private RegistrarVueloRequestDTO requestValido() {
        return new RegistrarVueloRequestDTO(
                2,
                List.of(
                        new RegistrarVueloRequestDTO.LegDTO("sfo", "YYZ", CabinClass.ECONOMY),
                        new RegistrarVueloRequestDTO.LegDTO("YYZ", "SFO", CabinClass.PREMIUM)),
                UnidadDistancia.KM,
                LocalDate.of(2026, 7, 1));
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }

    @Test
    void registroExitosoCalculaCadaLegYPersisteCarbonKg() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        VueloResponseDTO responseEsperado = new VueloResponseDTO();
        responseEsperado.setCarbonKg(new BigDecimal("2364.788"));
        when(emisionVueloMapper.toDto(any())).thenReturn(responseEsperado);

        EmisionResponseDTO response = service.registrar(requestValido(), USUARIO_ID);

        ArgumentCaptor<EmisionVuelo> captor = ArgumentCaptor.forClass(EmisionVuelo.class);
        verify(emisionRepository).save(captor.capture());
        EmisionVuelo guardada = captor.getValue();

        assertThat(guardada.getCarbonKg()).isEqualByComparingTo("2364.788");
        assertThat(guardada.getCarbonMt()).isEqualByComparingTo("2.365");
        assertThat(guardada.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(guardada.getCreatedByUserId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getTitulo()).isEqualTo("Viaje a\u00e9reo SFO-YYZ-SFO");
        assertThat(guardada.getDistanceUnit()).isEqualTo(UnidadDistancia.KM);
        assertThat(guardada.getDistanceValue()).isEqualByComparingTo("7908.990");
        assertThat(guardada.getFactorEmisionId()).isEqualTo("local-flight-distance-v1");
        assertThat(guardada.getLegs()).hasSize(2);
        assertThat(guardada.getLegs().get(0).getDepartureAirport()).isEqualTo("SFO");
        assertThat(guardada.getLegs().get(1).getCabinClass()).isEqualTo(CabinClass.PREMIUM);
        assertThat(response.getCarbonKg()).isEqualByComparingTo("2364.788");
    }

    @Test
    void aeropuertoSinCoordenadasNoPersisteNada() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        RegistrarVueloRequestDTO request = new RegistrarVueloRequestDTO(
                1,
                List.of(new RegistrarVueloRequestDTO.LegDTO("ZZZ", "SFO", CabinClass.ECONOMY)),
                UnidadDistancia.KM,
                LocalDate.of(2026, 7, 1));

        assertThatThrownBy(() -> service.registrar(request, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(emisionRepository, never()).save(any());
    }

    @Test
    void registroRespetaUnidadMillasDelRequest() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionVueloMapper.toDto(any())).thenReturn(new VueloResponseDTO());
        RegistrarVueloRequestDTO request = requestValido();
        request.setDistanceUnit(UnidadDistancia.MI);

        service.registrar(request, USUARIO_ID);

        ArgumentCaptor<EmisionVuelo> captor = ArgumentCaptor.forClass(EmisionVuelo.class);
        verify(emisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDistanceUnit()).isEqualTo(UnidadDistancia.MI);
        assertThat(captor.getValue().getDistanceValue()).isEqualByComparingTo("4914.417");
    }

    @Test
    void actualizarVueloUsaEmpresaDelUsuarioYRecalculaDatos() {
        UUID emisionId = UUID.randomUUID();
        EmisionVuelo existente = EmisionVuelo.builder()
                .id(emisionId)
                .empresaId(EMPRESA_ID)
                .createdByUserId(UUID.randomUUID())
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findByIdAndEmpresaId(emisionId, EMPRESA_ID))
                .thenReturn(Optional.of(existente));
        when(emisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emisionVueloMapper.toDto(any())).thenReturn(new VueloResponseDTO());

        service.actualizar(emisionId, requestValido(), USUARIO_ID);

        verify(emisionRepository).findByIdAndEmpresaId(emisionId, EMPRESA_ID);
        ArgumentCaptor<EmisionVuelo> captor = ArgumentCaptor.forClass(EmisionVuelo.class);
        verify(emisionRepository).save(captor.capture());
        assertThat(captor.getValue().getLegs()).hasSize(2);
        assertThat(captor.getValue().getTitulo()).isEqualTo("Viaje a\u00e9reo SFO-YYZ-SFO");
        assertThat(captor.getValue().getCarbonKg()).isEqualByComparingTo("2364.788");
    }

    @Test
    void actualizarVueloDeOtraEmpresaDevuelve404() {
        UUID emisionId = UUID.randomUUID();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findByIdAndEmpresaId(emisionId, EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(emisionId, requestValido(), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(emisionRepository, never()).save(any());
    }
}
