package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionesResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
class RecomendacionAmbientalServiceTest {

    @Mock
    private ItinerarioRepository itinerarioRepository;
    @Mock
    private ComparacionAlternativasService comparacionAlternativasService;
    @Mock
    private AlternativasIaClienteService alternativasIaClienteService;

    private RecomendacionAmbientalService service;

    private UUID usuarioId;
    private UUID itinerarioId;

    @BeforeEach
    void setUp() {
        service = new RecomendacionAmbientalService(
                itinerarioRepository, comparacionAlternativasService, alternativasIaClienteService);
        usuarioId = UUID.randomUUID();
        itinerarioId = UUID.randomUUID();
    }

    // --- Helpers ---

    private ItinerarioActividad actividad(String nombre, int puntuacion) {
        return ItinerarioActividad.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .categoriaTuristica(InteresTuristico.AVENTURA)
                .provincia(Provincia.PUNTARENAS)
                .puntuacionAmbientalEstimada(puntuacion)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(120)
                .orden(1)
                .build();
    }

    private Itinerario itinerarioCon(ClasificacionAmbiental clasificacion, BigDecimal ecoScore,
                                     ItinerarioActividad... actividades) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario)
                .cantidadDias(1)
                .fechaInicio(LocalDate.now())
                .ecoScore(ecoScore)
                .clasificacionAmbiental(clasificacion)
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .id(UUID.randomUUID())
                .itinerario(itinerario)
                .numeroDia(1)
                .fecha(LocalDate.now())
                .orden(1)
                .actividades(List.of(actividades))
                .build();

        for (ItinerarioActividad actividad : actividades) {
            actividad.setItinerarioDia(dia);
        }
        itinerario.setDias(List.of(dia));
        return itinerario;
    }

    // --- Validaciones de pertenencia y de EcoScore previo ---

    @Test
    void obtenerRecomendacionesLanza403CuandoItinerarioNoPertenecalUsuario() {
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerRecomendaciones(itinerarioId, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(alternativasIaClienteService, never()).buscarAlternativas(any(), any());
    }

    @Test
    void obtenerRecomendacionesLanza400CuandoNoHayEcoScoreCalculado() {
        Itinerario itinerario = itinerarioCon(null, null, actividad("Canopy Tour", 60));

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        assertThatThrownBy(() -> service.obtenerRecomendaciones(itinerarioId, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // --- Itinerario ya optimizado ---

    @Test
    void obtenerRecomendacionesRetornaMensajeInformativoCuandoClasificacionEsExcelente() {
        Itinerario itinerario = itinerarioCon(
                ClasificacionAmbiental.EXCELENTE, new BigDecimal("92.0"),
                actividad("Canopy Tour", 90));

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).isEmpty();
        assertThat(resultado.getMensaje()).isEqualTo("Tu itinerario ya presenta un excelente desempeño ambiental.");
        verify(alternativasIaClienteService, never()).buscarAlternativas(any(), any());
    }

    @Test
    void obtenerRecomendacionesRetornaMensajeNeutroCuandoNoHayActividadesMejorablesYClasificacionNoEsExcelente() {
        // BUENA (no EXCELENTE) y sin actividades bajo el umbral: no hay nada que recomendar,
        // pero afirmar "excelente desempeño" acá sería engañoso (regresión del bug reportado
        // en producción: EcoScore Moderado mostrando el mensaje de itinerario excelente).
        Itinerario itinerario = itinerarioCon(
                ClasificacionAmbiental.BUENA, new BigDecimal("65.0"),
                actividad("Canopy Tour", 65));

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).isEmpty();
        assertThat(resultado.getMensaje())
                .isEqualTo("No encontramos actividades específicas que sustituir para mejorar tu EcoScore en este momento.")
                .isNotEqualTo("Tu itinerario ya presenta un excelente desempeño ambiental.");
    }

    @Test
    void obtenerRecomendacionesRetornaMensajeNeutroCuandoIaNoEncuentraAlternativasYClasificacionEsModerada() {
        // Reproduce el caso reportado: EcoScore 47.3 (Moderada) impulsado por el componente de
        // establecimientos, con actividades individuales que puntúan bien (>= umbral) y por lo
        // tanto no generan recomendaciones. El mensaje no debe afirmar "excelente desempeño".
        ItinerarioActividad actividad = actividad("Llegada y check-in en hotel sostenible", 90);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("47.3"), actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).isEmpty();
        assertThat(resultado.getMensaje())
                .isEqualTo("No encontramos actividades específicas que sustituir para mejorar tu EcoScore en este momento.");
        verify(alternativasIaClienteService, never()).buscarAlternativas(any(), any());
    }

    // --- Generación de recomendaciones a partir del EcoScore ---

    @Test
    void obtenerRecomendacionesGeneraRecomendacionParaActividadMejorable() {
        ItinerarioActividad actividad = actividad("Canopy Tour", 50);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("55.0"), actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Senderismo en reserva", "Recorrido guiado",
                        new BigDecimal("15000"), "CRC", "Reserva Verde", 85)));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getMensaje()).isNull();
        assertThat(resultado.getRecomendaciones()).hasSize(1);

        RecomendacionAmbientalDTO recomendacion = resultado.getRecomendaciones().get(0);
        assertThat(recomendacion.getTipo()).isEqualTo("ACTIVIDAD_ALTERNATIVA");
        assertThat(recomendacion.getActividadId()).isEqualTo(actividad.getId());
        assertThat(recomendacion.getActividadNombre()).isEqualTo("Canopy Tour");
        assertThat(recomendacion.getAlternativa().getNombre()).isEqualTo("Senderismo en reserva");
        assertThat(recomendacion.getAlternativa().getEcoScore()).isEqualTo(85);
        assertThat(recomendacion.getCategoriaTuristica()).isEqualTo("AVENTURA");
        assertThat(recomendacion.getProvincia()).isEqualTo("PUNTARENAS");
    }

    @Test
    void obtenerRecomendacionesCalculaIncrementoEstimadoPonderadoPorCantidadDeActividades() {
        // Una sola actividad mejorable de dos: diferencia 35 * peso 0.20 / 2 actividades = 3.5
        ItinerarioActividad mejorable = actividad("Canopy Tour", 50);
        ItinerarioActividad optima = actividad("Kayak sostenible", 90);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("55.0"),
                mejorable, optima);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(alternativasIaClienteService.buscarAlternativas(eq(mejorable), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Senderismo en reserva", "Recorrido guiado",
                        new BigDecimal("15000"), "CRC", "Reserva Verde", 85)));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).hasSize(1);
        assertThat(resultado.getRecomendaciones().get(0).getIncrementoEstimado())
                .isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    void obtenerRecomendacionesOrdenaPorImpactoEsperadoDescendente() {
        ItinerarioActividad menorImpacto = actividad("Paseo en bote", 55); // diferencia 10
        ItinerarioActividad mayorImpacto = actividad("Canopy Tour", 40); // diferencia 45
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("45.0"),
                menorImpacto, mayorImpacto);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(alternativasIaClienteService.buscarAlternativas(eq(menorImpacto), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Alt bote", "Desc",
                        new BigDecimal("10000"), "CRC", "Est A", 65)));
        when(alternativasIaClienteService.buscarAlternativas(eq(mayorImpacto), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Alt canopy", "Desc",
                        new BigDecimal("12000"), "CRC", "Est B", 85)));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).hasSize(2);
        assertThat(resultado.getRecomendaciones().get(0).getActividadNombre()).isEqualTo("Canopy Tour");
        assertThat(resultado.getRecomendaciones().get(1).getActividadNombre()).isEqualTo("Paseo en bote");
        assertThat(resultado.getRecomendaciones().get(0).getIncrementoEstimado())
                .isGreaterThan(resultado.getRecomendaciones().get(1).getIncrementoEstimado());
    }

    @Test
    void obtenerRecomendacionesDescartaActividadCuandoIaNoDevuelveAlternativas() {
        ItinerarioActividad actividad = actividad("Canopy Tour", 50);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("55.0"), actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(List.of());

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).isEmpty();
        assertThat(resultado.getMensaje())
                .isEqualTo("No encontramos actividades específicas que sustituir para mejorar tu EcoScore en este momento.");
    }

    @Test
    void obtenerRecomendacionesContinuaConLasDemasActividadesCuandoIaFallaParaUna() {
        ItinerarioActividad fallaIa = actividad("Canopy Tour", 40);
        ItinerarioActividad conAlternativa = actividad("Paseo en bote", 45);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MODERADA, new BigDecimal("42.0"),
                fallaIa, conAlternativa);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(alternativasIaClienteService.buscarAlternativas(eq(fallaIa), any()))
                .thenThrow(ApiException.itinerarioGeneracionTimeout());
        when(alternativasIaClienteService.buscarAlternativas(eq(conAlternativa), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Alt bote", "Desc",
                        new BigDecimal("10000"), "CRC", "Est A", 80)));

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).hasSize(1);
        assertThat(resultado.getRecomendaciones().get(0).getActividadNombre()).isEqualTo("Paseo en bote");
    }

    @Test
    void obtenerRecomendacionesLimitaATresRecomendaciones() {
        ItinerarioActividad a1 = actividad("Actividad 1", 30);
        ItinerarioActividad a2 = actividad("Actividad 2", 35);
        ItinerarioActividad a3 = actividad("Actividad 3", 40);
        ItinerarioActividad a4 = actividad("Actividad 4", 45);
        Itinerario itinerario = itinerarioCon(ClasificacionAmbiental.MEJORABLE, new BigDecimal("35.0"),
                a1, a2, a3, a4);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        for (ItinerarioActividad a : List.of(a1, a2, a3, a4)) {
            when(alternativasIaClienteService.buscarAlternativas(eq(a), any()))
                    .thenReturn(List.of(new AlternativaIaDTO("Alt", "Desc",
                            new BigDecimal("10000"), "CRC", "Est", 90)));
        }

        RecomendacionesResponseDTO resultado = service.obtenerRecomendaciones(itinerarioId, usuarioId);

        assertThat(resultado.getRecomendaciones()).hasSize(3);
    }

    // --- Aplicación de recomendaciones al itinerario (PP-92) ---

    @Test
    void aplicarRecomendacionDelegaEnComparacionAlternativasService() {
        UUID actividadId = UUID.randomUUID();
        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Senderismo en reserva", "Recorrido guiado",
                new BigDecimal("15000"), "CRC", "Reserva Verde", 85,
                "AVENTURA", "PUNTARENAS");
        ItinerarioResponseDTO esperado = new ItinerarioResponseDTO();
        esperado.setId(itinerarioId);

        when(comparacionAlternativasService.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .thenReturn(esperado);

        ItinerarioResponseDTO resultado = service.aplicarRecomendacion(itinerarioId, actividadId, request, usuarioId);

        assertThat(resultado).isSameAs(esperado);
        verify(comparacionAlternativasService).sustituirActividad(itinerarioId, actividadId, request, usuarioId);
    }
}
