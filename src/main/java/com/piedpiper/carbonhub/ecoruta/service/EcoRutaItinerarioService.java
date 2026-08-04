package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.HistorialEcoRutaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.ActividadIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO.DiaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.ecoruta.service.ItinerarioIaClienteService.ResultadoGeneracionIA;
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
import java.util.UUID;

@Service
public class EcoRutaItinerarioService {

    private static final Logger log = LoggerFactory.getLogger(EcoRutaItinerarioService.class);

    private final PreferenciasViajeRepository preferenciasViajeRepository;
    private final ItinerarioRepository itinerarioRepository;
    private final ItinerarioIaClienteService itinerarioIaClienteService;
    private final ItinerarioCuotaService itinerarioCuotaService;
    private final EventoReconocimientoService eventoReconocimientoService;
    private final ItinerarioMapper mapper;

    public EcoRutaItinerarioService(PreferenciasViajeRepository preferenciasViajeRepository,
                                    ItinerarioRepository itinerarioRepository,
                                    ItinerarioIaClienteService itinerarioIaClienteService,
                                    ItinerarioCuotaService itinerarioCuotaService,
                                    EventoReconocimientoService eventoReconocimientoService,
                                    ItinerarioMapper mapper) {
        this.preferenciasViajeRepository = preferenciasViajeRepository;
        this.itinerarioRepository = itinerarioRepository;
        this.itinerarioIaClienteService = itinerarioIaClienteService;
        this.itinerarioCuotaService = itinerarioCuotaService;
        this.eventoReconocimientoService = eventoReconocimientoService;
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

        registrarEventoDeReconocimientoTrasCommit(usuarioId);

        return mapper.toDto(itinerario);
    }

    /**
     * Ambos casos (no existe / pertenece a otro usuario) devuelven el mismo 404 — nunca un 403
     * distinto, para no revelar por enumeración de IDs que un itinerario ajeno existe.
     */
    @Transactional(readOnly = true)
    public ItinerarioResponseDTO obtener(UUID itinerarioId, UUID usuarioId) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("Itinerario no encontrado."));
        return mapper.toDto(itinerario);
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
        return sb.toString();
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
                actividades.add(construirActividad(dia, actividadIa, orden++));
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
}
