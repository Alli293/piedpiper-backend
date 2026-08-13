package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapperImpl;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoRefinamientoIA;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba, con Hibernate y una base H2 reales (no mocks de repositorio), que
 * {@link EcoRutaItinerarioService#refinar} reemplaza correctamente los días/actividades de un
 * itinerario YA PERSISTIDO. El test unitario de {@code EcoRutaItinerarioServiceTest} mockea
 * {@code ItinerarioRepository.saveAndFlush(...)} para simplemente devolver el argumento, así que
 * nunca podía detectar que {@code itinerario.setDias(nuevaLista)} reventaba en producción con
 * "A collection with orphan deletion was no longer referenced by the owning entity instance" —
 * Hibernate exige mutar la MISMA instancia de una colección {@code @OneToMany(orphanRemoval=true)}
 * ya administrada, no reemplazar su referencia. Reportado en vivo por Alli probando el chat.
 */
@DataJpaTest
class EcoRutaItinerarioRefinarIntegrationTest {

    @Autowired
    private ItinerarioRepository itinerarioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private ItinerarioIaClienteService itinerarioIaClienteService;
    private EcoRutaItinerarioService service;

    @BeforeEach
    void setUp() {
        itinerarioIaClienteService = mock(ItinerarioIaClienteService.class);
        service = new EcoRutaItinerarioService(
                mock(PreferenciasViajeRepository.class),
                itinerarioRepository,
                itinerarioIaClienteService,
                mock(ItinerarioCuotaService.class),
                mock(EventoReconocimientoService.class),
                mock(PriorizacionAmbientalService.class),
                mock(EcoScoreService.class),
                mock(IndicadorAmbientalClient.class),
                mock(ImaClient.class),
                mock(BenchmarkClient.class),
                mock(PuntuacionAmbientalCalculator.class),
                mock(EmpresaRepository.class),
                new ItinerarioMapperImpl(),
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private Usuario usuarioActivo() {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email("viajero.refinamiento." + java.util.UUID.randomUUID() + "@carbonhub.test")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build());
    }

    private Itinerario itinerarioPersistidoConUnaActividad(Usuario usuario) {
        Itinerario itinerario = Itinerario.builder()
                .usuario(usuario)
                .cantidadDias(1)
                .fechaInicio(LocalDate.now().plusDays(5))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .estado(EstadoItinerario.GENERADO)
                .fechaGeneracion(Instant.now())
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .itinerario(itinerario)
                .numeroDia(1)
                .fecha(itinerario.getFechaInicio())
                .orden(1)
                .build();
        ItinerarioActividad actividad = ItinerarioActividad.builder()
                .itinerarioDia(dia)
                .nombre("Caminata original")
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(120)
                .provincia(com.piedpiper.carbonhub.ecoruta.models.enums.Provincia.PUNTARENAS)
                .orden(1)
                .build();
        dia.setActividades(new ArrayList<>(List.of(actividad)));
        itinerario.setDias(new ArrayList<>(List.of(dia)));

        return itinerarioRepository.saveAndFlush(itinerario);
    }

    private ActividadIaDTO actividadRefinada(String nombre) {
        return new ActividadIaDTO(nombre, "Descripción nueva", "10:00", 90,
                new BigDecimal("5000"), "CRC", "Reserva Nueva", "PUNTARENAS", 88, "NATURALEZA");
    }

    @Test
    void refinarReemplazaLosDiasDeUnItinerarioPersistidoSinRomperElOrphanRemoval() {
        Usuario usuario = usuarioActivo();
        Itinerario itinerario = itinerarioPersistidoConUnaActividad(usuario);

        ItinerarioIaResponseDTO itinerarioActualizado = new ItinerarioIaResponseDTO(
                List.of(new DiaIaDTO(1, "2026-08-01",
                        List.of(actividadRefinada("Caminata nueva"), actividadRefinada("Almuerzo local")))),
                85);
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Listo, agregué una parada para almorzar.", null, itinerarioActualizado);
        when(itinerarioIaClienteService.refinar(any(), anyInt())).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        RefinamientoItinerarioRequestDTO request = new RefinamientoItinerarioRequestDTO(
                "Agrega una parada para almorzar.", null);

        // Antes del fix, esta llamada lanzaba JpaSystemException al hacer flush.
        RefinamientoItinerarioResponseDTO response = service.refinar(itinerario.getId(), usuario.getId(), request);

        assertThat(response.getItinerario().getVersion()).isEqualTo(2);

        ItinerarioResponseDTO recargado = service.obtener(itinerario.getId(), usuario.getId());
        assertThat(recargado.getVersion()).isEqualTo(2);
        assertThat(recargado.getDias()).hasSize(1);
        assertThat(recargado.getDias().get(0).getActividades()).hasSize(2);
        assertThat(recargado.getDias().get(0).getActividades())
                .extracting("nombre")
                .containsExactlyInAnyOrder("Caminata nueva", "Almuerzo local");
    }
}
