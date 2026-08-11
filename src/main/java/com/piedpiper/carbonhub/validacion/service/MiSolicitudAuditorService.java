package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.validacion.mappers.ValidacionAuditorMapper;
import com.piedpiper.carbonhub.validacion.models.dtos.MiSolicitudAuditorResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lectura del estado de la propia solicitud de validación para la pantalla de espera del auditor
 * (`/auditor/validacion-pendiente`). Separado de {@link ValidacionAuditorService} porque ese
 * servicio es exclusivamente para acciones del administrador de plataforma sobre solicitudes
 * ajenas; esto es una consulta del auditor sobre la suya propia.
 */
@Service
public class MiSolicitudAuditorService {

    private final SolicitudValidacionRepository solicitudValidacionRepository;
    private final ValidacionAuditorMapper validacionAuditorMapper;

    public MiSolicitudAuditorService(SolicitudValidacionRepository solicitudValidacionRepository,
                                     ValidacionAuditorMapper validacionAuditorMapper) {
        this.solicitudValidacionRepository = solicitudValidacionRepository;
        this.validacionAuditorMapper = validacionAuditorMapper;
    }

    @Transactional(readOnly = true)
    public MiSolicitudAuditorResponseDTO obtener(UUID usuarioId) {
        SolicitudValidacion solicitud = solicitudValidacionRepository
                .findTopByAuditorIdOrderByFechaSolicitudDesc(usuarioId)
                .orElseThrow(ApiException::solicitudValidacionNoEncontrada);

        return validacionAuditorMapper.aMiSolicitudDto(solicitud);
    }
}
