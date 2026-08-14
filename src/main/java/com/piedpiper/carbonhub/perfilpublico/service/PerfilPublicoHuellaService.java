package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EvolucionHuellaDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PuntoHuellaDTO;
import com.piedpiper.carbonhub.perfilpublico.models.enums.RangoPeriodoHuella;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PerfilPublicoHuellaService {

    private static final Logger log = LoggerFactory.getLogger(PerfilPublicoHuellaService.class);
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final Locale LOCALE_ES_CR = Locale.forLanguageTag("es-CR");

    private final EmpresaRepository empresaRepository;
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final EmisionRepository emisionRepository;

    public PerfilPublicoHuellaService(EmpresaRepository empresaRepository,
                                      SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                      EmisionRepository emisionRepository) {
        this.empresaRepository = empresaRepository;
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.emisionRepository = emisionRepository;
    }

    @Transactional(readOnly = true)
    public EvolucionHuellaDTO obtener(String slugOriginal, String rangoOriginal) {
        String slug = slugOriginal == null ? "" : slugOriginal.toLowerCase();
        Empresa empresa = empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO)
                .orElseThrow(() -> new PerfilNoEncontradoException(
                        "El perfil que buscas no existe o ya no está disponible."));

        RangoPeriodoHuella rango = RangoPeriodoHuella.desde(rangoOriginal)
                .orElse(RangoPeriodoHuella.POR_DEFECTO);

        List<PuntoCalculado> puntos = solicitudAuditoriaRepository
                .listarPeriodosVerificados(
                        empresa.getId(),
                        EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                        EstadoCertificacion.ACTIVA)
                .stream()
                .map(solicitud -> punto(empresa.getId(), solicitud))
                .filter(PuntoCalculado::valido)
                .toList();

        List<PuntoCalculado> filtrados = aplicarRango(puntos, rango);
        List<PuntoHuellaDTO> serie = serieConVariacion(filtrados);

        return new EvolucionHuellaDTO(
                rango.getValor(),
                tendencia(filtrados),
                serie
        );
    }

    private PuntoCalculado punto(UUID empresaId, SolicitudAuditoria solicitud) {
        List<Emision> emisiones = emisionRepository.findAllByEmpresaIdAndPeriodo(
                empresaId,
                solicitud.getPeriodoInicio(),
                solicitud.getPeriodoFin().plusDays(1));

        BigDecimal totalKg = BigDecimal.ZERO;
        boolean tieneValorValido = false;

        for (Emision emision : emisiones) {
            BigDecimal carbonKg = emision.getCarbonKg();
            if (carbonKg == null || carbonKg.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Se omitio una emision con huella invalida en auditoria {}: emision {}",
                        solicitud.getId(), emision.getId());
                continue;
            }
            totalKg = totalKg.add(carbonKg);
            tieneValorValido = true;
        }

        if (!tieneValorValido) {
            log.warn("Se omitio el periodo verificado {} porque no tiene huellas validas.",
                    solicitud.getId());
        }

        return new PuntoCalculado(etiquetaPeriodo(solicitud), totalKg, tieneValorValido);
    }

    private String etiquetaPeriodo(SolicitudAuditoria solicitud) {
        if (solicitud.getPeriodoInicio().getYear() == solicitud.getPeriodoFin().getYear()
                && solicitud.getPeriodoInicio().getDayOfYear() == 1
                && solicitud.getPeriodoFin().getDayOfYear() == solicitud.getPeriodoFin().lengthOfYear()) {
            return String.valueOf(solicitud.getPeriodoInicio().getYear());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", LOCALE_ES_CR);
        String inicio = solicitud.getPeriodoInicio()
                .format(formatter)
                .replace(".", "");
        String fin = solicitud.getPeriodoFin()
                .format(formatter)
                .replace(".", "");
        return capitalizar(inicio) + " - " + capitalizar(fin);
    }

    private String capitalizar(String valor) {
        if (valor.isBlank()) {
            return valor;
        }
        return valor.substring(0, 1).toUpperCase(LOCALE_ES_CR) + valor.substring(1);
    }

    private List<PuntoCalculado> aplicarRango(List<PuntoCalculado> puntos, RangoPeriodoHuella rango) {
        if (rango == RangoPeriodoHuella.HISTORICO
                || puntos.size() <= rango.getCantidadPeriodos()) {
            return puntos;
        }
        return puntos.subList(puntos.size() - rango.getCantidadPeriodos(), puntos.size());
    }

    private List<PuntoHuellaDTO> serieConVariacion(List<PuntoCalculado> puntos) {
        List<PuntoHuellaDTO> serie = new ArrayList<>(puntos.size());
        BigDecimal anteriorKg = null;

        for (PuntoCalculado punto : puntos) {
            serie.add(new PuntoHuellaDTO(
                    punto.periodo(),
                    HuellasCarbono.toneladasDesdeKg(punto.huellaKg()),
                    variacion(anteriorKg, punto.huellaKg())
            ));
            anteriorKg = punto.huellaKg();
        }

        return serie;
    }

    private BigDecimal variacion(BigDecimal anteriorKg, BigDecimal actualKg) {
        if (anteriorKg == null) {
            return null;
        }
        if (anteriorKg.compareTo(BigDecimal.ZERO) == 0) {
            return actualKg.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO.setScale(1) : null;
        }
        return actualKg.subtract(anteriorKg)
                .multiply(CIEN)
                .divide(anteriorKg, 1, RoundingMode.HALF_UP);
    }

    private String tendencia(List<PuntoCalculado> puntos) {
        if (puntos.size() < 2) {
            return "sin_cambio";
        }

        BigDecimal primero = puntos.get(0).huellaKg();
        BigDecimal ultimo = puntos.get(puntos.size() - 1).huellaKg();
        int comparacion = ultimo.compareTo(primero);
        if (comparacion < 0) {
            return "reduccion";
        }
        if (comparacion > 0) {
            return "aumento";
        }
        return "sin_cambio";
    }

    private record PuntoCalculado(String periodo, BigDecimal huellaKg, boolean valido) {
    }
}
