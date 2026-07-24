package com.piedpiper.carbonhub.ecoruta.config;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CatalogoInsigniasEcoRuta {

    public static final String EVENTO_PRIMER_ITINERARIO = "primer_itinerario_generado";
    public static final String EVENTO_CINCO_ITINERARIOS = "cinco_itinerarios_generados";
    public static final String EVENTO_DIEZ_ITINERARIOS = "diez_itinerarios_generados";
    public static final String EVENTO_PRIMER_ITINERARIO_SOSTENIBLE =
            "primer_itinerario_sostenible";
    public static final String EVENTO_USUARIO_RECURRENTE = "usuario_recurrente";
    public static final String EVENTO_EXPLORADOR_PROVINCIAS = "explorador_de_provincias";

    private final Map<String, ReglaInsigniaEcoRuta> reglasPorEvento;
    private final Map<Long, ReglaInsigniaEcoRuta> reglasPorId;

    public CatalogoInsigniasEcoRuta() {
        Collection<ReglaInsigniaEcoRuta> reglas = java.util.List.of(
                new ReglaInsigniaEcoRuta(1L, EVENTO_PRIMER_ITINERARIO, "Primer itinerario",
                        "Creaste tu primer itinerario sostenible con EcoRuta."),
                new ReglaInsigniaEcoRuta(2L, EVENTO_PRIMER_ITINERARIO_SOSTENIBLE,
                        "EcoScore Excelente",
                        "Completaste tu primer itinerario sostenible."),
                new ReglaInsigniaEcoRuta(3L, EVENTO_CINCO_ITINERARIOS, "Exploradora local",
                        "Planificaste 5 itinerarios de bajo impacto."),
                new ReglaInsigniaEcoRuta(4L, EVENTO_USUARIO_RECURRENTE, "Usuario recurrente",
                        "Volviste a planificar rutas sostenibles con EcoRuta."),
                new ReglaInsigniaEcoRuta(5L, EVENTO_DIEZ_ITINERARIOS, "10 itinerarios",
                        "Planificaste 10 itinerarios de bajo impacto."),
                new ReglaInsigniaEcoRuta(6L, EVENTO_EXPLORADOR_PROVINCIAS,
                        "Explorador de provincias",
                        "Incluiste varias provincias en tus itinerarios de bajo impacto."));
        this.reglasPorEvento = reglas.stream()
                .collect(Collectors.toUnmodifiableMap(ReglaInsigniaEcoRuta::eventoDesbloqueo,
                        Function.identity()));
        this.reglasPorId = reglas.stream()
                .collect(Collectors.toUnmodifiableMap(ReglaInsigniaEcoRuta::idInsignia,
                        Function.identity()));
    }

    public Optional<ReglaInsigniaEcoRuta> buscarPorEvento(String eventoDesbloqueo) {
        if (eventoDesbloqueo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reglasPorEvento.get(eventoDesbloqueo.trim()));
    }

    public Optional<ReglaInsigniaEcoRuta> buscarPorId(Long idInsignia) {
        return Optional.ofNullable(reglasPorId.get(idInsignia));
    }
}
