package com.piedpiper.carbonhub.ecoruta.mappers;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioActividadResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItinerarioMapperTest {

    private final ItinerarioMapper mapper = Mappers.getMapper(ItinerarioMapper.class);

    @Test
    void toDtoMapeaIdDeActividadCorrectamente() {
        UUID actividadId = UUID.randomUUID();

        ItinerarioActividad entidad = ItinerarioActividad.builder()
                .id(actividadId)
                .nombre("Senderismo")
                .descripcion("Recorrido guiado")
                .horario(LocalTime.of(8, 0))
                .duracionMinutos(120)
                .costoAproximado(new BigDecimal("15000"))
                .moneda(Moneda.CRC)
                .establecimientoRecomendado("Reserva Biológica")
                .provincia(Provincia.HEREDIA)
                .orden(1)
                .puntuacionAmbientalEstimada(75)
                .build();

        ItinerarioActividadResponseDTO dto = mapper.toDto(entidad);

        assertThat(dto.getId()).isEqualTo(actividadId);
        assertThat(dto.getNombre()).isEqualTo("Senderismo");
        assertThat(dto.getMoneda()).isEqualTo("CRC");
        assertThat(dto.getProvincia()).isEqualTo("HEREDIA");
    }

    @Test
    void toDtoConMonedaNulaRetornaNullEnMoneda() {
        ItinerarioActividad entidad = ItinerarioActividad.builder()
                .id(UUID.randomUUID())
                .nombre("Kayak")
                .horario(LocalTime.of(10, 0))
                .duracionMinutos(90)
                .provincia(Provincia.PUNTARENAS)
                .orden(2)
                .build();

        ItinerarioActividadResponseDTO dto = mapper.toDto(entidad);

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getMoneda()).isNull();
    }
}
