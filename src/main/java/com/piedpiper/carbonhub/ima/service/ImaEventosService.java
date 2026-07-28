package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository.CategoriaMensual;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Detecta eventos sobre la serie del IMA mediante reglas deterministas.
 * No interviene la IA: dada la misma serie, el resultado siempre es el mismo.
 */
@Service
public class ImaEventosService {

    private static final String[] MESES_ES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private static final Map<CategoriaEmision, String> ETIQUETA_CATEGORIA = Map.of(
            CategoriaEmision.ELECTRICIDAD, "electricidad",
            CategoriaEmision.FLOTA, "flota vehicular",
            CategoriaEmision.VUELO, "vuelos",
            CategoriaEmision.ENVIO, "envíos de carga");

    private final EmisionRepository emisionRepository;

    public ImaEventosService(EmisionRepository emisionRepository) {
        this.emisionRepository = emisionRepository;
    }

    /**
     * Devuelve los eventos detectados en la ventana, ordenados cronológicamente.
     * Si en un mismo mes coinciden varios, se listan todos.
     */
    public List<ImaEventoDTO> detectar(UUID empresaId, List<ImaTendenciaPuntoDTO> serie,
                                       YearMonth desde, YearMonth hasta) {
        List<ImaEventoDTO> eventos = new ArrayList<>();

        eventos.addAll(detectarCrucesSector(serie));
        detectarMayorVariacion(serie).ifPresent(eventos::add);

        Map<YearMonth, Set<CategoriaEmision>> categoriasPorMes = cargarCategoriasPorMes(empresaId);
        eventos.addAll(detectarHuecosDatos(serie, categoriasPorMes));
        eventos.addAll(detectarNuevasCategorias(categoriasPorMes, desde, hasta));

        eventos.sort(Comparator.comparing(ImaEventoDTO::getMes)
                .thenComparing(evento -> evento.getTipo().name()));
        return eventos;
    }

    /**
     * Marca los meses en que la línea de la empresa cruza la del sector.
     * Solo compara puntos consecutivos donde ambas series tienen dato; si no hay
     * promedio sectorial en la ventana, no se emite ningún evento de este tipo.
     */
    private List<ImaEventoDTO> detectarCrucesSector(List<ImaTendenciaPuntoDTO> serie) {
        List<ImaEventoDTO> eventos = new ArrayList<>();
        ImaTendenciaPuntoDTO anterior = null;

        for (ImaTendenciaPuntoDTO actual : serie) {
            if (!tieneAmbos(actual)) {
                // Un mes sin ambos valores corta la comparación: el próximo punto
                // no debe compararse contra un mes que ya no es su predecesor.
                anterior = null;
                continue;
            }
            // Solo se compara contra el mes calendario inmediatamente anterior:
            // si hay un hueco en medio, no se atraviesa para inventar un cruce.
            if (anterior != null && esMesSiguiente(anterior, actual)) {
                detectarCruce(anterior, actual).ifPresent(eventos::add);
            }
            anterior = actual;
        }
        return eventos;
    }

    /** Cruce entre {@code anterior} y {@code actual} si el lado respecto al sector cambió. */
    private Optional<ImaEventoDTO> detectarCruce(ImaTendenciaPuntoDTO anterior, ImaTendenciaPuntoDTO actual) {
        int signoAnterior = comparar(anterior);
        int signoActual = comparar(actual);
        // Solo hay cruce si el lado cambia; empatar no cuenta como cruce.
        if (signoAnterior == 0 || signoActual == 0 || signoAnterior == signoActual) {
            return Optional.empty();
        }
        return Optional.of(ImaEventoDTO.builder()
                .mes(actual.getMes())
                .tipo(TipoEventoIma.CRUCE_SECTOR)
                .texto(signoActual > 0
                        ? "En " + nombrarMes(actual.getMes())
                          + " tu IMA superó el promedio de tu sector."
                        : "En " + nombrarMes(actual.getMes())
                          + " tu IMA quedó por debajo del promedio de tu sector.")
                .build());
    }

    /** true si {@code actual} es exactamente el mes calendario siguiente a {@code anterior}. */
    private boolean esMesSiguiente(ImaTendenciaPuntoDTO anterior, ImaTendenciaPuntoDTO actual) {
        return YearMonth.parse(anterior.getMes()).plusMonths(1)
                .equals(YearMonth.parse(actual.getMes()));
    }

    /**
     * Un único marcador en el mes con la mayor variación absoluta respecto al mes
     * anterior con dato. En caso de empate se conserva la primera ocurrencia.
     */
    private Optional<ImaEventoDTO> detectarMayorVariacion(List<ImaTendenciaPuntoDTO> serie) {
        BigDecimal mayorDelta = null;
        BigDecimal deltaConSigno = null;
        String mesMayor = null;
        ImaTendenciaPuntoDTO anterior = null;

        for (ImaTendenciaPuntoDTO actual : serie) {
            if (actual.getImaEmpresa() == null) {
                // Un mes sin dato corta la comparación: el próximo punto con dato no debe
                // medirse contra un mes que ya no es su predecesor inmediato. Sin este reset
                // se reportaría como "mayor cambio" un delta que en realidad atraviesa un hueco.
                anterior = null;
                continue;
            }
            if (anterior != null && esMesSiguiente(anterior, actual)) {
                BigDecimal delta = actual.getImaEmpresa().subtract(anterior.getImaEmpresa());
                BigDecimal absoluto = delta.abs();
                if (mayorDelta == null || absoluto.compareTo(mayorDelta) > 0) {
                    mayorDelta = absoluto;
                    deltaConSigno = delta;
                    mesMayor = actual.getMes();
                }
            }
            anterior = actual;
        }

        if (mesMayor == null || mayorDelta.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        String signo = deltaConSigno.signum() > 0 ? "+" : "-";
        return Optional.of(ImaEventoDTO.builder()
                .mes(mesMayor)
                .tipo(TipoEventoIma.MAYOR_VARIACION)
                .texto("En " + nombrarMes(mesMayor) + " registraste tu mayor cambio de IMA ("
                        + signo + deltaConSigno.abs().stripTrailingZeros().toPlainString() + ").")
                .build());
    }

    /** Meses de la ventana sin ningún registro de emisión. */
    private List<ImaEventoDTO> detectarHuecosDatos(List<ImaTendenciaPuntoDTO> serie,
                                                   Map<YearMonth, Set<CategoriaEmision>> categoriasPorMes) {
        List<ImaEventoDTO> eventos = new ArrayList<>();
        for (ImaTendenciaPuntoDTO punto : serie) {
            YearMonth periodo = YearMonth.parse(punto.getMes());
            if (categoriasPorMes.getOrDefault(periodo, Set.of()).isEmpty()) {
                eventos.add(ImaEventoDTO.builder()
                        .mes(punto.getMes())
                        .tipo(TipoEventoIma.HUECO_DATOS)
                        .texto("En " + nombrarMes(punto.getMes())
                                + " no registraste emisiones; bajó tu consistencia.")
                        .build());
            }
        }
        return eventos;
    }

    /**
     * Primera aparición de cada categoría. Se recorre el historial completo disponible,
     * de modo que una categoría ya registrada antes de la ventana no genera evento.
     */
    private List<ImaEventoDTO> detectarNuevasCategorias(Map<YearMonth, Set<CategoriaEmision>> categoriasPorMes,
                                                        YearMonth desde, YearMonth hasta) {
        List<ImaEventoDTO> eventos = new ArrayList<>();
        Set<CategoriaEmision> yaVistas = new HashSet<>();

        for (Map.Entry<YearMonth, Set<CategoriaEmision>> entrada : new TreeMap<>(categoriasPorMes).entrySet()) {
            YearMonth periodo = entrada.getKey();
            for (CategoriaEmision categoria : ordenarPorNombre(entrada.getValue())) {
                if (yaVistas.add(categoria)
                        && !periodo.isBefore(desde) && !periodo.isAfter(hasta)) {
                    eventos.add(ImaEventoDTO.builder()
                            .mes(periodo.toString())
                            .tipo(TipoEventoIma.NUEVA_CATEGORIA)
                            .texto("En " + nombrarMes(periodo.toString()) + " empezaste a registrar "
                                    + ETIQUETA_CATEGORIA.getOrDefault(categoria, categoria.name().toLowerCase())
                                    + ".")
                            .build());
                }
            }
        }
        return eventos;
    }

    /**
     * Carga las categorías registradas por la empresa en la totalidad de su historial, no solo en la
     * ventana. Es intencional: NUEVA_CATEGORIA necesita saber si una categoría ya existía
     * antes de la ventana para no marcarla como nueva. Por eso no recibe la fecha de inicio
     * de la ventana; consulta desde el comienzo del historial.
     */
    private Map<YearMonth, Set<CategoriaEmision>> cargarCategoriasPorMes(UUID empresaId) {
        List<CategoriaMensual> filas = emisionRepository.listarCategoriasPorMes(
                empresaId, LocalDate.of(1970, Month.JANUARY, 1));

        Map<YearMonth, Set<CategoriaEmision>> porMes = new HashMap<>();
        for (CategoriaMensual fila : filas) {
            porMes.computeIfAbsent(YearMonth.of(fila.getAnio(), fila.getMes()), k -> new HashSet<>())
                    .add(fila.getCategoria());
        }
        return porMes;
    }

    /** Ordena las categorías de un mes por nombre, para que el resultado sea determinista. */
    private List<CategoriaEmision> ordenarPorNombre(Set<CategoriaEmision> categorias) {
        List<CategoriaEmision> ordenadas = new ArrayList<>(categorias);
        ordenadas.sort(Comparator.comparing(CategoriaEmision::name));
        return ordenadas;
    }

    private boolean tieneAmbos(ImaTendenciaPuntoDTO punto) {
        return punto.getImaEmpresa() != null && punto.getImaPromedioSector() != null;
    }

    private int comparar(ImaTendenciaPuntoDTO punto) {
        return punto.getImaEmpresa().compareTo(punto.getImaPromedioSector());
    }

    /** Convierte un período ISO {@code YYYY-MM} en el nombre del mes en español. */
    private String nombrarMes(String periodo) {
        YearMonth ym = YearMonth.parse(periodo);
        return MESES_ES[ym.getMonthValue() - 1] + " " + ym.getYear();
    }
}
