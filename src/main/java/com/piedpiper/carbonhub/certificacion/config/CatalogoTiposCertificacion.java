package com.piedpiper.carbonhub.certificacion.config;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogo de tipos de certificacion. Vive en codigo (y no en una tabla) porque
 * el proyecto corre con {@code ddl-auto=update} y sin herramienta de migracion,
 * de modo que no hay forma confiable de sembrar datos de referencia. Sigue el
 * mismo patron que {@code CatalogoInsigniasEcoRuta}.
 */
@Component
public class CatalogoTiposCertificacion {

    private final Map<TipoCertificacion, DefinicionCertificacion> definicionesPorTipo;

    public CatalogoTiposCertificacion() {
        Collection<DefinicionCertificacion> definiciones = List.of(
                new DefinicionCertificacion(TipoCertificacion.INVENTARIO_GEI,
                        "Inventario de GEI",
                        "La organizacion cuantifico y reporto su inventario de gases de efecto "
                                + "invernadero para el periodo auditado.",
                        12, TipoLogroOpenBadges.QUALITY_ASSURANCE_CREDENTIAL,
                        "Presentar un inventario de emisiones completo para el periodo auditado y "
                                + "superar la auditoria de CarbonHub."),
                new DefinicionCertificacion(TipoCertificacion.REDUCCION_EMISIONES,
                        "Reduccion de Emisiones",
                        "La organizacion gestiono y cuantifico acciones de reduccion sobre sus "
                                + "fuentes de emision.",
                        12, TipoLogroOpenBadges.CERTIFICATE,
                        "Mantener un inventario vigente y demostrar reducciones cuantificadas "
                                + "respecto del periodo anterior."),
                new DefinicionCertificacion(TipoCertificacion.REDUCCION_PLUS,
                        "Reduccion Plus",
                        "La organizacion supero las metas de reduccion establecidas para su sector.",
                        12, TipoLogroOpenBadges.CERTIFICATE,
                        "Cumplir los criterios de Reduccion de Emisiones y superar la meta "
                                + "sectorial definida para el periodo."),
                new DefinicionCertificacion(TipoCertificacion.CARBONO_NEUTRAL,
                        "Carbono Neutral",
                        "La organizacion compenso la totalidad de sus emisiones residuales del "
                                + "periodo auditado.",
                        12, TipoLogroOpenBadges.CERTIFICATION,
                        "Mantener inventario y reducciones vigentes y compensar el 100% de las "
                                + "emisiones residuales del periodo."),
                new DefinicionCertificacion(TipoCertificacion.CARBONO_NEUTRAL_PLUS,
                        "Carbono Neutral Plus",
                        "La organizacion compenso mas emisiones de las que genero en el periodo "
                                + "auditado.",
                        12, TipoLogroOpenBadges.CERTIFICATION,
                        "Cumplir los criterios de Carbono Neutral y acreditar una compensacion "
                                + "superior a las emisiones del periodo."),
                new DefinicionCertificacion(TipoCertificacion.ADAPTACION_CLIMATICA,
                        "Adaptacion Climatica",
                        "La organizacion implemento medidas de adaptacion y resiliencia frente al "
                                + "cambio climatico.",
                        24, TipoLogroOpenBadges.CERTIFICATE,
                        "Documentar un plan de adaptacion con medidas implementadas y verificables "
                                + "en la auditoria."),
                new DefinicionCertificacion(TipoCertificacion.HUELLA_PRODUCTO,
                        "Huella de Carbono de Producto",
                        "La organizacion cuantifico la huella de carbono de un producto a lo largo "
                                + "de su ciclo de vida.",
                        36, TipoLogroOpenBadges.QUALITY_ASSURANCE_CREDENTIAL,
                        "Presentar el analisis de ciclo de vida del producto y superar la auditoria "
                                + "de CarbonHub."));

        this.definicionesPorTipo = definiciones.stream()
                .collect(Collectors.toUnmodifiableMap(DefinicionCertificacion::tipo,
                        Function.identity()));
    }

    public Optional<DefinicionCertificacion> buscar(TipoCertificacion tipo) {
        if (tipo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definicionesPorTipo.get(tipo));
    }

    public List<DefinicionCertificacion> listar() {
        return definicionesPorTipo.values().stream()
                .sorted(java.util.Comparator.comparing(definicion -> definicion.tipo().ordinal()))
                .toList();
    }
}
