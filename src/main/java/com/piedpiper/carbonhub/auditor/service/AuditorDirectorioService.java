package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.FiltrarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.OrdenamientoAuditores;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditorDirectorioService {

    private static final int TAMANIO_MINIMO = 1;
    private static final int TAMANIO_MAXIMO = 50;
    private static final int TAMANIO_POR_DEFECTO = 12;
    private static final int LONGITUD_MINIMA_BUSQUEDA = 2;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;
    private static final BigDecimal CALIFICACION_MINIMA = BigDecimal.valueOf(1);
    private static final BigDecimal CALIFICACION_MAXIMA = BigDecimal.valueOf(5);
    private static final Set<EspecialidadAuditor> TODAS_ESPECIALIDADES =
            EnumSet.allOf(EspecialidadAuditor.class);

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final PerfilAuditorMapper mapper;

    public AuditorDirectorioService(PerfilAuditorRepository perfilAuditorRepository,
                                    PerfilAuditorMapper mapper) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginaAuditoresResponseDTO listar(FiltrarAuditoresRequestDTO filtros) {
        String termino = normalizarTermino(filtros.getTerminoBusqueda());
        int tamanio = normalizarTamanio(filtros.getTamanioPagina());
        int numeroPagina = Math.max(filtros.getPagina(), 0);
        Pageable pageable = PageRequest.of(numeroPagina, tamanio, ordenar(filtros.getOrdenamiento()));

        ProvinciaCR provincia = parsearZona(filtros.getZonaGeografica());
        BigDecimal calificacionMinima = validarCalificacion(filtros.getCalificacionMinima());
        Set<EspecialidadAuditor> especialidades = parsearEspecialidades(filtros.getEspecialidades());
        boolean filtrarEspecialidades = !especialidades.isEmpty();
        boolean soloDisponibles = Boolean.TRUE.equals(filtros.getSoloDisponibles());

        Page<PerfilAuditor> resultado = perfilAuditorRepository.buscarDirectorio(
                Rol.AUDITOR_CERTIFICADO,
                EstadoUsuario.ACTIVO,
                termino,
                provincia,
                calificacionMinima,
                soloDisponibles,
                filtrarEspecialidades,
                filtrarEspecialidades ? especialidades : TODAS_ESPECIALIDADES,
                pageable);

        List<AuditorResumenResponseDTO> contenido = resultado.getContent().stream()
                .map(mapper::aResumen)
                .toList();

        return new PaginaAuditoresResponseDTO(
                contenido,
                resultado.getTotalElements(),
                resultado.getNumber(),
                resultado.getTotalPages());
    }

    private Sort ordenar(String ordenamiento) {
        if (ordenamiento == null || ordenamiento.isBlank()) {
            return sortDe(OrdenamientoAuditores.CALIFICACION);
        }
        OrdenamientoAuditores orden = OrdenamientoAuditores.desde(ordenamiento)
                .orElseThrow(ApiException::ordenamientoAuditoresInvalido);
        return sortDe(orden);
    }

    private Sort sortDe(OrdenamientoAuditores orden) {
        return switch (orden) {
            case CALIFICACION -> Sort.by(Sort.Order.desc("calificacionPromedio").nullsLast());
            case AUDITORIAS_COMPLETADAS -> Sort.by(Sort.Order.desc("auditoriasCompletadas"));
            case TIEMPO_RESPUESTA -> Sort.by(Sort.Order.asc("tiempoRespuestaHoras").nullsLast());
        };
    }

    private String normalizarTermino(String terminoBusqueda) {
        if (terminoBusqueda == null) {
            return null;
        }
        String termino = terminoBusqueda.trim().replaceAll("[%_]", "");
        if (termino.length() < LONGITUD_MINIMA_BUSQUEDA || termino.length() > LONGITUD_MAXIMA_BUSQUEDA) {
            return null;
        }
        return termino;
    }

    private int normalizarTamanio(Integer tamanioPagina) {
        if (tamanioPagina == null || tamanioPagina < TAMANIO_MINIMO || tamanioPagina > TAMANIO_MAXIMO) {
            return TAMANIO_POR_DEFECTO;
        }
        return tamanioPagina;
    }

    private ProvinciaCR parsearZona(String zonaGeografica) {
        if (zonaGeografica == null || zonaGeografica.isBlank()) {
            return null;
        }
        return ProvinciaCR.desde(zonaGeografica)
                .orElseThrow(ApiException::zonaAuditorInvalida);
    }

    private BigDecimal validarCalificacion(BigDecimal calificacionMinima) {
        if (calificacionMinima == null) {
            return null;
        }
        if (calificacionMinima.compareTo(CALIFICACION_MINIMA) < 0
                || calificacionMinima.compareTo(CALIFICACION_MAXIMA) > 0) {
            throw ApiException.calificacionMinimaInvalida();
        }
        return calificacionMinima;
    }

    private Set<EspecialidadAuditor> parsearEspecialidades(List<String> especialidades) {
        if (especialidades == null) {
            return EnumSet.noneOf(EspecialidadAuditor.class);
        }
        return especialidades.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> EspecialidadAuditor.desde(valor)
                        .orElseThrow(ApiException::especialidadAuditorInvalida))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EspecialidadAuditor.class)));
    }
}
