package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Nombre legible y urgencia de una certificacion respecto a su vencimiento.
 * Compartido entre los bloques del dashboard que necesitan presentar este
 * dato — el calendario de vencimientos (PP-77) y el panel de alertas
 * activas (PP-76) — para que las dos copias no diverjan (ya paso una vez
 * en PP-77, ver el historial de {@code CalendarioVencimientosService}).
 */
@Service
public class VencimientoPresentacionService {

    private static final String URGENCIA_VENCIDA = "vencida";

    private final CatalogoTiposCertificacion catalogoTiposCertificacion;

    public VencimientoPresentacionService(CatalogoTiposCertificacion catalogoTiposCertificacion) {
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
    }

    /**
     * El nombre legible vive en {@link CatalogoTiposCertificacion} — es el
     * mismo que ya usan la credencial OpenBadges y los correos de
     * vencimiento de PP-71.
     */
    public String nombreLegible(Certificacion certificacion) {
        return catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .map(DefinicionCertificacion::nombre)
                .orElseGet(certificacion.getTipo()::getCodigo);
    }

    /**
     * "vencida" es un valor propio de la presentacion, no de
     * {@link TipoAlerta}: una certificacion con {@code diasRestantes <= 0}
     * ya paso su fecha de vencimiento, un estado distinto de "esta por
     * vencer en los proximos 7/30/90 dias".
     */
    public String urgenciaPara(long diasRestantes) {
        if (diasRestantes <= 0) {
            return URGENCIA_VENCIDA;
        }

        return Arrays.stream(TipoAlerta.values())
                .filter(tipo -> diasRestantes <= tipo.getDias())
                .min(Comparator.comparingInt(TipoAlerta::getDias))
                .map(TipoAlerta::getCodigo)
                .orElse(TipoAlerta.DIAS_90.getCodigo());
    }
}
