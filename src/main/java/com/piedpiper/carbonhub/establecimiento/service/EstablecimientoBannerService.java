package com.piedpiper.carbonhub.establecimiento.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;
import com.piedpiper.carbonhub.establecimiento.models.dtos.EstablecimientoBannerResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resuelve el banner de origen (bandera + país) de un establecimiento para PP-95. Un
 * "establecimiento" acá es una {@link Empresa} registrada y verificada en la plataforma: el país
 * de origen es directamente {@code Empresa.pais} (ya capturado en la configuración inicial de la
 * empresa, PP-18) — PP-95 no agrega un campo nuevo ni un flujo de edición propio, solo lo expone
 * enriquecido con datos de países (ver {@link CountriesDevClientImpl} para la fuente elegida).
 */
@Service
public class EstablecimientoBannerService {

    private final EmpresaRepository empresaRepository;
    private final CountriesDevClient countriesDevClient;

    public EstablecimientoBannerService(EmpresaRepository empresaRepository,
                                        CountriesDevClient countriesDevClient) {
        this.empresaRepository = empresaRepository;
        this.countriesDevClient = countriesDevClient;
    }

    @Transactional(readOnly = true)
    public EstablecimientoBannerResponseDTO obtenerBanner(UUID empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "Este establecimiento no fue encontrado."));

        BannerOrigenDTO banner = countriesDevClient.consultarPais(empresa.getPais()).orElse(null);
        return new EstablecimientoBannerResponseDTO(banner);
    }
}
