package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionComparacionMapper;
import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmisionComparacionService {
    private static final BigDecimal KG_POR_TONELADA = new BigDecimal("1000");
    private static final BigDecimal UMBRAL_CERCA = new BigDecimal("80.0");
    private static final BigDecimal UMBRAL_SUPERADO = new BigDecimal("100.0");

    private final EmisionRepository emisionRepository;
    private final LimiteEmisionesRepository limiteEmisionesRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmisionComparacionMapper emisionComparacionMapper;

    public EmisionComparacionService(EmisionRepository emisionRepository,
                                     LimiteEmisionesRepository limiteEmisionesRepository,
                                     UsuarioRepository usuarioRepository,
                                     EmisionComparacionMapper emisionComparacionMapper) {
        this.emisionRepository = emisionRepository;
        this.limiteEmisionesRepository = limiteEmisionesRepository;
        this.usuarioRepository = usuarioRepository;
        this.emisionComparacionMapper = emisionComparacionMapper;
    }

    @Transactional(readOnly = true)
    public ComparacionEmisionesResponseDTO comparar(UUID usuarioId, Integer anio) {
        Integer anioComparar = anio == null ? Year.now().getValue() : anio;
        validarAnio(anioComparar);

        UUID empresaId = empresaId(usuarioId);
        BigDecimal huellaKg = Optional.ofNullable(
                emisionRepository.sumCarbonKgByEmpresaIdAndAnio(empresaId, anioComparar)
        ).orElse(BigDecimal.ZERO);
        BigDecimal huellaT = huellaKg.divide(KG_POR_TONELADA, 4, RoundingMode.HALF_UP);

        return limiteEmisionesRepository.findByEmpresaIdAndAnio(empresaId, anioComparar)
                .map(limite -> compararConLimite(anioComparar, huellaT, limite))
                .orElseGet(() -> emisionComparacionMapper.toDto(
                        anioComparar,
                        huellaT,
                        null,
                        null,
                        "sin_limite",
                        "No se ha declarado un limite para " + anioComparar + "."
                ));
    }

    private ComparacionEmisionesResponseDTO compararConLimite(
            Integer anio,
            BigDecimal huellaT,
            LimiteEmisiones limite) {
        BigDecimal limiteT = limite.getLimiteMt();
        BigDecimal porcentaje = huellaT
                .multiply(new BigDecimal("100"))
                .divide(limiteT, 1, RoundingMode.HALF_UP);

        return emisionComparacionMapper.toDto(
                anio,
                huellaT,
                limiteT,
                porcentaje,
                estado(porcentaje),
                null
        );
    }

    private String estado(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_SUPERADO) > 0) {
            return "superado";
        }
        if (porcentaje.compareTo(UMBRAL_CERCA) >= 0) {
            return "cerca";
        }
        return "dentro";
    }

    private void validarAnio(Integer anio) {
        int maximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > maximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Anio invalido.");
        }
    }

    private UUID empresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null || empresa.getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return empresa.getId();
    }
}
