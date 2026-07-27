package com.piedpiper.carbonhub.auditoria.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Punto de integración con PP-58 (emisión de certificaciones).
 *
 * <p>La creación de una solicitud de auditoría necesita saber si la empresa ya cuenta con una
 * certificación vigente para decidir en el servidor si la solicitud es INICIAL o RENOVACION, y en
 * el segundo caso para calcular el inicio del período como el día siguiente al vencimiento.
 *
 * <p>Mientras PP-58 no exista, la implementación por defecto responde que no hay certificación
 * activa, de modo que toda solicitud se registra como INICIAL. Cuando PP-58 llegue, basta con
 * publicar un bean que implemente esta interfaz consultando la certificación real de la empresa.
 */
public interface CertificacionActivaConsulta {

    Optional<LocalDate> fechaVencimientoCertificacionActiva(UUID empresaId);
}
