package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.validacion.mappers.ValidacionAuditorMapper;
import com.piedpiper.carbonhub.validacion.models.dtos.MiSolicitudAuditorResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiSolicitudAuditorServiceTest {

    @Mock
    private SolicitudValidacionRepository solicitudValidacionRepository;
    @Spy
    private ValidacionAuditorMapper validacionAuditorMapper = Mappers.getMapper(ValidacionAuditorMapper.class);

    @InjectMocks
    private MiSolicitudAuditorService service;

    private static final UUID AUDITOR_ID = UUID.randomUUID();

    @Test
    void devuelveElEstadoDeLaSolicitudPendiente() {
        SolicitudValidacion pendiente = SolicitudValidacion.builder()
                .id(UUID.randomUUID())
                .auditor(Usuario.builder().id(AUDITOR_ID).build())
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(Instant.parse("2026-08-01T12:00:00Z"))
                .build();
        when(solicitudValidacionRepository.findTopByAuditorIdOrderByFechaSolicitudDesc(AUDITOR_ID))
                .thenReturn(Optional.of(pendiente));

        MiSolicitudAuditorResponseDTO respuesta = service.obtener(AUDITOR_ID);

        assertThat(respuesta.getEstado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.getFechaSolicitud()).isEqualTo(Instant.parse("2026-08-01T12:00:00Z"));
        assertThat(respuesta.getFechaResolucion()).isNull();
        assertThat(respuesta.getMotivoRechazo()).isNull();
    }

    @Test
    void devuelveElMotivoDeRechazoCuandoFueRechazada() {
        SolicitudValidacion rechazada = SolicitudValidacion.builder()
                .id(UUID.randomUUID())
                .auditor(Usuario.builder().id(AUDITOR_ID).build())
                .estado(EstadoSolicitud.RECHAZADO)
                .fechaSolicitud(Instant.parse("2026-08-01T12:00:00Z"))
                .fechaResolucion(Instant.parse("2026-08-05T09:00:00Z"))
                .motivoRechazo("La certificación adjunta está vencida.")
                .build();
        when(solicitudValidacionRepository.findTopByAuditorIdOrderByFechaSolicitudDesc(AUDITOR_ID))
                .thenReturn(Optional.of(rechazada));

        MiSolicitudAuditorResponseDTO respuesta = service.obtener(AUDITOR_ID);

        assertThat(respuesta.getEstado()).isEqualTo("RECHAZADO");
        assertThat(respuesta.getMotivoRechazo()).isEqualTo("La certificación adjunta está vencida.");
        assertThat(respuesta.getFechaResolucion()).isEqualTo(Instant.parse("2026-08-05T09:00:00Z"));
    }

    @Test
    void sinSolicitudPropiaDevuelve404() {
        when(solicitudValidacionRepository.findTopByAuditorIdOrderByFechaSolicitudDesc(AUDITOR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
