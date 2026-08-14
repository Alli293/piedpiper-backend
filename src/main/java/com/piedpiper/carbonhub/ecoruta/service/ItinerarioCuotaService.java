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

    /**
     * Cada mensaje del chat de refinamiento (PP-88) dispara su propia llamada a Gemini, igual que
     * una generación — más generoso que {@link #MAX_GENERACIONES_POR_HORA} porque una sesión de
     * ajuste real conversa varias veces sobre el mismo itinerario.
     */
    private static final int MAX_REFINAMIENTOS_POR_HORA = 20;

    private final PreferenciasViajeRepository preferenciasViajeRepository;

    public ItinerarioCuotaService(PreferenciasViajeRepository preferenciasViajeRepository) {
        this.preferenciasViajeRepository = preferenciasViajeRepository;
    }

    /**
     * Ventana fija de 1 hora, máximo {@value #MAX_GENERACIONES_POR_HORA} solicitudes — a diferencia
     * del límite silencioso de restablecimiento de contraseña (PP-29), este SÍ es visible: protege
     * la cuota de Gemini, no revela ninguna información sensible del usuario.
     * <p>
     * Usa {@code findByUsuario_IdForUpdate} (mismo patrón que {@code UsuarioRepository}) para que
     * el lookup tome un {@code PESSIMISTIC_WRITE} sobre la fila: dos solicitudes concurrentes del
     * mismo usuario ya no pueden leer el mismo contador antes de que alguna confirme, que era el
     * escenario que señaló Ariela en la revisión — la segunda solicitud espera el lock y ve el
     * contador ya incrementado por la primera.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reservarGeneracion(UUID usuarioId) {
        PreferenciasViaje preferencias = preferenciasViajeRepository.findByUsuario_IdForUpdate(usuarioId)
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

    /**
     * Mismo mecanismo que {@link #reservarGeneracion}, ventana y contador propios, para el chat de
     * refinamiento (PP-88) — señalado en revisión: antes ningún límite protegía la cuota de Gemini
     * en ese endpoint.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reservarRefinamiento(UUID usuarioId) {
        PreferenciasViaje preferencias = preferenciasViajeRepository.findByUsuario_IdForUpdate(usuarioId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No has completado tus preferencias de viaje todavía."));

        Instant ahora = Instant.now();
        Instant ventanaInicio = preferencias.getItinerarioRefinamientoVentanaInicio();

        if (ventanaInicio == null || ventanaInicio.isBefore(ahora.minus(1, ChronoUnit.HOURS))) {
            preferencias.setItinerarioRefinamientoVentanaInicio(ahora);
            preferencias.setItinerarioRefinamientoContador(0);
        }

        if (preferencias.getItinerarioRefinamientoContador() >= MAX_REFINAMIENTOS_POR_HORA) {
            throw ApiException.itinerarioRefinamientosExcedidos();
        }

        preferencias.setItinerarioRefinamientoContador(preferencias.getItinerarioRefinamientoContador() + 1);
        preferenciasViajeRepository.saveAndFlush(preferencias);
    }
}
