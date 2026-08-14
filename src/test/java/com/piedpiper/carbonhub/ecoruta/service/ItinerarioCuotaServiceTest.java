package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItinerarioCuotaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private PreferenciasViajeRepository preferenciasViajeRepository;

    private ItinerarioCuotaService service;

    private PreferenciasViaje preferencias() {
        return PreferenciasViaje.builder()
                .id(UUID.randomUUID())
                .cantidadDias(2)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .itinerarioGeneracionContador(0)
                .build();
    }

    @org.junit.jupiter.api.BeforeEach
    void configurar() {
        service = new ItinerarioCuotaService(preferenciasViajeRepository);
    }

    @Test
    void sinPreferenciasGuardadasLanza404() {
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarGeneracion(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void primeraSolicitudIncrementaElContadorYGuarda() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        service.reservarGeneracion(USUARIO_ID);

        assertThat(preferencias.getItinerarioGeneracionContador()).isEqualTo(1);
        org.mockito.Mockito.verify(preferenciasViajeRepository).saveAndFlush(preferencias);
    }

    @Test
    void reinicializaLaVentanaDeRateLimitPasadaUnaHora() {
        PreferenciasViaje preferencias = preferencias();
        preferencias.setItinerarioGeneracionContador(5);
        preferencias.setItinerarioGeneracionVentanaInicio(Instant.now().minus(2, ChronoUnit.HOURS));
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        service.reservarGeneracion(USUARIO_ID);

        assertThat(preferencias.getItinerarioGeneracionContador()).isEqualTo(1);
    }

    @Test
    void sextaSolicitudEnLaMismaHoraLanza429() {
        PreferenciasViaje preferencias = preferencias();
        preferencias.setItinerarioGeneracionContador(5);
        preferencias.setItinerarioGeneracionVentanaInicio(Instant.now());
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        assertThatThrownBy(() -> service.reservarGeneracion(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        org.mockito.Mockito.verify(preferenciasViajeRepository, org.mockito.Mockito.never())
                .saveAndFlush(any());
    }

    // --- reservarRefinamiento (PP-88, chat de refinamiento) ---

    @Test
    void refinamientoSinPreferenciasGuardadasLanza404() {
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarRefinamiento(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void primerMensajeIncrementaElContadorDeRefinamientoYGuarda() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        service.reservarRefinamiento(USUARIO_ID);

        assertThat(preferencias.getItinerarioRefinamientoContador()).isEqualTo(1);
        org.mockito.Mockito.verify(preferenciasViajeRepository).saveAndFlush(preferencias);
    }

    @Test
    void reinicializaLaVentanaDeRefinamientoPasadaUnaHora() {
        PreferenciasViaje preferencias = preferencias();
        preferencias.setItinerarioRefinamientoContador(20);
        preferencias.setItinerarioRefinamientoVentanaInicio(Instant.now().minus(2, ChronoUnit.HOURS));
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        service.reservarRefinamiento(USUARIO_ID);

        assertThat(preferencias.getItinerarioRefinamientoContador()).isEqualTo(1);
    }

    @Test
    void mensaje21EnLaMismaHoraLanza429() {
        PreferenciasViaje preferencias = preferencias();
        preferencias.setItinerarioRefinamientoContador(20);
        preferencias.setItinerarioRefinamientoVentanaInicio(Instant.now());
        when(preferenciasViajeRepository.findByUsuario_IdForUpdate(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        assertThatThrownBy(() -> service.reservarRefinamiento(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        org.mockito.Mockito.verify(preferenciasViajeRepository, org.mockito.Mockito.never())
                .saveAndFlush(any());
    }
}
