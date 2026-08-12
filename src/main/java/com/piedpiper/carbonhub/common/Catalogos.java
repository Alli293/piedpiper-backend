package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.exceptions.ApiException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utilidad para resolver valores de texto contra catálogos de enums,
 * de forma tolerante (case-insensitive, con trim).
 */
public final class Catalogos {

    private Catalogos() {
    }

    public static <E extends Enum<E>> Optional<E> desde(Class<E> tipo, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }

    /**
     * Valida una lista de valores contra un catálogo de enum (sin duplicados, todos válidos) y
     * devuelve el conjunto resuelto. La deduplicación normaliza mayúsculas/espacios antes de
     * comparar, para que valores como "manufactura" y "MANUFACTURA " se detecten como el mismo.
     */
    public static <E extends Enum<E>> Set<E> resolverConjunto(Class<E> tipo, List<String> valores,
                                                              Supplier<ApiException> siHayDuplicados,
                                                              Function<List<String>, ApiException> siHayInvalidos) {
        List<String> normalizados = valores.stream()
                .map(valor -> valor == null ? null : valor.trim().toUpperCase())
                .toList();
        if (normalizados.size() != new HashSet<>(normalizados).size()) {
            throw siHayDuplicados.get();
        }
        List<String> invalidos = valores.stream()
                .filter(valor -> desde(tipo, valor).isEmpty())
                .toList();
        if (!invalidos.isEmpty()) {
            throw siHayInvalidos.apply(invalidos);
        }
        return valores.stream()
                .map(valor -> desde(tipo, valor).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
    }
}
