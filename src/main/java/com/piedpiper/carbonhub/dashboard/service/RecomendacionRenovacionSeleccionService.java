package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Selecciona, de forma puramente determinista, cuál certificación con
 * alerta activa debe renovarse primero (PP-72): menor cantidad de días
 * restantes; en caso de empate, mayor impacto en huella. Separada de
 * {@link DashboardRecomendacionService} para poder probarla sin tocar la
 * IA ni la base de datos — la IA nunca decide el orden, solo redacta la
 * justificación de una decisión que ya se tomó acá.
 */
@Service
public class RecomendacionRenovacionSeleccionService {

    public Optional<CertAlertaDTO> seleccionarPrioritaria(List<CertAlertaDTO> alertas) {
        return alertas.stream()
                .min(Comparator.comparingInt(CertAlertaDTO::getDiasRestantes)
                        .thenComparing(CertAlertaDTO::getImpactoHuellaT, Comparator.reverseOrder()));
    }
}
