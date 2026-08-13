package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapperImpl;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ConversacionContextoDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.FiltrarItinerariosRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.MensajeConversacionDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PaginaItinerariosResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ResultadoPriorizacion;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;
import com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoGeneracionIA;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoRefinamientoIA;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private EcoScoreService ecoScoreService;
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
                ecoScoreService,
                mock(IndicadorAmbientalClient.class),
                mock(ImaClient.class),
                mock(BenchmarkClient.class),
                mock(PuntuacionAmbientalCalculator.class),
                empresaRepository, mapper, new com.fasterxml.jackson.databind.ObjectMapper());

        // Stub default para empresaRepository, usado tanto en construirActividad/EcoScore
        // (matching de empresa) como en el prompt de la IA (obtenerNombresEstablecimientosVerificados)
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
        // priorizacionAmbientalService no fue stubbeado (devuelve null): la enriquecida debe
        // degradar a lista vacía, nunca null, o el frontend revienta al leer .length.
        assertThat(response.getEstablecimientosEvaluados()).isNotNull().isEmpty();
        // Sin empresas activas registradas (stub default), ninguna actividad debe quedar
        // vinculada a una empresa.
        assertThat(response.getDias().get(0).getActividades().get(0).getEmpresaId()).isNull();
    }

    @Test
    void establecimientoRecomendadoQueCoincideConEmpresaActivaQuedaVinculado() {
        UUID empresaId = UUID.randomUUID();
        Empresa empresaRegistrada = Empresa.builder()
                .id(empresaId)
                .nombreEmpresa("Reserva Selvatura")
                .build();
        when(empresaRepository.findByEstado(any())).thenReturn(List.of(empresaRegistrada));

        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getDias().get(0).getActividades().get(0).getEmpresaId()).isEqualTo(empresaId);
    }

    @Test
    void establecimientoRecomendadoSinCoincidenciaNoQuedaVinculado() {
        Empresa otraEmpresa = Empresa.builder()
                .id(UUID.randomUUID())
                .nombreEmpresa("Café Britt")
                .build();
        when(empresaRepository.findByEstado(any())).thenReturn(List.of(otraEmpresa));

        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getDias().get(0).getActividades().get(0).getEmpresaId()).isNull();
    }

    @Test
    void nombreDeEmpresaMuyCortoNoActivaMatchingPorContains() {
        // "Sel" es substring de "Reserva Selvatura" (el establecimientoRecomendado de
        // actividadValida()), pero un nombre de empresa tan corto actuaría como comodín si se
        // acepta por `contains` — no debe quedar vinculado.
        Empresa nombreCorto = Empresa.builder()
                .id(UUID.randomUUID())
                .nombreEmpresa("Sel")
                .build();
        when(empresaRepository.findByEstado(any())).thenReturn(List.of(nombreCorto));

        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getDias().get(0).getActividades().get(0).getEmpresaId()).isNull();
    }

    @Test
    void establecimientosEvaluadosResuelveEmpresaIdYPuntuacionDelMismoRankeadoConNombresDuplicados() {
        // Bug real corregido en PP-95: construirEstablecimientosEvaluados deduplicaba por nombre
        // usando dos fuentes distintas (una para elegir el EstablecimientoRankeado, otra --ya
        // filtrada por detalleAmbiental != null-- para el mapa de puntuaciones), así que con dos
        // rankeados de mismo nombre pero distinto empresaId/puntuación cada una podía "ganar" un
        // objeto distinto: el score de uno terminaba mostrado junto al empresaId del otro. Ahora
        // debe deduplicar UNA sola vez y tomar empresaId + puntuación del mismo objeto elegido.
        UUID empresaCorrecta = UUID.randomUUID();
        UUID empresaIncorrecta = UUID.randomUUID();

        PuntuacionAmbientalResponseDTO puntuacionCorrecta =
                new PuntuacionAmbientalResponseDTO(new BigDecimal("90.0"), new BigDecimal("40"),
                        new BigDecimal("30"), new BigDecimal("20"), 5);
        PuntuacionAmbientalResponseDTO puntuacionIncorrecta =
                new PuntuacionAmbientalResponseDTO(new BigDecimal("30.0"), new BigDecimal("10"),
                        new BigDecimal("10"), new BigDecimal("10"), 0);

        EstablecimientoRankeado primeraOcurrencia = new EstablecimientoRankeado(
                empresaCorrecta, "Reserva Selvatura", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                puntuacionCorrecta);
        EstablecimientoRankeado segundaOcurrencia = new EstablecimientoRankeado(
                empresaIncorrecta, "Reserva Selvatura", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                puntuacionIncorrecta);

        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);
        when(priorizacionAmbientalService.aplicarPriorizacion(any(), any(), any()))
                .thenReturn(new ResultadoPriorizacion(List.of(primeraOcurrencia, segundaOcurrencia), 2, 0));

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getEstablecimientosEvaluados()).hasSize(1);
        var establecimiento = response.getEstablecimientosEvaluados().get(0);
        assertThat(establecimiento.getNombreEstablecimiento()).isEqualTo("Reserva Selvatura");
        // empresaId y puntuacionAmbiental deben venir del MISMO EstablecimientoRankeado (el
        // primero, según el orden de dedupe) -- nunca la combinación score-de-uno/empresaId-de-otro.
        assertThat(establecimiento.getEmpresaId()).isEqualTo(empresaCorrecta);
        assertThat(establecimiento.getPuntuacionAmbiental().getPuntuacionTotal())
                .isEqualByComparingTo(new BigDecimal("90.0"));
    }

    @Test
    void ecoScoreCalculadoSePersisteYSeIncluyeEnLaRespuesta() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);
        when(priorizacionAmbientalService.aplicarPriorizacion(any(), any(), any()))
                .thenAnswer(i -> new ResultadoPriorizacion(i.getArgument(0), 1, 0));
        when(ecoScoreService.calcular(any(), any()))
                .thenReturn(new EcoScoreResultado(new BigDecimal("68.0"), ClasificacionAmbiental.BUENA, false));

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getEcoScore()).isEqualByComparingTo(new BigDecimal("68.0"));
        assertThat(response.getClasificacionAmbiental()).isEqualTo("BUENA");
        assertThat(response.isEcoScoreParcial()).isFalse();
        assertThat(response.getEcoScoreCalculadoEn()).isNotNull();
        verify(itinerarioRepository).save(any(Itinerario.class));
    }

    @Test
    void sinDatosParaEcoScoreDejaLosCamposEnNullYNoLosPersiste() {
        PreferenciasViaje preferencias = preferencias();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(preferencias));
        when(itinerarioIaClienteService.generar(any(), eq(2))).thenReturn(
                new ResultadoGeneracionIA(respuestaValida(2), ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));
        when(itinerarioRepository.countByUsuario_Id(USUARIO_ID)).thenReturn(1L);
        // priorizacionAmbientalService.aplicarPriorizacion no se stubbea: devuelve null por defecto,
        // por lo que calcularYPersistirEcoScore no llega a invocar a ecoScoreService (mismo camino
        // de degradación graciosa que enriquecerConPuntuacionAmbiental).

        ItinerarioResponseDTO response = service.generar(USUARIO_ID);

        assertThat(response.getEcoScore()).isNull();
        assertThat(response.getEcoScoreCalculadoEn()).isNull();
        verify(itinerarioRepository, never()).save(any());
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
        // Sin actividades no hay establecimientos que evaluar: debe ser lista vacía, nunca null.
        assertThat(response.getEstablecimientosEvaluados()).isNotNull().isEmpty();
    }

    @Test
    void obtenerDevuelveElEcoScorePersistidoSinRecalcular() {
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
                .ecoScore(new BigDecimal("68.0"))
                .clasificacionAmbiental(ClasificacionAmbiental.BUENA)
                .ecoScoreParcial(false)
                .ecoScoreCalculadoEn(Instant.now())
                .dias(List.of())
                .build();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));

        ItinerarioResponseDTO response = service.obtener(itinerarioId, USUARIO_ID);

        assertThat(response.getEcoScore()).isEqualByComparingTo(new BigDecimal("68.0"));
        assertThat(response.getClasificacionAmbiental()).isEqualTo("BUENA");
        verify(ecoScoreService, never()).calcular(any(), any());
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

    // --- refinar (PP-88) ---

    private Itinerario itinerarioExistente(UUID itinerarioId) {
        return Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario())
                .cantidadDias(2)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .estado(EstadoItinerario.GENERADO)
                .version(1)
                .fechaGeneracion(Instant.now())
                .dias(new ArrayList<>())
                .build();
    }

    private RefinamientoItinerarioRequestDTO mensaje(String texto) {
        return new RefinamientoItinerarioRequestDTO(texto, null);
    }

    @Test
    void refinarConAjusteExitosoModificaEIncrementaLaVersion() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Agregué una caminata al aire libre.", null, respuestaValida(2));
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));

        RefinamientoItinerarioResponseDTO response = service.refinar(
                itinerarioId, USUARIO_ID, mensaje("Quiero más actividades al aire libre."));

        assertThat(response.getItinerario().getVersion()).isEqualTo(2);
        assertThat(response.getItinerario().getDias()).hasSize(2);
        assertThat(response.getRespuestaAsistente()).isEqualTo("Agregué una caminata al aire libre.");
        assertThat(response.getHistorialMensajes()).hasSize(2);
        assertThat(response.getHistorialMensajes().get(0).getRol()).isEqualTo("USUARIO");
        assertThat(response.getHistorialMensajes().get(1).getRol()).isEqualTo("ASISTENTE");
        assertThat(response.getActividadParaComparar()).isNull();
    }

    @Test
    void refinarConSolicitudAmbiguaNoModificaElItinerario() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                true, "¿A qué actividad te referís?", null, null);
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        RefinamientoItinerarioResponseDTO response = service.refinar(
                itinerarioId, USUARIO_ID, mensaje("Cámbiala."));

        assertThat(response.getItinerario().getVersion()).isEqualTo(1);
        assertThat(response.getRespuestaAsistente()).isEqualTo("¿A qué actividad te referís?");
        verify(itinerarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void refinarConSolicitudDeAlternativasNoModificaYDevuelveLaActividad() {
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Te muestro otras opciones de hospedaje.", actividadId, null);
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        RefinamientoItinerarioResponseDTO response = service.refinar(
                itinerarioId, USUARIO_ID, mensaje("¿Hay opciones de hospedaje con menor huella?"));

        assertThat(response.getItinerario().getVersion()).isEqualTo(1);
        assertThat(response.getActividadParaComparar()).isEqualTo(actividadId);
        verify(itinerarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void refinarConTimeoutDeIaNoPersisteNadaYPropagaElError() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        when(itinerarioIaClienteService.refinar(any(), eq(2)))
                .thenThrow(ApiException.itinerarioRefinamientoFallido());

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, mensaje("Quiero más playas.")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));

        verify(itinerarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void refinarConErrorDePersistenciaLanza500ConMensajeExacto() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Listo.", null, respuestaValida(2));
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo de conexion"));

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, mensaje("Quiero más playas.")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(ex.getMessage()).isEqualTo(
                            "No fue posible guardar los cambios del itinerario. Intenta nuevamente.");
                });
    }

    @Test
    void refinarConItinerarioInexistenteOAjenoLanza403ConMensajeExacto() {
        // findByIdAndUsuario_Id ya filtra por dueño: refinar() hace esta única consulta (ya no hay
        // un chequeo de ownership duplicado en el controlador) y trata "no existe" e "es de otro
        // usuario" igual, con 403 -- coincide con el AC de PP-88 para itinerario ajeno.
        UUID itinerarioId = UUID.randomUUID();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, mensaje("Quiero más playas.")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("No tienes permiso para modificar este itinerario.");
                });

        verify(itinerarioIaClienteService, never()).refinar(any(), anyInt());
    }

    // --- poda del historial en el prompt (revisión de nanoulloa en el PR #86) ---

    @Test
    void refinarConHistorialLargoSoloUsaLosUltimosTurnosEnElPrompt() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                true, "¿Podrías ser más específico?", null, null);
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        // 15 turnos (30 mensajes) -- muy por encima de MAX_TURNOS_HISTORIAL_EN_PROMPT (10).
        List<MensajeConversacionDTO> historialLargo = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            historialLargo.add(new MensajeConversacionDTO("USUARIO", "mensaje-viejo-" + i));
            historialLargo.add(new MensajeConversacionDTO("ASISTENTE", "respuesta-vieja-" + i));
        }
        historialLargo.set(historialLargo.size() - 1, new MensajeConversacionDTO("ASISTENTE", "el-mas-reciente"));

        RefinamientoItinerarioRequestDTO request = new RefinamientoItinerarioRequestDTO(
                "Cámbialo.",
                new ConversacionContextoDTO(itinerarioId, historialLargo, 1));

        service.refinar(itinerarioId, USUARIO_ID, request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(itinerarioIaClienteService).refinar(promptCaptor.capture(), eq(2));
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("el-mas-reciente");
        assertThat(prompt).doesNotContain("mensaje-viejo-1\n");
        assertThat(prompt).doesNotContain("respuesta-vieja-1\n");
    }

    // --- moneda preferida en el prompt de refinar() (revisión de carias03 en el PR #86) ---

    @Test
    void refinarUsaLaMonedaPreferidaDelUsuarioEnElPrompt() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        itinerario.getUsuario().setMoneda("USD");
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                true, "¿Podrías ser más específico?", null, null);
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        service.refinar(itinerarioId, USUARIO_ID, mensaje("Cámbiala."));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(itinerarioIaClienteService).refinar(promptCaptor.capture(), eq(2));
        assertThat(promptCaptor.getValue()).contains("USD");
    }

    @Test
    void refinarUsaCrcPorDefectoCuandoElUsuarioNoTieneMonedaConfigurada() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        itinerario.getUsuario().setMoneda(null);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                true, "¿Podrías ser más específico?", null, null);
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));

        service.refinar(itinerarioId, USUARIO_ID, mensaje("Cámbiala."));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(itinerarioIaClienteService).refinar(promptCaptor.capture(), eq(2));
        assertThat(promptCaptor.getValue()).contains("CRC");
    }

    // --- cuota de mensajes de refinamiento (Major de Arielajr15 en el PR #86) ---

    @Test
    void refinarRespetaLaCuotaDeMensajesDeRefinamiento() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        org.mockito.Mockito.doThrow(ApiException.itinerarioRefinamientosExcedidos())
                .when(itinerarioCuotaService).reservarRefinamiento(USUARIO_ID);

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, mensaje("Quiero más playas.")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(itinerarioIaClienteService, never()).refinar(any(), anyInt());
    }

    // --- validación de itinerarioId/versionItinerario del contexto (Major de Arielajr15 en el PR #86) ---

    @Test
    void refinarConContextoDeOtroItinerarioLanza400() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));

        var contexto = new ConversacionContextoDTO(UUID.randomUUID(), List.of(), null);
        var request = new RefinamientoItinerarioRequestDTO("Quiero más playas.", contexto);

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(itinerarioIaClienteService, never()).refinar(any(), anyInt());
    }

    @Test
    void refinarConVersionDesactualizadaLanza409() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));

        var contexto = new ConversacionContextoDTO(itinerarioId, List.of(), itinerario.getVersion() + 1);
        var request = new RefinamientoItinerarioRequestDTO("Quiero más playas.", contexto);

        assertThatThrownBy(() -> service.refinar(itinerarioId, USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(itinerarioIaClienteService, never()).refinar(any(), anyInt());
    }

    // --- VALIDO_PARCIAL en refinar() (Major de carias03 en el PR #86) ---

    @Test
    void refinarConRespuestaParcialMarcaGeneradoParcialConMensaje() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Solo pude ajustar un día.", null, respuestaValida(1));
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_PARCIAL));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));

        RefinamientoItinerarioResponseDTO response = service.refinar(
                itinerarioId, USUARIO_ID, mensaje("Ajustá todo el itinerario."));

        assertThat(response.getItinerario().isGeneradoParcial()).isTrue();
        assertThat(response.getItinerario().getMensajeParcial()).isNotBlank();
    }

    @Test
    void refinarConRespuestaCompletaNoQuedaMarcadoComoParcial() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioExistente(itinerarioId);
        itinerario.setGeneradoParcial(true);
        itinerario.setMensajeParcial("parcial de una generación anterior");
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));
        RefinamientoIaResponseDTO respuestaIa = new RefinamientoIaResponseDTO(
                false, "Listo.", null, respuestaValida(2));
        when(itinerarioIaClienteService.refinar(any(), eq(2))).thenReturn(
                new ResultadoRefinamientoIA(respuestaIa, ResultadoValidacionItinerario.VALIDO_COMPLETO));
        when(itinerarioRepository.saveAndFlush(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));

        RefinamientoItinerarioResponseDTO response = service.refinar(
                itinerarioId, USUARIO_ID, mensaje("Quiero más actividades al aire libre."));

        assertThat(response.getItinerario().isGeneradoParcial()).isFalse();
        assertThat(response.getItinerario().getMensajeParcial()).isNull();
    }

    // --- listar (PP-89) ---

    private Itinerario itinerarioResumen(UUID id) {
        return Itinerario.builder()
                .id(id)
                .usuario(usuario())
                .cantidadDias(3)
                .fechaInicio(LocalDate.now().plusDays(5))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .estado(EstadoItinerario.GENERADO)
                .version(1)
                .ecoScore(new BigDecimal("70.0"))
                .clasificacionAmbiental(ClasificacionAmbiental.BUENA)
                .fechaGeneracion(Instant.now())
                .dias(List.of())
                .build();
    }

    @Test
    void listarDevuelveElContenidoPaginadoConProvinciasPorItinerario() {
        Itinerario itin1 = itinerarioResumen(UUID.randomUUID());
        Itinerario itin2 = itinerarioResumen(UUID.randomUUID());
        var pagina = new PageImpl<>(List.of(itin1, itin2),
                PageRequest.of(0, EcoRutaItinerarioService.TAMANIO_PAGINA,
                        Sort.by(Sort.Direction.DESC, "fechaGeneracion")),
                2);
        when(itinerarioRepository.findByUsuario_Id(eq(USUARIO_ID), any())).thenReturn(pagina);
        var filaProvincia = org.mockito.Mockito.mock(ItinerarioRepository.ProvinciaPorItinerario.class);
        when(filaProvincia.getItinerarioId()).thenReturn(itin1.getId());
        when(filaProvincia.getProvincia())
                .thenReturn(com.piedpiper.carbonhub.ecoruta.models.enums.Provincia.PUNTARENAS);
        when(itinerarioRepository.findProvinciasVisitadasPorItinerarios(List.of(itin1.getId(), itin2.getId())))
                .thenReturn(List.of(filaProvincia));

        PaginaItinerariosResponseDTO respuesta = service.listar(USUARIO_ID, new FiltrarItinerariosRequestDTO(1));

        assertThat(respuesta.getContenido()).hasSize(2);
        assertThat(respuesta.getContenido().get(0).getProvinciasVisitadas()).containsExactly("PUNTARENAS");
        assertThat(respuesta.getContenido().get(1).getProvinciasVisitadas()).isEmpty();
        assertThat(respuesta.getTotalResultados()).isEqualTo(2);
        assertThat(respuesta.getPaginaActual()).isEqualTo(1);
        assertThat(respuesta.getTamanioPagina()).isEqualTo(EcoRutaItinerarioService.TAMANIO_PAGINA);
    }

    @Test
    void listarConErrorDeBdLanza500ConMensajeExacto() {
        when(itinerarioRepository.findByUsuario_Id(eq(USUARIO_ID), any()))
                .thenThrow(new DataAccessResourceFailureException("fallo de conexion"));

        assertThatThrownBy(() -> service.listar(USUARIO_ID, new FiltrarItinerariosRequestDTO(1)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(ex.getMessage()).isEqualTo("No fue posible recuperar la información solicitada.");
                });
    }

    // --- eliminar (PP-89, fuera del AC — pedido explícito del equipo) ---

    @Test
    void eliminarConItinerarioPropioLoBorra() {
        UUID itinerarioId = UUID.randomUUID();
        Itinerario itinerario = itinerarioResumen(itinerarioId);
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID))
                .thenReturn(Optional.of(itinerario));

        service.eliminar(itinerarioId, USUARIO_ID);

        verify(itinerarioRepository).delete(itinerario);
    }

    @Test
    void eliminarConItinerarioInexistenteOAjenoLanza403ConMensajeExacto() {
        UUID itinerarioId = UUID.randomUUID();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(itinerarioId, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo(
                            "No tienes permiso para modificar este itinerario.");
                });

        verify(itinerarioRepository, never()).delete(any());
    }
}
