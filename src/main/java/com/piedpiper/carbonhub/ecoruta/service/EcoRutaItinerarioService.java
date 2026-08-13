package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoEcoScoreResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.HistorialEcoRutaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;
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
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EventoReconocimientoCodigo;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EcoRutaItinerarioService {

    private static final Logger log = LoggerFactory.getLogger(EcoRutaItinerarioService.class);

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

        // Pre-cargar empresas activas una sola vez y pasarla a construirItinerario para vincular
        // cada actividad con su empresa (persistido en ItinerarioActividad.empresa). Ya no se
        // reutiliza en aplicarPriorizacionAmbiental: ese flujo ahora lee el vínculo ya persistido
        // en cada actividad en vez de volver a matchear por nombre (ver
        // extraerEstablecimientosRankeados).
        List<Empresa> empresasActivas = empresaRepository.findByEstado(EstadoEmpresa.ACTIVO);

        Itinerario itinerario = construirItinerario(preferencias, resultadoIA, empresasActivas);

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

    private Itinerario construirItinerario(PreferenciasViaje preferencias, ResultadoGeneracionIA resultadoIA,
                                            List<Empresa> empresasActivas) {
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

        List<ItinerarioDia> dias = new ArrayList<>();
        for (DiaIaDTO diaIa : respuesta.getDias()) {
            ItinerarioDia dia = ItinerarioDia.builder()
                    .itinerario(itinerario)
                    .numeroDia(diaIa.getNumeroDia())
                    .fecha(preferencias.getFechaInicio().plusDays(diaIa.getNumeroDia() - 1L))
                    .orden(diaIa.getNumeroDia())
                    .build();

            List<ItinerarioActividad> actividades = new ArrayList<>();
            int orden = 1;
            for (ActividadIaDTO actividadIa : diaIa.getActividades()) {
                actividades.add(construirActividad(dia, actividadIa, orden++, empresasActivas));
            }
            dia.setActividades(actividades);
            dias.add(dia);
        }
        itinerario.setDias(dias);
        return itinerario;
    }

    /**
     * {@code ItinerarioValidador} ya garantizó que {@code provincia} (siempre) y {@code moneda}
     * (cuando hay costo) resuelven contra su catálogo para cualquier respuesta que llegue hasta
     * acá — no se re-valida aquí (CONVENTIONS.md §4.7).
     */
    private ItinerarioActividad construirActividad(ItinerarioDia dia, ActividadIaDTO actividadIa, int orden,
                                                     List<Empresa> empresasActivas) {
        Provincia provincia = Catalogos.desde(Provincia.class, actividadIa.getProvincia())
                .orElseThrow(() -> ApiException.itinerarioRespuestaInvalida());
        Moneda moneda = actividadIa.getMoneda() != null
                ? Catalogos.desde(Moneda.class, actividadIa.getMoneda()).orElse(null)
                : null;
        Empresa empresa = actividadIa.getEstablecimientoRecomendado() != null
                        && !actividadIa.getEstablecimientoRecomendado().isBlank()
                ? matchearEmpresaPorNombre(actividadIa.getEstablecimientoRecomendado(), empresasActivas)
                        .orElse(null)
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
                .empresa(empresa)
                .provincia(provincia)
                .orden(orden)
                .puntuacionAmbientalEstimada(actividadIa.getPuntuacionAmbientalEstimada())
                .categoriaTuristica(actividadIa.getCategoriaTuristica() != null
                        ? Catalogos.desde(InteresTuristico.class, actividadIa.getCategoriaTuristica()).orElse(null)
                        : null)
                .build();
    }

    /**
     * Nombres de empresa más cortos que esto quedan fuera del matching por {@code contains}: un
     * nombre corto (ej. "Sol") actuaría como comodín y matchearía cualquier establecimiento que
     * lo contenga como substring ("Hotel Solarium", "Soluciones Verdes"), atribuyendo
     * incorrectamente el establecimiento a esa empresa.
     */
    private static final int LONGITUD_MINIMA_NOMBRE_EMPRESA_PARA_MATCHING = 4;

    /**
     * Busca, por coincidencia parcial de nombre (case-insensitive, en cualquier dirección), la
     * empresa activa registrada en CarbonHub que corresponde al establecimiento recomendado por la
     * IA. Se usa una única vez, al construir la actividad en el momento de generación
     * ({@link #construirActividad}) — el resultado queda persistido en
     * {@code ItinerarioActividad.empresa} y no se vuelve a recalcular en lecturas posteriores del
     * itinerario (ver {@link #extraerEstablecimientosRankeados(Itinerario)}).
     */
    private Optional<Empresa> matchearEmpresaPorNombre(String nombreEstablecimiento, List<Empresa> empresasActivas) {
        String nombreNormalizado = nombreEstablecimiento.toLowerCase();
        return empresasActivas.stream()
                .filter(e -> e.getNombreEmpresa() != null
                        && e.getNombreEmpresa().length() >= LONGITUD_MINIMA_NOMBRE_EMPRESA_PARA_MATCHING)
                .filter(e -> e.getNombreEmpresa().toLowerCase().contains(nombreNormalizado)
                        || nombreNormalizado.contains(e.getNombreEmpresa().toLowerCase()))
                .findFirst();
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
     * itinerario. Solo incluye establecimientos vinculados a una empresa registrada en CarbonHub.
     * Los que no matchean se omiten del cálculo ambiental — su puntuación será calculada por la
     * IA si disponible.
     *
     * <p>Lee {@code actividad.getEmpresa()} (persistido en {@link #construirActividad} al generar
     * el itinerario) en vez de volver a matchear por nombre contra el catálogo de empresas
     * activas. Antes de este cambio, cada lectura del itinerario re-ejecutaba
     * {@link #matchearEmpresaPorNombre} contra el catálogo <em>vigente</em> al momento de la
     * consulta — lo que producía dos problemas: (1) el resultado podía divergir del que se
     * calculó al generar el itinerario si el catálogo de empresas cambiaba entretanto (una
     * empresa se desactiva, cambia de nombre, etc.), y (2) nombres de empresa ambiguos entre sí
     * (p. ej. "Café del Valle" / "Café del Valle S.A.") podían resolver a una empresa distinta en
     * cada lectura, porque el matching por {@code contains} sin desambiguación no es determinista
     * frente a variaciones en el orden de la consulta. Usar el vínculo ya persistido lo hace
     * estable: la actividad siempre reporta la misma empresa con la que quedó vinculada al
     * generarse, sin importar cuántas veces se consulte después.
     */
    private List<EstablecimientoRankeado> extraerEstablecimientosRankeados(Itinerario itinerario) {
        List<EstablecimientoRankeado> establecimientos = new ArrayList<>();
        int totalActividades = itinerario.getDias().stream()
                .mapToInt(dia -> dia.getActividades().size())
                .sum();

        int posicion = 0;
        for (ItinerarioDia dia : itinerario.getDias()) {
            for (ItinerarioActividad actividad : dia.getActividades()) {
                posicion++;
                if (actividad.getEstablecimientoRecomendado() == null
                        || actividad.getEstablecimientoRecomendado().isBlank()) {
                    continue;
                }

                UUID empresaId = actividad.getEmpresa() != null ? actividad.getEmpresa().getId() : null;

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

        responseDTO.setEstablecimientosEvaluados(construirEstablecimientosEvaluados(
                resultadoPriorizacion.getEstablecimientosRankeados()));

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
     * Arma el desglose por establecimiento expuesto en la respuesta del itinerario (Req PP-91),
     * omitiendo establecimientos sin puntuación. Incluye {@code empresaId} (PP-95) cuando el
     * establecimiento matcheó con una empresa registrada — {@code null} si no matcheó, ver
     * {@link #extraerEstablecimientosRankeados}.
     *
     * <p>Deduplica por nombre UNA sola vez sobre {@code establecimientosRankeados} y arma el DTO a
     * partir del {@link EstablecimientoRankeado} elegido — nombre, {@code empresaId} y
     * {@code detalleAmbiental} salen siempre del mismo objeto. Antes de este fix, el empresaId y
     * el score se resolvían con dos dedupe distintos (uno acá, otro implícito en cómo el llamador
     * armaba el mapa de puntuaciones), que en itinerarios con dos actividades recomendando el
     * mismo nombre de establecimiento podían "ganar" filas distintas — mostrando el score de una
     * actividad junto al empresaId (o la bandera) de otra.
     */
    private List<EstablecimientoEcoScoreResponseDTO> construirEstablecimientosEvaluados(
            List<EstablecimientoRankeado> establecimientosRankeados) {
        Map<String, EstablecimientoRankeado> primerRankeadoPorNombre = establecimientosRankeados.stream()
                .collect(Collectors.toMap(
                        EstablecimientoRankeado::getNombreEstablecimiento,
                        rankeado -> rankeado,
                        (a, b) -> a
                ));

        return primerRankeadoPorNombre.values().stream()
                .filter(rankeado -> rankeado.getDetalleAmbiental() != null)
                .map(rankeado -> new EstablecimientoEcoScoreResponseDTO(
                        rankeado.getNombreEstablecimiento(),
                        rankeado.getEmpresaId(),
                        rankeado.getDetalleAmbiental()))
                .toList();
    }

    /**
     * Calcula y asigna puntuaciones ambientales al DTO de respuesta sin persistir registros
     * de auditoría. Usado al obtener un itinerario ya guardado para enriquecer la visualización.
     */
    private void enriquecerConPuntuacionesCalculadas(ItinerarioResponseDTO responseDTO, Itinerario itinerario) {
        // Ya no hace falta cargar el catálogo de empresas activas acá: extraerEstablecimientosRankeados
        // lee el vínculo empresa-actividad ya persistido, no vuelve a matchear por nombre.
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

        // Calcular puntuaciones sin persistir. Se setean tanto en el mapa (usado más abajo para
        // enriquecer cada actividad del día a día) como en el propio EstablecimientoRankeado
        // (usado por construirEstablecimientosEvaluados): así el desglose y la actividad
        // individual siempre leen la puntuación del mismo establecimiento resuelto, sin depender
        // de un segundo lookup por nombre que podría resolver a una fila distinta si hay dos
        // establecimientos con el mismo nombre en el itinerario.
        Map<String, PuntuacionAmbientalResponseDTO> puntuacionesPorEstablecimiento = new java.util.HashMap<>();

        for (EstablecimientoRankeado est : establecimientos) {
            UUID empresaId = est.getEmpresaId();
            Integer scoreIA = scoreEstimadoPorNombre.get(est.getNombreEstablecimiento());
            PuntuacionAmbientalResponseDTO detalle = puntuacionCalculator.calcular(
                    indicadores.get(empresaId), imaMap.get(empresaId), benchmarkMap.get(empresaId), scoreIA);
            est.setDetalleAmbiental(detalle);
            puntuacionesPorEstablecimiento.put(est.getNombreEstablecimiento(), detalle);
        }

        // Asignar al DTO de respuesta
        responseDTO.setEstablecimientosEvaluados(construirEstablecimientosEvaluados(establecimientos));

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
