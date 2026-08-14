package com.piedpiper.carbonhub.meta.service;

import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.common.RangosPeriodoDashboard;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.enums.PeriodoDashboard;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.meta.mappers.MetaMapper;
import com.piedpiper.carbonhub.meta.models.dtos.CrearMetaRequestDTO;
import com.piedpiper.carbonhub.meta.models.dtos.MetaResponseDTO;
import com.piedpiper.carbonhub.meta.models.entities.Meta;
import com.piedpiper.carbonhub.meta.models.enums.EstadoMeta;
import com.piedpiper.carbonhub.meta.repository.MetaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Metas de reducción de huella de carbono (PP-78): alta y listado con
 * progreso calculado en cada consulta.
 *
 * <p>El progreso NO se persiste: se recalcula cada vez comparando la
 * huella actual del período seleccionado (mismo {@link PeriodoDashboard}
 * que el bloque de resumen de huella, PP-19/PP-61) contra
 * {@code valorObjetivoHuellaT}, usando {@link RangosPeriodoDashboard} — el
 * mismo cálculo de rango de fechas que {@code DashboardHuellaService} —
 * para que el selector de período mueva a ambos bloques de forma
 * consistente.</p>
 */
@Service
public class MetaService {

    private static final Logger log = LoggerFactory.getLogger(MetaService.class);
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final MetaRepository metaRepository;
    private final EmisionRepository emisionRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final EmpresaRepository empresaRepository;
    private final MetaMapper metaMapper;

    public MetaService(
            MetaRepository metaRepository,
            EmisionRepository emisionRepository,
            EmisionEmpresaService emisionEmpresaService,
            EmpresaRepository empresaRepository,
            MetaMapper metaMapper) {
        this.metaRepository = metaRepository;
        this.emisionRepository = emisionRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.empresaRepository = empresaRepository;
        this.metaMapper = metaMapper;
    }

    @Transactional
    public MetaResponseDTO crear(UUID usuarioId, CrearMetaRequestDTO request) {
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);
        if (request.getFechaLimite().isBefore(hoy)) {
            throw ApiException.fechaLimiteMetaInvalida();
        }

        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        // getReferenceById (no una consulta con findById): emisionEmpresaService.empresaId()
        // ya garantiza que la empresa existe (viene de usuario.getEmpresa(), respaldado por la
        // FK); volver a consultarla aquí solo para setear la relación sería una verificación
        // redundante de algo que la capa anterior ya asegura (docs/CONVENTIONS.md §4.7).
        Empresa empresa = empresaRepository.getReferenceById(empresaId);

        Meta meta = Meta.builder()
                .empresa(empresa)
                .nombreMeta(request.getNombreMeta())
                .valorObjetivoHuellaT(request.getValorObjetivoHuellaT())
                .fechaLimite(request.getFechaLimite())
                .fechaCreacion(Instant.now())
                .build();

        Meta guardada;
        try {
            // saveAndFlush (no save): Meta.id usa GenerationType.UUID, asignado en memoria,
            // asi que save() no dispara el INSERT real de inmediato -- Hibernate puede
            // diferirlo hasta el flush/commit de la transaccion, que ocurre despues de que
            // este metodo retorna. Sin el flush explicito aca, una violacion real de
            // constraint escaparia de este catch y llegaria como excepcion no controlada al
            // terminar la transaccion, sin pasar por el mensaje de error propio de abajo.
            guardada = metaRepository.saveAndFlush(meta);
        } catch (DataAccessException e) {
            log.error("Error al guardar la meta '{}' para la empresa {}", request.getNombreMeta(), empresaId, e);
            throw ApiException.errorInterno("Ocurrió un error al guardar la meta. Por favor, intenta nuevamente.");
        }

        BigDecimal huellaActualT = huellaActualToneladas(
                empresaId, PeriodoDashboard.POR_DEFECTO, hoy.getYear(), hoy);
        return aDto(guardada, huellaActualT, hoy);
    }

    @Transactional
    public MetaResponseDTO actualizar(UUID usuarioId, UUID id, CrearMetaRequestDTO request) {
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);
        if (request.getFechaLimite().isBefore(hoy)) {
            throw ApiException.fechaLimiteMetaInvalida();
        }

        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        Meta meta = metaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontró la meta solicitada."));

        meta.setNombreMeta(request.getNombreMeta());
        meta.setValorObjetivoHuellaT(request.getValorObjetivoHuellaT());
        meta.setFechaLimite(request.getFechaLimite());
        Meta actualizada = metaRepository.save(meta);

        BigDecimal huellaActualT = huellaActualToneladas(
                empresaId, PeriodoDashboard.POR_DEFECTO, hoy.getYear(), hoy);
        return aDto(actualizada, huellaActualT, hoy);
    }

    @Transactional
    public void eliminar(UUID usuarioId, UUID id) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        Meta meta = metaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("No se encontró la meta solicitada."));
        metaRepository.delete(meta);
    }

    @Transactional(readOnly = true)
    public List<MetaResponseDTO> listar(UUID usuarioId, String periodo, Integer anio) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        PeriodoDashboard periodoNormalizado = PeriodoDashboard.desde(periodo).orElse(PeriodoDashboard.POR_DEFECTO);
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);
        int anioConsultar = anio == null ? hoy.getYear() : anio;
        RangosPeriodoDashboard.validarAnio(anioConsultar);

        List<Meta> metas = metaRepository.findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(
                empresaId, EstadoMeta.ACTIVA);

        BigDecimal huellaActualT = huellaActualToneladas(empresaId, periodoNormalizado, anioConsultar, hoy);

        return metas.stream()
                .map(meta -> aDto(meta, huellaActualT, hoy))
                .collect(Collectors.toList());
    }

    private MetaResponseDTO aDto(Meta meta, BigDecimal huellaActualT, LocalDate hoy) {
        MetaResponseDTO dto = metaMapper.toDto(meta);
        dto.setHuellaActualT(huellaActualT);
        dto.setProgresoPorcentaje(progreso(huellaActualT, meta.getValorObjetivoHuellaT()));
        dto.setVencida(meta.getFechaLimite().isBefore(hoy));
        return dto;
    }

    /** {@code (huellaActualT / valorObjetivoHuellaT) × 100}, tal como pide el criterio de aceptación. */
    private int progreso(BigDecimal huellaActualT, BigDecimal valorObjetivoHuellaT) {
        return huellaActualT
                .multiply(CIEN)
                .divide(valorObjetivoHuellaT, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal huellaActualToneladas(UUID empresaId, PeriodoDashboard periodo, int anio, LocalDate hoy) {
        RangosPeriodoDashboard.Rango rango = RangosPeriodoDashboard.actual(periodo, anio, hoy);
        BigDecimal carbonKg = emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                empresaId, rango.inicio(), rango.fin());
        return HuellasCarbono.toneladasDesdeKg(Optional.ofNullable(carbonKg).orElse(BigDecimal.ZERO));
    }
}
