package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ComparacionResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioActividadRepository;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparacionAlternativasServiceTest {

    @Mock
    private ItinerarioRepository itinerarioRepository;
    @Mock
    private ItinerarioActividadRepository itinerarioActividadRepository;
    @Mock
    private AlternativasIaClienteService alternativasIaClienteService;
    @Mock
    private ItinerarioMapper mapper;
    @Mock
    private EntityManager entityManager;

    private ComparacionAlternativasService service;

    private UUID usuarioId;
    private UUID itinerarioId;
    private UUID actividadId;

    @BeforeEach
    void setUp() {
        service = new ComparacionAlternativasService(
                itinerarioRepository, itinerarioActividadRepository,
                alternativasIaClienteService, mapper, entityManager);
        usuarioId = UUID.randomUUID();
        itinerarioId = UUID.randomUUID();
        actividadId = UUID.randomUUID();
    }

    // --- Helpers ---

    private Itinerario crearItinerarioConActividad(ItinerarioActividad actividad) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario)
                .cantidadDias(1)
                .fechaInicio(LocalDate.now())
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .id(UUID.randomUUID())
                .itinerario(itinerario)
                .numeroDia(1)
                .fecha(LocalDate.now())
                .orden(1)
                .actividades(List.of(actividad))
                .build();

        actividad.setItinerarioDia(dia);
        itinerario.setDias(List.of(dia));
        return itinerario;
    }

    private ItinerarioActividad actividadOriginal() {
        return ItinerarioActividad.builder()
                .id(actividadId)
                .nombre("Canopy Tour")
                .categoriaTuristica(InteresTuristico.AVENTURA)
                .provincia(Provincia.PUNTARENAS)
                .puntuacionAmbientalEstimada(60)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(120)
                .orden(1)
                .build();
    }

    private List<AlternativaIaDTO> alternativasIaDePrueba() {
        return List.of(
                new AlternativaIaDTO("Kayak en manglar", "Recorrido guiado",
                        new BigDecimal("15000"), "CRC", "EcoTours CR", 85),
                new AlternativaIaDTO("Senderismo volcánico", "Caminata por senderos",
                        new BigDecimal("12000"), "CRC", "Green Trails", 72)
        );
    }

    // --- Tests de flujo exitoso de obtención de alternativas con mock de IA ---

    @Test
    void obtenerAlternativasRetornaListaOrdenadaPorEcoScoreDescendente() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(alternativasIaDePrueba());

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        assertThat(resultado.getAlternativas()).hasSize(2);
        assertThat(resultado.getAlternativas().get(0).getEcoScore()).isEqualTo(85);
        assertThat(resultado.getAlternativas().get(1).getEcoScore()).isEqualTo(72);
        assertThat(resultado.getActividadOriginalNombre()).isEqualTo("Canopy Tour");
        assertThat(resultado.getEcoScoreOriginal()).isEqualTo(60);
        assertThat(resultado.getCategoriaTuristica()).isEqualTo("AVENTURA");
        assertThat(resultado.getProvincia()).isEqualTo("PUNTARENAS");
        assertThat(resultado.getMensaje()).isNull();
    }

    @Test
    void obtenerAlternativasCalculaDiferenciaAmbientalCorrectamente() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(alternativasIaDePrueba());

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        // ecoScoreOriginal = 60, alternativas: 85 y 72
        assertThat(resultado.getAlternativas().get(0).getDiferenciaAmbiental()).isEqualTo(25); // 85 - 60
        assertThat(resultado.getAlternativas().get(1).getDiferenciaAmbiental()).isEqualTo(12); // 72 - 60
    }

    @Test
    void obtenerAlternativasMarcaPrimeraComoMejorDesempeno() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(alternativasIaDePrueba());

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        assertThat(resultado.getAlternativas().get(0).isMejorDesempeno()).isTrue();
        assertThat(resultado.getAlternativas().get(1).isMejorDesempeno()).isFalse();
    }

    @Test
    void obtenerAlternativasUsaEcoScoreDefault50CuandoOriginalEsNull() {
        ItinerarioActividad actividad = actividadOriginal();
        actividad.setPuntuacionAmbientalEstimada(null);
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(List.of(new AlternativaIaDTO("Alt", "Desc",
                        new BigDecimal("10000"), "CRC", "Est", 80)));

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        assertThat(resultado.getEcoScoreOriginal()).isEqualTo(50);
        assertThat(resultado.getAlternativas().get(0).getDiferenciaAmbiental()).isEqualTo(30); // 80 - 50
    }

    // --- Tests de lista vacía cuando no hay alternativas ---

    @Test
    void obtenerAlternativasRetornaListaVaciaConMensajeCuandoIaRetornaVacia() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(List.of());

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        assertThat(resultado.getAlternativas()).isEmpty();
        assertThat(resultado.getMensaje()).isEqualTo("No existen alternativas disponibles para esta actividad.");
    }

    @Test
    void obtenerAlternativasRetornaListaVaciaConMensajeCuandoIaRetornaNull() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(alternativasIaClienteService.buscarAlternativas(eq(actividad), any()))
                .thenReturn(null);

        ComparacionResponseDTO resultado = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        assertThat(resultado.getAlternativas()).isEmpty();
        assertThat(resultado.getMensaje()).isEqualTo("No existen alternativas disponibles para esta actividad.");
    }

    // --- Tests de sustitución exitosa ---

    @Test
    void sustituirActividadReemplazaCamposCorrectamente() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Kayak en manglar", "Recorrido guiado",
                new BigDecimal("15000"), "CRC", "EcoTours CR", 85,
                "AVENTURA", "PUNTARENAS");

        ItinerarioResponseDTO expectedResponse = new ItinerarioResponseDTO();
        expectedResponse.setId(itinerarioId);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(itinerarioActividadRepository.save(any(ItinerarioActividad.class)))
                .thenReturn(actividad);
        when(mapper.toDto(itinerario)).thenReturn(expectedResponse);

        ItinerarioResponseDTO resultado = service.sustituirActividad(
                itinerarioId, actividadId, request, usuarioId);

        assertThat(resultado).isNotNull();
        assertThat(actividad.getNombre()).isEqualTo("Kayak en manglar");
        assertThat(actividad.getDescripcion()).isEqualTo("Recorrido guiado");
        assertThat(actividad.getCostoAproximado()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(actividad.getEstablecimientoRecomendado()).isEqualTo("EcoTours CR");
        assertThat(actividad.getPuntuacionAmbientalEstimada()).isEqualTo(85);
        verify(itinerarioActividadRepository).save(actividad);
    }

    @Test
    void sustituirActividadIncrementaLaVersionDelItinerario() {
        ItinerarioActividad actividad = actividadOriginal();
        Itinerario itinerario = crearItinerarioConActividad(actividad);
        assertThat(itinerario.getVersion()).isEqualTo(1); // valor por defecto del builder

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Kayak en manglar", "Recorrido guiado",
                new BigDecimal("15000"), "CRC", "EcoTours CR", 85,
                "AVENTURA", "PUNTARENAS");

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(itinerarioActividadRepository.save(any(ItinerarioActividad.class))).thenReturn(actividad);
        when(mapper.toDto(itinerario)).thenReturn(new ItinerarioResponseDTO());
        // itinerario.version es un @Version real de JPA: en producción Hibernate lo sube solo al
        // detectar el OPTIMISTIC_FORCE_INCREMENT en el flush. El mock de EntityManager no corre
        // Hibernate de verdad, así que acá se simula ese efecto explícitamente.
        doAnswer(invocation -> {
            Itinerario it = invocation.getArgument(0);
            it.setVersion(it.getVersion() + 1);
            return null;
        }).when(entityManager).lock(eq(itinerario), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT));

        service.sustituirActividad(itinerarioId, actividadId, request, usuarioId);

        verify(entityManager).lock(itinerario, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        verify(entityManager).flush();
        assertThat(itinerario.getVersion()).isEqualTo(2);
    }

    @Test
    void sustituirActividadMantieneOrdenYDia() {
        ItinerarioActividad actividad = actividadOriginal();
        actividad.setOrden(3);
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Kayak en manglar", "Recorrido guiado",
                new BigDecimal("15000"), "CRC", "EcoTours CR", 85,
                "AVENTURA", "PUNTARENAS");

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(itinerarioActividadRepository.save(any())).thenReturn(actividad);
        when(mapper.toDto(itinerario)).thenReturn(new ItinerarioResponseDTO());

        service.sustituirActividad(itinerarioId, actividadId, request, usuarioId);

        // El orden y día no deben cambiar
        assertThat(actividad.getOrden()).isEqualTo(3);
        assertThat(actividad.getItinerarioDia()).isNotNull();
    }

    // --- Tests de rechazo de sustitución con alternativa no equivalente ---

    @Test
    void sustituirActividadRechazaCuandoCategoriaNoCoincide() {
        ItinerarioActividad actividad = actividadOriginal(); // AVENTURA, PUNTARENAS
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Restaurante orgánico", "Comida local",
                new BigDecimal("20000"), "CRC", "Finca Verde", 75,
                "GASTRONOMIA_LOCAL", "PUNTARENAS"); // categoría diferente

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo(
                            "La alternativa seleccionada no es equivalente a la actividad original.");
                });

        verify(itinerarioActividadRepository, never()).save(any());
    }

    @Test
    void sustituirActividadRechazaCuandoProvinciaNoCoincide() {
        ItinerarioActividad actividad = actividadOriginal(); // AVENTURA, PUNTARENAS
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Rappel en catarata", "Descenso guiado",
                new BigDecimal("18000"), "CRC", "Adventure CR", 80,
                "AVENTURA", "ALAJUELA"); // provincia diferente

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo(
                            "La alternativa seleccionada no es equivalente a la actividad original.");
                });

        verify(itinerarioActividadRepository, never()).save(any());
    }

    @Test
    void sustituirActividadRechazaCuandoCategoriaYProvinciaNoCoinciden() {
        ItinerarioActividad actividad = actividadOriginal(); // AVENTURA, PUNTARENAS
        Itinerario itinerario = crearItinerarioConActividad(actividad);

        SustitucionRequestDTO request = new SustitucionRequestDTO(
                "Museo Nacional", "Visita cultural",
                new BigDecimal("5000"), "CRC", "Museo Nacional", 70,
                "CULTURA", "SAN_JOSE"); // ambos diferentes

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioActividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(itinerarioActividadRepository, never()).save(any());
    }
}
