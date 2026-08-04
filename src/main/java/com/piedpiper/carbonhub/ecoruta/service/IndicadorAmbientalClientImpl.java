package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.ecoruta.models.dtos.CertificacionActivaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación real del cliente de indicadores ambientales.
 * Consulta las certificaciones activas y vigentes de cada empresa
 * a través del CertificacionRepository.
 */
@Component
public class IndicadorAmbientalClientImpl implements IndicadorAmbientalClient {

    private static final Logger log = LoggerFactory.getLogger(IndicadorAmbientalClientImpl.class);

    private final CertificacionRepository certificacionRepository;

    public IndicadorAmbientalClientImpl(CertificacionRepository certificacionRepository) {
        this.certificacionRepository = certificacionRepository;
    }

    @Override
    public Map<UUID, IndicadorAmbientalDTO> consultarIndicadores(List<UUID> empresaIds) {
        log.debug("Consultando indicadores ambientales para {} empresas", empresaIds.size());

        Map<UUID, IndicadorAmbientalDTO> resultado = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        for (UUID empresaId : empresaIds) {
            List<Certificacion> certificaciones = certificacionRepository
                    .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                            empresaId, EstadoCertificacion.ACTIVA, hoy);

            if (certificaciones.isEmpty()) {
                continue;
            }

            List<CertificacionActivaDTO> activas = certificaciones.stream()
                    .map(this::mapToCertificacionActiva)
                    .toList();

            IndicadorAmbientalDTO dto = new IndicadorAmbientalDTO(empresaId, activas, Instant.now());
            resultado.put(empresaId, dto);
        }

        log.debug("Indicadores ambientales obtenidos para {}/{} empresas", resultado.size(), empresaIds.size());
        return resultado;
    }

    private CertificacionActivaDTO mapToCertificacionActiva(Certificacion cert) {
        return new CertificacionActivaDTO(
                cert.getId(),
                cert.getTipo().name(),
                cert.getEstado().name(),
                cert.getFechaEmision()
        );
    }
}
