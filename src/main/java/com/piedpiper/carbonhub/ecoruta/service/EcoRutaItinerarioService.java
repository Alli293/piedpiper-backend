package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ConversacionContextoDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoEcoScoreResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.FiltrarItinerariosRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.HistorialEcoRutaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResumenResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.MensajeConversacionDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PaginaItinerariosResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RefinamientoItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ResultadoPriorizacion;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoGeneracionIA;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoRefinamientoIA;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EventoReconocimientoCodigo;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EcoRutaItinerarioService {

    private static final Logger log = LoggerFactory.getLogger(EcoRutaItinerarioService.class);

    /** Fijado para esta pantalla; ver {@code SolicitudAuditoriaListadoService} para el mismo patrón. */
    static final int TAMANIO_PAGINA = 12;

    /** Tope para que {@code (pagina - 1) * tamanio} nunca desborde el {@code int} que usa Spring Data. */
    static final int PAGINA_MAXIMA = Integer.MAX_VALUE / TAMANIO_PAGINA;

    private final PreferenciasViajeRepository preferenciasViajeRepository;
    private final ItinerarioRepository itinerarioRepository;
    private final ItinerarioIaClienteService itinerarioIaClienteService;
    private final ItinerarioCuotaService itinerarioCuotaService;
    private final EventoReconocimientoService eventoReconocimientoService;
    private final PriorizacionAmbientalService priorizacionAmbientalService;
    private final EcoScoreService ecoScoreService;
    private final IndicadorAmbientalClient indicadorClient;
    private final ImaClient imaClient;
    private final BenchmarkClient benchmarkClient;
    private final PuntuacionAmbientalCalculator puntuacionCalculator;
    private final EmpresaRepository empresaRepository;
    private final ItinerarioMapper mapper;

    public EcoRutaItinerarioService(PreferenciasViajeRepository preferenciasViajeRepository,
                                    ItinerarioRepository itinerarioRepository,
                                    ItinerarioIaClienteService itinerarioIaClienteService,
                                    ItinerarioCuotaService itinerarioCuotaService,
                                    EventoReconocimientoService eventoReconocimientoService,
                                    PriorizacionAmbientalService priorizacionAmbientalService,
                                    EcoScoreService ecoScoreService,
                                    IndicadorAmbientalClient indicadorClient,
                                    ImaClient imaClient,
                                    BenchmarkClient benchmarkClient,
                                    PuntuacionAmbientalCalculator puntuacionCalculator,
                                    EmpresaRepository empresaRepository,
                                    ItinerarioMapper mapper) {
        this.preferenciasViajeRepository = preferenciasViajeRepository;
        this.itinerarioRepository = itinerarioRepository;
        this.itinerarioIaClienteService = itinerarioIaClienteService;
        this.itinerarioCuotaService = itinerarioCuotaService;
        this.eventoReconocimientoService = eventoReconocimientoService;
        this.priorizacionAmbientalService = priorizacionAmbientalService;
        this.ecoScoreService = ecoScoreService;
        this.indicadorClient = indicadorClient;
        this.imaClient = imaClient;
        this.benchmarkClient = benchmarkClient;
        this.puntuacionCalculator = puntuacionCalculator;
        this.empresaRepository = empresaRepository;
        this.mapper = mapper;
    }

    /**
     * A propósito, NO {@code @Transactional}: la llamada a Gemini puede tardar hasta 10s y no debe
     * retener una conexión del pool mientras espera. Cada paso con acceso a datos administra su
     * propia transacción corta ({@link ItinerarioCuotaService#reservarGeneracion} en una separada
     * vía {@code REQUIRES_NEW}, la lectura de preferencias/historial y el guardado final vía las
     * transacciones implícitas de Spring Data por método).
     */
    public ItinerarioResponseDTO generar(UUID usuarioId) {
        PreferenciasViaje preferencias = preferenciasViajeRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No has completado tus preferencias de viaje todavía."));

        if (preferencias.getFechaInicio().isBefore(LocalDate.now())) {
            throw ApiException.datosInvalidos(
                    "La fecha de inicio de tu viaje ya pasó. Actualiza tus preferencias antes de generar el itinerario.");
        }

        itinerarioCuotaService.reservarGeneracion(usuarioId);

        HistorialEcoRutaDTO historial = construirHistorial(usuarioId);
        String contexto = construirContextoTuristico(preferencias, historial);

        ResultadoGeneracionIA resultadoIA = itinerarioIaClienteService.generar(
                contexto, preferencias.getCantidadDias());

        Itinerario itinerario = construirItinerario(preferencias, resultadoIA);

        try {
            itinerarioRepository.saveAndFlush(itinerario);
        } catch (DataAccessException e) {
            log.error("Error inesperado al guardar el itinerario del usuario {}", usuarioId, e);
            throw ApiException.errorInterno(
                    "No fue posible guardar el itinerario. Intenta nuevamente.");
        }

        // Aplicar priorización ambiental: calcula scores y persiste registros de auditoría
        // dentro de la misma transacción (@Transactional)
        ResultadoPriorizacion resultadoPriorizacion = aplicarPriorizacionAmbiental(itinerario, usuarioId);

        // Calcular y persistir el EcoScore del itinerario (Req PP-91)
        calcularYPersistirEcoScore(itinerario, resultadoPriorizacion);

        registrarEventoDeReconocimientoTrasCommit(usuarioId);

        ItinerarioResponseDTO responseDTO = mapper.toDto(itinerario);
        responseDTO.setEstablecimientosEvaluados(List.of());

        // Enriquecer la respuesta con puntuaciones ambientales por actividad/establecimiento
        enriquecerConPuntuacionAmbiental(responseDTO, resultadoPriorizacion);

        return responseDTO;
    }

    /**
     * Devuelve un itinerario por ID si pertenece al usuario. El controlador valida la propiedad
     * antes de llegar aquí (403 si el itinerario no es suyo); este método solo maneja el caso
     * "no encontrado" con 404. Enriquece la respuesta con puntuaciones ambientales calculadas
     * al vuelo (sin persistir registros de auditoría).
     */
    @Transactional(readOnly = true)
    public ItinerarioResponseDTO obtener(UUID itinerarioId, UUID usuarioId) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No fue posible encontrar el itinerario solicitado."));
        ItinerarioResponseDTO responseDTO = mapper.toDto(itinerario);
        responseDTO.setEstablecimientosEvaluados(List.of());

        // Enriquecer con puntuaciones ambientales calculadas al vuelo (sin persistir)
        enriquecerConPuntuacionesCalculadas(responseDTO, itinerario);

        return responseDTO;
    }

    /**
     * Verifica si un itinerario pertenece al usuario indicado. Utilizado por el controlador
     * para validar la propiedad antes de permitir operaciones sobre un itinerario (Req 4.3).
     */
    @Transactional(readOnly = true)
    public boolean perteneceAlUsuario(UUID itinerarioId, UUID usuarioId) {
        return itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId).isPresent();
    }

    /**
     * Listado paginado de "Mis itinerarios" (PP-89), más recientes primero. Una página fuera de
     * rango (incluida la que desborda el límite de Spring Data) cae en un resultado vacío, no en
     * un error — es un parámetro de paginación de la propia pantalla, no un dato que el usuario
     * escriba a mano.
     */
    @Transactional(readOnly = true)
    public PaginaItinerariosResponseDTO listar(UUID usuarioId, FiltrarItinerariosRequestDTO filtros) {
        try {
            Page<Itinerario> pagina = itinerarioRepository.findByUsuario_Id(usuarioId, paginaDe(filtros.getPagina()));
            List<ItinerarioResumenResponseDTO> contenido = pagina.getContent().stream()
                    .map(this::aResumen)
                    .toList();
            return new PaginaItinerariosResponseDTO(
                    contenido, pagina.getTotalElements(), pagina.getNumber() + 1,
                    pagina.getTotalPages(), TAMANIO_PAGINA);
        } catch (DataAccessException e) {
            log.error("Error al listar los itinerarios del usuario {}", usuarioId, e);
            throw ApiException.errorInterno("No fue posible recuperar la información solicitada.");
        }
    }

    /**
     * Elimina un itinerario del usuario. El controlador ya validó ownership antes de llegar acá
     * (403 si el itinerario es ajeno); este método solo maneja el caso "no encontrado" con 404.
     */
    @Transactional
    public void eliminar(UUID itinerarioId, UUID usuarioId) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "El itinerario solicitado no existe o ya no está disponible."));
        itinerarioRepository.delete(itinerario);
    }

    private ItinerarioResumenResponseDTO aResumen(Itinerario itinerario) {
        ItinerarioResumenResponseDTO resumen = mapper.toResumenDto(itinerario);
        resumen.setProvinciasVisitadas(
                itinerarioRepository.findProvinciasVisitadasByItinerarioId(itinerario.getId()).stream()
                        .map(Enum::name)
                        .toList());
        return resumen;
    }

    private static Pageable paginaDe(Integer pagina) {
        int solicitada = pagina == null ? 1 : Math.clamp(pagina, 1, PAGINA_MAXIMA);
        return PageRequest.of(solicitada - 1, TAMANIO_PAGINA,
                Sort.by(Sort.Direction.DESC, "fechaGeneracion"));
    }

    /**
     * Interpreta un mensaje libre del chat de refinamiento (PP-88) y, si corresponde, regenera
     * parcialmente el itinerario. A propósito NO {@code @Transactional} por la misma razón que
     * {@link #generar}: la llamada a Gemini puede tardar hasta 10s. El controlador ya validó
     * ownership antes de llegar acá (403), así que este método solo maneja el caso "no encontrado"
     * con 404 — no debería ocurrir en la práctica dado ese chequeo previo, pero se cubre por si
     * el itinerario fue borrado entre la validación y esta llamada.
     */
    public RefinamientoItinerarioResponseDTO refinar(UUID itinerarioId, UUID usuarioId,
                                                       RefinamientoItinerarioRequestDTO request) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No fue posible encontrar el itinerario solicitado."));

        List<MensajeConversacionDTO> historialPrevio = obtenerHistorialPrevio(request.getContextoConversacional());

        String contexto = construirContextoRefinamiento(itinerario, historialPrevio, request.getMensajeUsuario());

        ResultadoRefinamientoIA resultadoIA = itinerarioIaClienteService.refinar(
                contexto, itinerario.getCantidadDias());
        RefinamientoIaResponseDTO respuesta = resultadoIA.respuesta();

        List<MensajeConversacionDTO> historialActualizado = new ArrayList<>(historialPrevio);
        historialActualizado.add(new MensajeConversacionDTO("USUARIO", request.getMensajeUsuario()));
        historialActualizado.add(new MensajeConversacionDTO("ASISTENTE", respuesta.getRespuestaTexto()));

        if (respuesta.isRequiereAclaracion() || respuesta.getItinerarioActualizado() == null) {
            // Ni una aclaración pedida ni una comparación de alternativas modifican el itinerario.
            ItinerarioResponseDTO sinCambios = mapper.toDto(itinerario);
            sinCambios.setEstablecimientosEvaluados(List.of());
            return new RefinamientoItinerarioResponseDTO(
                    sinCambios, respuesta.getRespuestaTexto(), historialActualizado,
                    respuesta.getActividadParaComparar());
        }

        // No usar setDias(nuevaLista): Itinerario.dias es un @OneToMany(orphanRemoval = true) ya
        // administrado por Hibernate para este itinerario persistido. Reemplazar la referencia de
        // la colección (en vez de mutar la misma instancia) la "desreferencia" del lado de
        // Hibernate y el flush revienta con "A collection with orphan deletion was no longer
        // referenced by the owning entity instance" — hay que limpiar y volver a llenar la MISMA
        // colección para que el orphan removal seguido de las inserciones nuevas funcione.
        List<ItinerarioDia> diasNuevos = construirDias(itinerario, respuesta.getItinerarioActualizado().getDias(),
                itinerario.getFechaInicio());
        itinerario.getDias().clear();
        itinerario.getDias().addAll(diasNuevos);
        itinerario.setVersion(itinerario.getVersion() + 1);
        if (respuesta.getItinerarioActualizado().getPuntuacionAmbientalPreliminar() != null) {
            itinerario.setPuntuacionAmbientalPreliminar(
                    BigDecimal.valueOf(respuesta.getItinerarioActualizado().getPuntuacionAmbientalPreliminar()));
        }

        try {
            itinerarioRepository.saveAndFlush(itinerario);
        } catch (DataAccessException e) {
            log.error("Error al guardar los cambios del itinerario {} tras refinamiento", itinerarioId, e);
            throw ApiException.errorInterno(
                    "No fue posible guardar los cambios del itinerario. Intenta nuevamente.");
        }

        ResultadoPriorizacion resultadoPriorizacion = aplicarPriorizacionAmbiental(itinerario, usuarioId);
        calcularYPersistirEcoScore(itinerario, resultadoPriorizacion);

        ItinerarioResponseDTO responseDTO = mapper.toDto(itinerario);
        responseDTO.setEstablecimientosEvaluados(List.of());
        enriquecerConPuntuacionAmbiental(responseDTO, resultadoPriorizacion);

        return new RefinamientoItinerarioResponseDTO(
                responseDTO, respuesta.getRespuestaTexto(), historialActualizado, null);
    }

    private List<MensajeConversacionDTO> obtenerHistorialPrevio(ConversacionContextoDTO contexto) {
        if (contexto == null || contexto.getHistorialMensajes() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(contexto.getHistorialMensajes());
    }

    /**
     * Arma el prompt de refinamiento: el itinerario actual completo (con el id de cada actividad,
     * para que la IA pueda señalar una en concreto vía {@code actividadParaComparar}), el historial
     * de la conversación y el mensaje nuevo del usuario.
     */
    private String construirContextoRefinamiento(Itinerario itinerario, List<MensajeConversacionDTO> historial,
                                                   String mensajeUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("Moneda preferida del usuario (usar SIEMPRE esta moneda si modificás o agregás ")
                .append("costoAproximado/moneda de alguna actividad, salvo que el establecimiento real ")
                .append("solo opere en otra): ")
                .append(monedaPreferidaDe(itinerario.getUsuario())).append("\n\n");
        sb.append("Itinerario actual (JSON):\n").append(serializarItinerarioParaPrompt(itinerario)).append("\n\n");

        if (!historial.isEmpty()) {
            sb.append("Historial de la conversación:\n");
            for (MensajeConversacionDTO mensaje : historial) {
                sb.append(mensaje.getRol()).append(": ").append(mensaje.getContenido()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Mensaje nuevo del usuario: ").append(mensajeUsuario);
        return sb.toString();
    }

    private String serializarItinerarioParaPrompt(Itinerario itinerario) {
        StringBuilder sb = new StringBuilder("{\"dias\":[");
        List<ItinerarioDia> dias = itinerario.getDias();
        for (int i = 0; i < dias.size(); i++) {
            ItinerarioDia dia = dias.get(i);
            sb.append("{\"numeroDia\":").append(dia.getNumeroDia()).append(",\"actividades\":[");
            List<ItinerarioActividad> actividades = dia.getActividades();
            for (int j = 0; j < actividades.size(); j++) {
                ItinerarioActividad actividad = actividades.get(j);
                sb.append("{\"id\":\"").append(actividad.getId()).append("\",")
                        .append("\"nombre\":\"").append(escaparJson(actividad.getNombre())).append("\",")
                        .append("\"horario\":\"").append(actividad.getHorario()).append("\",")
                        .append("\"establecimientoRecomendado\":\"")
                        .append(escaparJson(actividad.getEstablecimientoRecomendado())).append("\",")
                        .append("\"provincia\":\"").append(actividad.getProvincia()).append("\"}");
                if (j < actividades.size() - 1) sb.append(",");
            }
            sb.append("]}");
            if (i < dias.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escaparJson(String texto) {
        return texto == null ? "" : texto.replace("\"", "'");
    }

    /**
     * Historial derivado únicamente de datos del usuario autenticado — nunca se acepta desde el
     * cliente, así que el criterio "debe corresponder al usuario de la sesión activa" se cumple
     * por construcción.
     */
    private HistorialEcoRutaDTO construirHistorial(UUID usuarioId) {
        long total = itinerarioRepository.countByUsuario_Id(usuarioId);
        List<String> provincias = itinerarioRepository.findProvinciasVisitadasByUsuarioId(usuarioId).stream()
                .map(Enum::name)
                .toList();
        return new HistorialEcoRutaDTO((int) total, provincias);
    }

    /**
     * Solo incluye los campos de preferencias/historial necesarios para la recomendación — nunca
     * datos personales del usuario (nombre, correo, etc.) que no aportan a la generación.
     * Incluye también la lista de establecimientos verificados en CarbonHub para que la IA los
     * priorice al generar el itinerario (Req PP-86: priorización ambiental).
     */
    private String construirContextoTuristico(PreferenciasViaje preferencias, HistorialEcoRutaDTO historial) {
        StringBuilder sb = new StringBuilder();
        sb.append("Duración del viaje: ").append(preferencias.getCantidadDias()).append(" días\n");
        sb.append("Fecha de inicio: ").append(preferencias.getFechaInicio()).append("\n");
        sb.append("Tipo de viaje: ").append(preferencias.getTipoViaje().name()).append("\n");
        sb.append("Intereses: ").append(preferencias.getIntereses().stream()
                .map(Enum::name).toList()).append("\n");
        if (preferencias.getPresupuesto() != null) {
            sb.append("Presupuesto estimado: ").append(preferencias.getPresupuesto()).append("\n");
        }
        if (preferencias.getProvinciaPreferida() != null) {
            sb.append("Provincia preferida: ").append(preferencias.getProvinciaPreferida().name()).append("\n");
        }
        if (preferencias.isBuscarCercaDeMi() && preferencias.getUbicacionActual() != null) {
            sb.append("Priorizar cercanía a: ").append(preferencias.getUbicacionActual()).append("\n");
        }
        if (preferencias.getLimitacionesMovilidad() != null) {
            sb.append("Restricciones de accesibilidad: ").append(preferencias.getLimitacionesMovilidad()).append("\n");
        }
        sb.append("Requiere hospedaje: ").append(preferencias.isRequiereHospedaje()).append("\n");
        sb.append("Moneda preferida del usuario (usar SIEMPRE esta moneda en costoAproximado/moneda ")
                .append("de cada actividad, salvo que el establecimiento real solo opere en otra): ")
                .append(monedaPreferidaDe(preferencias.getUsuario())).append("\n");
        sb.append("Itinerarios generados previamente por el usuario: ")
                .append(historial.getTotalItinerariosGenerados()).append("\n");
        if (!historial.getProvinciasVisitadas().isEmpty()) {
            sb.append("Provincias ya visitadas por el usuario: ")
                    .append(historial.getProvinciasVisitadas()).append("\n");
        }

        // Incluir establecimientos verificados para que la IA los priorice
        List<String> nombresVerificados = obtenerNombresEstablecimientosVerificados();
        if (!nombresVerificados.isEmpty()) {
            sb.append("\nEstablecimientos verificados ambientalmente en CarbonHub (PRIORIZAR en el campo ")
              .append("establecimientoRecomendado cuando sea geográficamente coherente): ")
              .append(nombresVerificados).append("\n");
        }

        return sb.toString();
    }

    /**
     * El usuario configura su moneda preferida al completar su perfil inicial
     * ({@code PerfilInicialService}/{@code PreferenciasUsuarioService}), pero hasta ahora ningún
     * flujo de EcoRuta se la pasaba a la IA — Gemini elegía CRC/USD libremente por actividad, sin
     * relación con lo que el usuario configuró. Se resuelve acá con el mismo catálogo que usa el
     * resto de la app ({@code user.models.enums.Moneda}), con CRC como default si el usuario nunca
     * lo configuró explícitamente.
     */
    private String monedaPreferidaDe(com.piedpiper.carbonhub.user.models.entities.Usuario usuario) {
        return com.piedpiper.carbonhub.user.models.enums.Moneda.desde(usuario.getMoneda())
                .orElse(com.piedpiper.carbonhub.user.models.enums.Moneda.POR_DEFECTO)
                .name();
    }

    /**
     * Obtiene los nombres de empresas activas registradas en CarbonHub para inyectarlos en el
     * prompt de la IA. Así Gemini puede priorizarlos como establecimientos recomendados y el
     * matching posterior funciona correctamente para calcular puntuaciones ambientales.
     */
    private List<String> obtenerNombresEstablecimientosVerificados() {
        try {
            return empresaRepository.findByEstado(
                    com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa.ACTIVO).stream()
                    .map(com.piedpiper.carbonhub.empresa.models.entities.Empresa::getNombreEmpresa)
                    .limit(50)
                    .toList();
        } catch (Exception e) {
            log.warn("No se pudieron cargar establecimientos verificados para el prompt.", e);
            return List.of();
        }
    }

    private Itinerario construirItinerario(PreferenciasViaje preferencias, ResultadoGeneracionIA resultadoIA) {
        ItinerarioIaResponseDTO respuesta = resultadoIA.respuesta();
        boolean parcial = resultadoIA.resultado() == ResultadoValidacionItinerario.VALIDO_PARCIAL;

        Itinerario itinerario = Itinerario.builder()
                .usuario(preferencias.getUsuario())
                .cantidadDias(preferencias.getCantidadDias())
                .fechaInicio(preferencias.getFechaInicio())
                .tipoViaje(preferencias.getTipoViaje())
                .estado(EstadoItinerario.GENERADO)
                .puntuacionAmbientalPreliminar(respuesta.getPuntuacionAmbientalPreliminar() != null
                        ? BigDecimal.valueOf(respuesta.getPuntuacionAmbientalPreliminar())
                        : null)
                .generadoParcial(parcial)
                .mensajeParcial(parcial
                        ? "Se generó un itinerario parcial porque no se encontraron suficientes "
                                + "actividades compatibles con tus preferencias."
                        : null)
                .fechaGeneracion(Instant.now())
                .build();

        itinerario.setDias(construirDias(itinerario, respuesta.getDias(), preferencias.getFechaInicio()));
        return itinerario;
    }

    /**
     * Construye la lista de {@link ItinerarioDia} (con sus actividades) a partir del shape crudo
     * de la IA. Compartido entre {@link #generar} y {@link #refinar} — ambos flujos arman un
     * itinerario completo a partir de una respuesta de Gemini, solo cambia de dónde sale la fecha
     * de inicio (preferencias vs. el itinerario ya existente).
     */
    private List<ItinerarioDia> construirDias(Itinerario itinerario, List<DiaIaDTO> diasIa, LocalDate fechaInicio) {
        List<ItinerarioDia> dias = new ArrayList<>();
        for (DiaIaDTO diaIa : diasIa) {
            ItinerarioDia dia = ItinerarioDia.builder()
                    .itinerario(itinerario)
                    .numeroDia(diaIa.getNumeroDia())
                    .fecha(fechaInicio.plusDays(diaIa.getNumeroDia() - 1L))
                    .orden(diaIa.getNumeroDia())
                    .build();

            List<ItinerarioActividad> actividades = new ArrayList<>();
            int orden = 1;
            for (ActividadIaDTO actividadIa : diaIa.getActividades()) {
                actividades.add(construirActividad(dia, actividadIa, orden++));
            }
            dia.setActividades(actividades);
            dias.add(dia);
        }
        return dias;
    }

    /**
     * {@code ItinerarioValidador} ya garantizó que {@code provincia} (siempre) y {@code moneda}
     * (cuando hay costo) resuelven contra su catálogo para cualquier respuesta que llegue hasta
     * acá — no se re-valida aquí (CONVENTIONS.md §4.7).
     */
    private ItinerarioActividad construirActividad(ItinerarioDia dia, ActividadIaDTO actividadIa, int orden) {
        Provincia provincia = Catalogos.desde(Provincia.class, actividadIa.getProvincia())
                .orElseThrow(() -> ApiException.itinerarioRespuestaInvalida());
        Moneda moneda = actividadIa.getMoneda() != null
                ? Catalogos.desde(Moneda.class, actividadIa.getMoneda()).orElse(null)
                : null;

        return ItinerarioActividad.builder()
                .itinerarioDia(dia)
                .nombre(actividadIa.getNombre())
                .descripcion(actividadIa.getDescripcion())
                .horario(LocalTime.parse(actividadIa.getHorario()))
                .duracionMinutos(actividadIa.getDuracionMinutos())
                .costoAproximado(actividadIa.getCostoAproximado())
                .moneda(moneda)
                .establecimientoRecomendado(actividadIa.getEstablecimientoRecomendado())
                .provincia(provincia)
                .orden(orden)
                .puntuacionAmbientalEstimada(actividadIa.getPuntuacionAmbientalEstimada())
                .categoriaTuristica(actividadIa.getCategoriaTuristica() != null
                        ? Catalogos.desde(InteresTuristico.class, actividadIa.getCategoriaTuristica()).orElse(null)
                        : null)
                .build();
    }

    /**
     * Otorga insignias por conteo simple (1er/5to/10mo itinerario). Los códigos
     * `primer_itinerario_sostenible`, `usuario_recurrente` y `explorador_de_provincias` del
     * catálogo quedan pendientes — ninguna historia de Jira define todavía el umbral/criterio
     * exacto para dispararlos.
     */
    private void registrarEventoDeReconocimientoTrasCommit(UUID usuarioId) {
        long total = itinerarioRepository.countByUsuario_Id(usuarioId);
        String codigo = switch ((int) total) {
            case 1 -> EventoReconocimientoCodigo.PRIMER_ITINERARIO_GENERADO.getCodigo();
            case 5 -> EventoReconocimientoCodigo.CINCO_ITINERARIOS_GENERADOS.getCodigo();
            case 10 -> EventoReconocimientoCodigo.DIEZ_ITINERARIOS_GENERADOS.getCodigo();
            default -> null;
        };
        if (codigo == null) {
            return;
        }
        Runnable emitir = () -> eventoReconocimientoService.generar(usuarioId, codigo);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitir.run();
                }
            });
        } else {
            emitir.run();
        }
    }

    /**
     * Enriquece el itinerario con puntuaciones ambientales para los establecimientos que
     * coincidan con empresas registradas en CarbonHub. No altera el orden del itinerario;
     * los registros de ponderación se persisten para auditoría (Req 5.4).
     */
    private ResultadoPriorizacion aplicarPriorizacionAmbiental(Itinerario itinerario, UUID usuarioId) {
        List<EstablecimientoRankeado> establecimientos = extraerEstablecimientosRankeados(itinerario);

        if (establecimientos.isEmpty()) {
            return new ResultadoPriorizacion(establecimientos, 0, 0);
        }

        try {
            return priorizacionAmbientalService.aplicarPriorizacion(
                    establecimientos, itinerario.getId(), usuarioId);
        } catch (Exception e) {
            log.warn("Fallo en priorización ambiental. Continuando sin puntuaciones ambientales.", e);
            return new ResultadoPriorizacion(establecimientos, establecimientos.size(), 3);
        }
    }

    /**
     * Calcula el EcoScore del itinerario (Req PP-91) y lo persiste sobre la entidad. No falla la
     * generación si el cálculo no es posible: deja los campos de EcoScore en null, lo que el
     * frontend interpreta como "no fue posible calcular el impacto ambiental del itinerario".
     */
    private void calcularYPersistirEcoScore(Itinerario itinerario, ResultadoPriorizacion resultadoPriorizacion) {
        if (resultadoPriorizacion == null || resultadoPriorizacion.getEstablecimientosRankeados() == null) {
            return;
        }
        List<EstablecimientoRankeado> establecimientos = resultadoPriorizacion.getEstablecimientosRankeados();
        EcoScoreResultado resultado;
        try {
            resultado = ecoScoreService.calcular(establecimientos, itinerario);
        } catch (Exception e) {
            log.warn("Fallo al calcular el EcoScore del itinerario {}. Continuando sin EcoScore.",
                    itinerario.getId(), e);
            return;
        }

        if (resultado == null) {
            return;
        }

        itinerario.setEcoScore(resultado.getEcoScore());
        itinerario.setClasificacionAmbiental(resultado.getClasificacion());
        itinerario.setEcoScoreParcial(resultado.isParcial());
        itinerario.setEcoScoreCalculadoEn(Instant.now());
        itinerarioRepository.save(itinerario);
    }

    /**
     * Construye la lista de {@link EstablecimientoRankeado} a partir de las actividades del
     * itinerario. Solo incluye establecimientos que coincidan con una empresa registrada en
     * CarbonHub (por nombre parcial, case-insensitive). Los que no matchean se omiten del
     * cálculo ambiental — su puntuación será calculada por la IA si disponible.
     */
    private List<EstablecimientoRankeado> extraerEstablecimientosRankeados(Itinerario itinerario) {
        List<EstablecimientoRankeado> establecimientos = new ArrayList<>();
        int totalActividades = itinerario.getDias().stream()
                .mapToInt(dia -> dia.getActividades().size())
                .sum();

        // Pre-cargar todas las empresas activas para matching por nombre
        var empresasActivas = empresaRepository.findByEstado(
                com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa.ACTIVO);
        if (empresasActivas.size() > 100) {
            log.warn("Catálogo de empresas activas ({}) supera el tope de matching (100). "
                    + "Establecimientos fuera del primer bloque no se vincularán con scores reales.",
                    empresasActivas.size());
        }

        int posicion = 0;
        for (ItinerarioDia dia : itinerario.getDias()) {
            for (ItinerarioActividad actividad : dia.getActividades()) {
                posicion++;
                if (actividad.getEstablecimientoRecomendado() == null
                        || actividad.getEstablecimientoRecomendado().isBlank()) {
                    continue;
                }

                // Buscar empresa registrada por coincidencia parcial de nombre
                String nombreActividad = actividad.getEstablecimientoRecomendado().toLowerCase();
                var empresaMatch = empresasActivas.stream()
                        .filter(e -> e.getNombreEmpresa() != null &&
                                (e.getNombreEmpresa().toLowerCase().contains(nombreActividad) ||
                                 nombreActividad.contains(e.getNombreEmpresa().toLowerCase())))
                        .findFirst();

                // Solo incluir si matchea con empresa real — sin match no hay datos verificados
                UUID empresaId = empresaMatch
                        .map(com.piedpiper.carbonhub.empresa.models.entities.Empresa::getId)
                        .orElse(null);

                // Puntuación turística base: orden inverso normalizado (1.0 para el primero)
                BigDecimal puntuacionTuristica = totalActividades > 0
                        ? BigDecimal.valueOf(1.0 - ((double) (posicion - 1) / totalActividades))
                        : BigDecimal.ONE;

                EstablecimientoRankeado rankeado = new EstablecimientoRankeado();
                rankeado.setEmpresaId(empresaId);
                rankeado.setNombreEstablecimiento(actividad.getEstablecimientoRecomendado());
                rankeado.setPuntuacionTuristica(puntuacionTuristica);
                rankeado.setPuntuacionAmbiental(BigDecimal.ZERO);
                rankeado.setPuntuacionFinal(puntuacionTuristica);

                establecimientos.add(rankeado);
            }
        }
        return establecimientos;
    }

    /**
     * Enriquece el DTO de respuesta con las puntuaciones ambientales calculadas por el servicio
     * de priorización. Mapea cada actividad a su puntuación usando el nombre del establecimiento.
     */
    private void enriquecerConPuntuacionAmbiental(ItinerarioResponseDTO responseDTO,
                                                   ResultadoPriorizacion resultadoPriorizacion) {
        if (resultadoPriorizacion == null || resultadoPriorizacion.getEstablecimientosRankeados() == null) {
            return;
        }

        Map<String, PuntuacionAmbientalResponseDTO> puntuacionesPorEstablecimiento =
                resultadoPriorizacion.getEstablecimientosRankeados().stream()
                        .filter(e -> e.getDetalleAmbiental() != null)
                        .collect(Collectors.toMap(
                                EstablecimientoRankeado::getNombreEstablecimiento,
                                EstablecimientoRankeado::getDetalleAmbiental,
                                (a, b) -> a // En caso de duplicados, conservar el primero
                        ));

        responseDTO.setEstablecimientosEvaluados(construirEstablecimientosEvaluados(puntuacionesPorEstablecimiento));

        if (responseDTO.getDias() == null) {
            return;
        }

        for (var dia : responseDTO.getDias()) {
            if (dia.getActividades() == null) {
                continue;
            }
            for (var actividad : dia.getActividades()) {
                if (actividad.getEstablecimientoRecomendado() != null) {
                    PuntuacionAmbientalResponseDTO puntuacion =
                            puntuacionesPorEstablecimiento.get(actividad.getEstablecimientoRecomendado());
                    actividad.setPuntuacionAmbiental(puntuacion);
                }
            }
        }
    }

    /**
     * Convierte el mapa nombre → puntuación ambiental en el desglose por establecimiento expuesto
     * en la respuesta del itinerario (Req PP-91), omitiendo establecimientos sin puntuación.
     */
    private List<EstablecimientoEcoScoreResponseDTO> construirEstablecimientosEvaluados(
            Map<String, PuntuacionAmbientalResponseDTO> puntuacionesPorEstablecimiento) {
        return puntuacionesPorEstablecimiento.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> new EstablecimientoEcoScoreResponseDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Calcula y asigna puntuaciones ambientales al DTO de respuesta sin persistir registros
     * de auditoría. Usado al obtener un itinerario ya guardado para enriquecer la visualización.
     */
    private void enriquecerConPuntuacionesCalculadas(ItinerarioResponseDTO responseDTO, Itinerario itinerario) {
        List<EstablecimientoRankeado> establecimientos = extraerEstablecimientosRankeados(itinerario);
        if (establecimientos.isEmpty()) {
            return;
        }

        List<UUID> empresaIds = establecimientos.stream()
                .map(EstablecimientoRankeado::getEmpresaId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // Consultar indicadores (solo lectura, sin persistir)
        Map<UUID, IndicadorAmbientalDTO> indicadores;
        Map<UUID, IMADTO> imaMap;
        Map<UUID, BenchmarkDTO> benchmarkMap;
        try {
            indicadores = indicadorClient.consultarIndicadores(empresaIds);
            imaMap = imaClient.consultarIma(empresaIds);
            benchmarkMap = benchmarkClient.consultarBenchmark(empresaIds);
        } catch (Exception e) {
            log.warn("No se pudieron calcular puntuaciones ambientales para la visualización.", e);
            return;
        }

        // Mapear score estimado de la IA por nombre de establecimiento
        Map<String, Integer> scoreEstimadoPorNombre = new java.util.HashMap<>();
        for (ItinerarioDia dia : itinerario.getDias()) {
            for (ItinerarioActividad act : dia.getActividades()) {
                if (act.getEstablecimientoRecomendado() != null && act.getPuntuacionAmbientalEstimada() != null) {
                    scoreEstimadoPorNombre.put(act.getEstablecimientoRecomendado(), act.getPuntuacionAmbientalEstimada());
                }
            }
        }

        // Calcular puntuaciones sin persistir
        Map<String, PuntuacionAmbientalResponseDTO> puntuacionesPorEstablecimiento = new java.util.HashMap<>();

        for (EstablecimientoRankeado est : establecimientos) {
            UUID empresaId = est.getEmpresaId();
            Integer scoreIA = scoreEstimadoPorNombre.get(est.getNombreEstablecimiento());
            PuntuacionAmbientalResponseDTO detalle = puntuacionCalculator.calcular(
                    indicadores.get(empresaId), imaMap.get(empresaId), benchmarkMap.get(empresaId), scoreIA);
            puntuacionesPorEstablecimiento.put(est.getNombreEstablecimiento(), detalle);
        }

        // Asignar al DTO de respuesta
        responseDTO.setEstablecimientosEvaluados(construirEstablecimientosEvaluados(puntuacionesPorEstablecimiento));

        if (responseDTO.getDias() == null) return;
        for (var dia : responseDTO.getDias()) {
            if (dia.getActividades() == null) continue;
            for (var actividad : dia.getActividades()) {
                if (actividad.getEstablecimientoRecomendado() != null) {
                    actividad.setPuntuacionAmbiental(
                            puntuacionesPorEstablecimiento.get(actividad.getEstablecimientoRecomendado()));
                }
            }
        }
    }
}
