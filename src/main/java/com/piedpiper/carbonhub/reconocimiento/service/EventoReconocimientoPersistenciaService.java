package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoReconocimientoPersistenciaService {

    private final EventoReconocimientoRepository eventoReconocimientoRepository;

    public EventoReconocimientoPersistenciaService(
            EventoReconocimientoRepository eventoReconocimientoRepository) {
        this.eventoReconocimientoRepository = eventoReconocimientoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoReconocimiento guardarNuevo(EventoReconocimiento evento) {
        return eventoReconocimientoRepository.saveAndFlush(evento);
    }
}
