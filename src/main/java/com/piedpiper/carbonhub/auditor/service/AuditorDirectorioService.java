package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.OrdenamientoAuditores;
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

import java.util.List;

@Service
public class AuditorDirectorioService {

    private static final int TAMANIO_MINIMO = 1;
    private static final int TAMANIO_MAXIMO = 50;
    private static final int TAMANIO_POR_DEFECTO = 12;
    private static final int LONGITUD_MINIMA_BUSQUEDA = 2;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final PerfilAuditorMapper mapper;

    public AuditorDirectorioService(PerfilAuditorRepository perfilAuditorRepository,
                                    PerfilAuditorMapper mapper) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginaAuditoresResponseDTO listar(String terminoBusqueda, int pagina, Integer tamanioPagina,
                                             String ordenamiento) {
        String termino = normalizarTermino(terminoBusqueda);
        int tamanio = normalizarTamanio(tamanioPagina);
        int numeroPagina = Math.max(pagina, 0);
        Pageable pageable = PageRequest.of(numeroPagina, tamanio, ordenar(ordenamiento));

        Page<PerfilAuditor> resultado = perfilAuditorRepository.buscarDirectorio(
                Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO, termino, pageable);

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
}
