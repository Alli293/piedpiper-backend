package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Reserva la cuota de generación de itinerarios en su propia transacción ({@code REQUIRES_NEW}),
 * separada de {@link EcoRutaItinerarioService#generar(UUID)}. Es un bean aparte (no un método más
 * de {@code EcoRutaItinerarioService}) porque una auto-invocación dentro de la misma clase no pasa
 * por el proxy de Spring y {@code REQUIRES_NEW} no tendría efecto — el mismo problema que ya afectó
 * a {@code EmisionCertificacionService} (PP-70/PP-71). Así, si la llamada a la IA falla después,
 * el rollback de esa transacción no deshace este incremento: un intento fallido sí cuenta contra
 * la cuota, como está documentado desde el principio.
 */
@Service
public class ItinerarioCuotaService {

    private static final int MAX_GENERACIONES_POR_HORA = 5;

    private final PreferenciasViajeRepository preferenciasViajeRepository;

    public ItinerarioCuotaService(PreferenciasViajeRepository preferenciasViajeRepository) {
        this.preferenciasViajeRepository = preferenciasViajeRepository;
    }

    /**
     * Ventana fija de 1 hora, máximo {@value #MAX_GENERACIONES_POR_HORA} solicitudes — a diferencia
     * del límite silencioso de restablecimiento de contraseña (PP-29), este SÍ es visible: protege
     * la cuota de Gemini, no revela ninguna información sensible del usuario.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reservarGeneracion(UUID usuarioId) {
        PreferenciasViaje preferencias = preferenciasViajeRepository.findWithLockByUsuario_Id(usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No has completado tus preferencias de viaje todavía."));

        Instant ahora = Instant.now();
        Instant ventanaInicio = preferencias.getItinerarioGeneracionVentanaInicio();

        if (ventanaInicio == null || ventanaInicio.isBefore(ahora.minus(1, ChronoUnit.HOURS))) {
            preferencias.setItinerarioGeneracionVentanaInicio(ahora);
            preferencias.setItinerarioGeneracionContador(0);
        }

        if (preferencias.getItinerarioGeneracionContador() >= MAX_GENERACIONES_POR_HORA) {
            throw ApiException.itinerarioGeneracionesExcedidas();
        }

        preferencias.setItinerarioGeneracionContador(preferencias.getItinerarioGeneracionContador() + 1);
        preferenciasViajeRepository.saveAndFlush(preferencias);
    }
}
