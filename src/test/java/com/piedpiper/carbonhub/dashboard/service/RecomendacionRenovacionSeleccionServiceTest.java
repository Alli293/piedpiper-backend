package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecomendacionRenovacionSeleccionServiceTest {

    private final RecomendacionRenovacionSeleccionService service = new RecomendacionRenovacionSeleccionService();

    @Test
    void ordenaPorDiasRestantesAscendente() {
        CertAlertaDTO a30 = certAlerta("A", 30, "10");
        CertAlertaDTO a7 = certAlerta("B", 7, "5");
        CertAlertaDTO a90 = certAlerta("C", 90, "50");

        Optional<CertAlertaDTO> prioritaria = service.seleccionarPrioritaria(List.of(a30, a7, a90));

        assertThat(prioritaria).isPresent();
        assertThat(prioritaria.get().getNombreCertificacion()).isEqualTo("B");
    }

    @Test
    void enEmpateDeDiasPriorizaMayorImpactoEnHuella() {
        CertAlertaDTO impactoBajo = certAlerta("Bajo impacto", 7, "10");
        CertAlertaDTO impactoAlto = certAlerta("Alto impacto", 7, "50");

        Optional<CertAlertaDTO> prioritaria =
                service.seleccionarPrioritaria(List.of(impactoBajo, impactoAlto));

        assertThat(prioritaria).isPresent();
        assertThat(prioritaria.get().getNombreCertificacion()).isEqualTo("Alto impacto");
    }

    @Test
    void devuelveVacioSiNoHayAlertas() {
        assertThat(service.seleccionarPrioritaria(List.of())).isEmpty();
    }

    @Test
    void conUnaSolaAlertaLaDevuelveDirectamente() {
        CertAlertaDTO unica = certAlerta("Única", 15, "20");

        Optional<CertAlertaDTO> prioritaria = service.seleccionarPrioritaria(List.of(unica));

        assertThat(prioritaria).contains(unica);
    }

    private static CertAlertaDTO certAlerta(String nombre, int diasRestantes, String impactoHuellaT) {
        return new CertAlertaDTO(
                UUID.randomUUID(), nombre, LocalDate.now().plusDays(diasRestantes),
                diasRestantes, new BigDecimal(impactoHuellaT));
    }
}
