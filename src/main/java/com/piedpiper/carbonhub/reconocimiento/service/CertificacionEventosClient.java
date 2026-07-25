package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;

public interface CertificacionEventosClient {

    void enviar(EventoCertificacionRequestDTO request);
}
