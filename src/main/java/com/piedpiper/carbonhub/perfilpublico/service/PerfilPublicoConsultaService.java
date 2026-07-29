package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
public class PerfilPublicoConsultaService {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9-]{1,120}$");

    private final EmpresaRepository empresaRepository;
    private final CertificacionRepository certificacionRepository;

    public PerfilPublicoConsultaService(EmpresaRepository empresaRepository,
                                        CertificacionRepository certificacionRepository) {
        this.empresaRepository = empresaRepository;
        this.certificacionRepository = certificacionRepository;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponseDTO obtenerPorSlug(String slugOriginal) {
        // 1. Normalizar slug a minúsculas
        String slug = slugOriginal == null ? "" : slugOriginal.toLowerCase();

        // 2. Validar formato con regex
        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new PerfilNoEncontradoException(
                    "El perfil que buscas no existe o ya no está disponible.");
        }

        // 3. Buscar empresa por slug
        Empresa empresa = empresaRepository.findBySlug(slug)
                .orElseThrow(() -> new PerfilNoEncontradoException(
                        "El perfil que buscas no existe o ya no está disponible."));

        // 4. Verificar estado ACTIVO
        if (empresa.getEstado() != EstadoEmpresa.ACTIVO) {
            throw new PerfilNoEncontradoException(
                    "Este perfil no está disponible en este momento.");
        }

        // 5. Contar certificaciones vigentes (fecha expiración futura o null)
        int certificacionesVigentes = contarCertificacionesVigentes(empresa);

        // 6. Contar insignias activas — retorna 0 (PP-60 aún no implementado)
        int insigniasActivas = 0;

        // 7. Ensamblar DTO
        String nivelEcologico = resolverNivelEcologico(empresa.getNivelEcologico());

        return new PerfilPublicoResponseDTO(
                empresa.getNombreEmpresa(),
                empresa.getLogoUrl(),
                empresa.getSectorIndustrial() != null
                        ? empresa.getSectorIndustrial().name()
                        : null,
                empresa.getPais(),
                nivelEcologico,
                null, // fechaActualizacionNivel - no existe campo en entidad aún
                certificacionesVigentes,
                insigniasActivas
        );
    }

    private int contarCertificacionesVigentes(Empresa empresa) {
        try {
            return certificacionRepository
                    .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                            empresa.getId(), EstadoCertificacion.ACTIVA, LocalDate.now())
                    .size();
        } catch (Exception e) {
            return 0;
        }
    }

    private String resolverNivelEcologico(String nivelEcologico) {
        if (nivelEcologico == null || nivelEcologico.isBlank()) {
            return "Sin nivel";
        }
        return nivelEcologico;
    }
}
