package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionIaTexto;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionRenovacionResponseDTO;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Recomendación de renovación" del dashboard de Certificaciones (PP-72):
 * identifica cuál certificación con alerta activa debe renovarse primero
 * y le pide a la IA que redacte la justificación.
 *
 * <p>Deliberadamente SIN {@code @Transactional}: la consulta a base de
 * datos vive en {@link RecomendacionRenovacionConsultaService} (un bean
 * aparte, no solo un método aparte — ver el porqué en su javadoc), y la
 * llamada a Gemini ({@link RecomendacionRenovacionIaService}) corre acá,
 * fuera de cualquier transacción. Antes ambas corrían dentro del mismo
 * {@code @Transactional(readOnly = true)}: con el LLM lento o caído, cada
 * carga del dashboard dejaba una conexión del pool tomada hasta el
 * timeout, y con varios usuarios a la vez el pool se agotaba — se caía
 * todo lo que necesita base, no solo este bloque (docs/CONVENTIONS.md
 * §4.5).</p>
 *
 * <p><b>Caché en memoria por usuario, con vigencia de un día:</b> la
 * certificación prioritaria y sus días restantes cambian, como mucho, una
 * vez al día — regenerar el texto con Gemini en cada carga del dashboard
 * es latencia y costo por algo que casi siempre da lo mismo. Es
 * deliberadamente simple (un {@code ConcurrentHashMap}, no un caché
 * distribuido): con una sola instancia del backend alcanza, y evita sumar
 * una dependencia nueva (Spring Cache/Redis) para esto. Si el backend
 * llega a correr en más de una instancia, este caché deja de ser
 * consistente entre instancias — ahí sí conviene migrar a uno
 * distribuido.</p>
 */
@Service
public class DashboardRecomendacionService {

    private final RecomendacionRenovacionConsultaService consultaService;
    private final RecomendacionRenovacionIaService iaService;

    private final ConcurrentHashMap<UUID, CacheEntry> cachePorUsuario = new ConcurrentHashMap<>();

    public DashboardRecomendacionService(
            RecomendacionRenovacionConsultaService consultaService,
            RecomendacionRenovacionIaService iaService) {
        this.consultaService = consultaService;
        this.iaService = iaService;
    }

    public Optional<RecomendacionRenovacionResponseDTO> obtenerRecomendacion(UUID usuarioId) {
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);

        CacheEntry cacheada = cachePorUsuario.get(usuarioId);
        if (cacheada != null && cacheada.fecha().equals(hoy)) {
            return cacheada.valor();
        }

        Optional<CertAlertaDTO> prioritaria = consultaService.obtenerCertificacionPrioritaria(usuarioId);
        Optional<RecomendacionRenovacionResponseDTO> resultado = prioritaria.map(this::construirRespuesta);

        cachePorUsuario.put(usuarioId, new CacheEntry(hoy, resultado));
        return resultado;
    }

    private RecomendacionRenovacionResponseDTO construirRespuesta(CertAlertaDTO prioritaria) {
        Optional<RecomendacionIaTexto> texto = iaService.generar(prioritaria);

        return new RecomendacionRenovacionResponseDTO(
                prioritaria.getIdCertificacion(),
                prioritaria.getNombreCertificacion(),
                prioritaria.getFechaVencimiento(),
                prioritaria.getDiasRestantes(),
                prioritaria.getImpactoHuellaT(),
                texto.map(RecomendacionIaTexto::justificacion).orElse(null),
                texto.map(RecomendacionIaTexto::sugerenciaAccion).orElse(null));
    }

    private record CacheEntry(LocalDate fecha, Optional<RecomendacionRenovacionResponseDTO> valor) {
    }
}
