package com.piedpiper.carbonhub.auditoria.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación temporal de {@link CertificacionActivaConsulta}.
 *
 * <p>PP-58 (certificaciones) todavía no está en develop, así que no existe una tabla de
 * certificaciones que consultar. Hasta entonces ninguna empresa tiene certificación activa y toda
 * solicitud de auditoría se clasifica como INICIAL, respetando el período que envía el cliente.
 *
 * <p>Al integrar PP-58 se reemplaza este bean por uno que consulte la certificación vigente de la
 * empresa; el resto del flujo (tipo RENOVACION y cálculo de periodoInicio) ya está implementado en
 * {@link SolicitudAuditoriaService}.
 */
@Service
public class CertificacionActivaConsultaPorDefecto implements CertificacionActivaConsulta {

    @Override
    public Optional<LocalDate> fechaVencimientoCertificacionActiva(UUID empresaId) {
        return Optional.empty();
    }
}
