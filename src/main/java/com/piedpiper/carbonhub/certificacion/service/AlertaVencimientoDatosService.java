package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.models.dtos.AlertaVencimientoNotificacionDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve, dentro de una transaccion, todo lo que el correo de una alerta de vencimiento necesita
 * (PP-71). Se separa del servicio que envia para que el envio y sus reintentos ocurran fuera de la
 * transaccion, como pide la convencion de efectos externos.
 */
@Service
public class AlertaVencimientoDatosService {

    private final AlertaRepository alertaRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final String certificacionDetalleUrl;

    public AlertaVencimientoDatosService(
            AlertaRepository alertaRepository,
            CatalogoTiposCertificacion catalogoTiposCertificacion,
            @Value("${frontend.certificacion-detalle-url}") String certificacionDetalleUrl) {
        this.alertaRepository = alertaRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.certificacionDetalleUrl = certificacionDetalleUrl;
    }

    @Transactional(readOnly = true)
    public Optional<AlertaVencimientoNotificacionDTO> datosDe(UUID alertaId) {
        return alertaRepository.buscarConEmpresaYCertificacion(alertaId).map(this::aDto);
    }

    private AlertaVencimientoNotificacionDTO aDto(Alerta alerta) {
        LocalDate vencimiento = alerta.getCertificacion().getFechaVencimiento();
        return new AlertaVencimientoNotificacionDTO(
                alerta.getId(),
                alerta.getEmpresa().getCorreoCorporativo(),
                alerta.getEmpresa().getNombreEmpresa(),
                nombreDe(alerta),
                vencimiento,
                ChronoUnit.DAYS.between(LocalDate.now(ZonasHorarias.COSTA_RICA), vencimiento),
                alerta.getTipoAlerta().getCodigo(),
                urlDe(alerta));
    }

    /**
     * El nombre legible sale del catalogo, que es el mismo que se imprime en la credencial, para que
     * el correo y la certificacion no le digan cosas distintas al usuario. Si el tipo no estuviera en
     * el catalogo se cae al codigo del enum antes que dejar el correo sin nombre.
     */
    private String nombreDe(Alerta alerta) {
        return catalogoTiposCertificacion.buscar(alerta.getCertificacion().getTipo())
                .map(definicion -> definicion.nombre())
                .orElseGet(() -> alerta.getCertificacion().getTipo().getCodigo());
    }

    private String urlDe(Alerta alerta) {
        return certificacionDetalleUrl.replace("{slug}", alerta.getEmpresa().getSlug());
    }
}
