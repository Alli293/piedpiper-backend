package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapperImpl;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoGeneracionIA;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EventoReconocimientoCodigo;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcoRutaItinerarioServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private PreferenciasViajeRepository preferenciasViajeRepository;
    @Mock
    private ItinerarioRepository itinerarioRepository;
    @Mock
    private ItinerarioIaClienteService itinerarioIaClienteService;
    @Mock
    private ItinerarioCuotaService itinerarioCuotaService;
    @Mock
    private EventoReconocimientoService eventoReconocimientoService;
    @Mock
    private PriorizacionAmbientalService priorizacionAmbientalService;
    @Mock
    private com.piedpiper.carbonhub.empresa.repository.EmpresaRepository empresaRepository;

    private ItinerarioMapper mapper;
    private EcoRutaItinerarioService service;

    @BeforeEach
    void configurar() {
        mapper = new ItinerarioMapperImpl();
        service = new EcoRutaItinerarioService(
                preferenciasViajeRepository, itinerarioRepository,
                itinerarioIaClienteService, itinerarioCuotaService, eventoReconocimientoService,
                priorizacionAmbientalService,
                mock(IndicadorAmbientalClient.class),
                mock(ImaClient.class),
                mock(BenchmarkClient.class),
                mock(PuntuacionAmbientalCalculator.class),
                empresaRepository, mapper);

        // Stub default para empresaRepository usado en extraerEstablecimientosRankeados
        lenient().when(empresaRepository.findByEstado(any()))
                .thenReturn(java.util.List.of());
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("ana.perez@example.com")
                .nombre("Ana")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private PreferenciasViaje preferencias() {
        return PreferenciasViaje.builder()
                .id(UUID.randomUUID())
                .usuario(usuario())
                .cantidadDias(2)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .intereses(new ArrayList<>(List.of(InteresTuristico.NATURALEZA)))
                .buscarCercaDeMi(false)
                .requiereHospedaje(false)
                .itinerarioGeneracionContador(0)
                .build();
    }

    private ActividadIaDTO actividadValida() {
        return new ActividadIaDTO(
                "Caminata por puentes colgantes", "Recorrido guiado", "09:00", 150,
                new BigDecimal("13000"), "CRC", "Reserva Selvatura", "PUNTARENAS", 80, "NATURALEZA");
    }

    private ItinerarioIaResponseDTO respuestaValida(int dias) {
        List<DiaIaDTO> listaDias = new ArrayList<>();
        for (int i = 1; i <= dias; i++) {
            listaDias.add(new DiaIaDTO(i, "2026-08-0" + i, List.of(actividadValida())));
        }
        return new ItinerarioIaResponseDTO(listaDias, 82);
    }

    @Test
    void sinPreferenciasGuardadasLanza404() {
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(itinerarioIaClienteService, never()).generar(any(), anyInt());
    }

    @Test
    void fechaInicioVencidaLanzaError() {
        PreferenciasViaje preferencias = preferencias();
        preferencias.setFechaInicio(LocalDate.now().minusDays(1));
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));

        assertThatThrownBy(() -> service.generar(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(itinerarioIaClienteService, never()).generar(any(), anyInt());
        // La fecha vencida no debe consumir cuota: se valida antes de reservarGeneracion.
        verify(itinerarioCuotaService, never()).reservarGeneracion(any());
    }

    @Test
    void generacionExitosaPersisteYDevuelveElItinerario() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getEstado()).isEqualTo("GENERADO");
        assertThat(response.isGeneradoParcial()).isFalse();
        assertThat(response.getDias()).hasSize(2);
        assertThat(response.getDias().get(0).getActividades()).hasSize(1);
        assertThat(response.getDias().get(0).getActividades().get(0).getProvincia()).isEqualTo("PUNTARENAS");
    }

    @Test
    void itinerarioParcialMarcaElFlagCorrecto() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(1), ResultadoValidacionItinerario.VALIDO_PARCIAL));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.isGeneradoParcial()).isTrue();
        assertThat(response.getMensajeParcial()).isNotBlank();
    }

    @Test
    void errorDePersistenciaLanza500ControladoYNoDejaFilaHuerfana() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo de conexion"));

        assertThatThrownBy(() -> service.generar(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));

        verify(eventoReconocimientoService, never()).generar(any(), any());
    }

    @Test
    void eventoDeReconocimientoSoloSeDisparaTrasExito() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        service.generar(USUARIO_ID);

        // Sin sincronizacion de transaccion activa en el test, el evento se emite de inmediato.
        verify(eventoReconocimientoService).generar(
                USUARIO_ID, EventoReconocimientoCodigo.PRIMER_ITINERARIO_GENERADO.getCodigo());
    }

    @Test
    void quintoItinerarioDisparaElEventoDeCincoGenerados() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(5L);

        service.generar(USUARIO_ID);

        verify(eventoReconocimientoService).generar(
                USUARIO_ID, EventoReconocimientoCodigo.CINCO_ITINERARIOS_GENERADOS.getCodigo());
    }

    // La lógica de la ventana/contador de cuota en sí se prueba en ItinerarioCuotaServiceTest
    // (unitario) y en ItinerarioCuotaServiceIntegrationTest (que confirma con una transacción
    // real que el incremento sobrevive el rollback del llamador). Acá solo nos interesa que
    // EcoRutaItinerarioService delegue correctamente y reaccione bien a lo que el bean de cuota
    // decida.

    @Test
    void reservaLaCuotaAntesDeLlamarALaIA() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        service.generar(USUARIO_ID);

        verify(itinerarioCuotaService).reservarGeneracion(USUARIO_ID);
    }

    @Test
    void cuotaExcedidaLanza429SinLlamarALaIAYSinGuardarNada() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        doThrow(ApiException.itinerarioGeneracionesExcedidas())
                .when(itinerarioCuotaService).reservarGeneracion(USUARIO_ID);

        assertThatThrownBy(() -> service.generar(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(itinerarioIaClienteService, never()).generar(any(), anyInt());
        verify(itinerarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaLlamadaFallidaALaIANoRevierteLaCuotaYaReservada() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2)))
                .thenThrow(ApiException.itinerarioGeneracionTimeout());

        assertThatThrownBy(() -> service.generar(USUARIO_ID)).isInstanceOf(ApiException.class);

        // La cuota ya se reservó (y se guardó en su propia transacción REQUIRES_NEW) antes de
        // llamar a la IA — el fallo posterior de la IA no debe deshacerla.
        verify(itinerarioCuotaService).reservarGeneracion(USUARIO_ID);
    }

    // --- obtener ---

    @Test
    void obtenerConItinerarioPropioDevuelveElDto() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario())
                .cantidadDias(2)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .estado(com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario.GENERADO)
                .version(1)
                .fechaGeneracion(Instant.now())
                .dias(List.of())
                .build();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));

        ItinerarioResponseDTO response = service.obtener(itinerarioId, USUARIO_ID);

        assertThat(response.getId()).isEqualTo(itinerarioId);
    }

    @Test
    void obtenerConItinerarioInexistenteLanza404() {
        UUID itinerarioId = UUID.randomUUID();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(itinerarioId, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void obtenerConItinerarioDeOtroUsuarioLanzaElMismo404() {
        // findByIdAndUsuario_Id ya filtra por dueño: un itinerario ajeno se comporta
        // exactamente igual que uno inexistente (mismo 404), para no filtrar por enumeración de IDs.
        UUID itinerarioId = UUID.randomUUID();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(itinerarioId, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
