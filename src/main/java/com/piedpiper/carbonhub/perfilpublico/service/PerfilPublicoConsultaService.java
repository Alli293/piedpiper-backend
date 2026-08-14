package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.BusquedaPerfilPublicoDTO;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PerfilPublicoConsultaService {

    // PLACEHOLDER: cuartiles parejos sobre el IMA (0-100), pendientes de que producto/diseño
    // defina los cortes reales por nivel. No mover sin actualizar este comentario.
    private static final BigDecimal UMBRAL_PLATINO = BigDecimal.valueOf(90);
    private static final BigDecimal UMBRAL_ORO = BigDecimal.valueOf(75);
    private static final BigDecimal UMBRAL_PLATA = BigDecimal.valueOf(60);
    private static final BigDecimal UMBRAL_BRONCE = BigDecimal.valueOf(40);

    private final EmpresaRepository empresaRepository;
    private final CertificacionRepository certificacionRepository;
    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final ImaSnapshotRepository imaSnapshotRepository;
    private final SlugResolverService slugResolver;

    public PerfilPublicoConsultaService(EmpresaRepository empresaRepository,
                                        CertificacionRepository certificacionRepository,
                                        InsigniaEmpresaRepository insigniaEmpresaRepository,
                                        ImaSnapshotRepository imaSnapshotRepository,
                                        SlugResolverService slugResolver) {
        this.empresaRepository = empresaRepository;
        this.certificacionRepository = certificacionRepository;
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.slugResolver = slugResolver;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponseDTO obtenerPorSlug(String slugOriginal) {
        Empresa empresa = slugResolver.resolver(slugOriginal);

        // 4. Contar certificaciones vigentes (fecha expiración futura o null)
        int certificacionesVigentes = contarCertificacionesVigentes(empresa);

        // 5. Contar insignias activas
        int insigniasActivas = insigniaEmpresaRepository
                .findByEmpresaIdOrderByFechaObtencionDesc(empresa.getId()).size();

        // 6. Ensamblar DTO
        NivelEcologicoInfo nivelInfo = resolverNivelEcologico(empresa);

        return new PerfilPublicoResponseDTO(
                empresa.getNombreEmpresa(),
                empresa.getLogoUrl(),
                empresa.getSectorIndustrial() != null
                        ? empresa.getSectorIndustrial().name()
                        : null,
                empresa.getPais(),
                nivelInfo.nivel(),
                nivelInfo.fechaActualizacion(),
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

    @Transactional(readOnly = true)
    public Page<BusquedaPerfilPublicoDTO> buscarPorNombre(String nombre, int page, int size) {
        int limitedSize = Math.min(size, 12);
        Pageable pageable = PageRequest.of(page, limitedSize);

        Page<Empresa> empresas;
        if (nombre == null || nombre.trim().length() < 3) {
            // Sin filtro: retorna las primeras empresas activas (catálogo)
            empresas = empresaRepository.findByEstado(EstadoEmpresa.ACTIVO, pageable);
        } else {
            empresas = empresaRepository.findByNombreEmpresaContainingIgnoreCaseAndEstado(
                    nombre.trim(), EstadoEmpresa.ACTIVO, pageable);
        }

        // Últimos snapshots de la página en una sola consulta: evita pegarle a
        // imaSnapshotRepository una vez por empresa (N+1 en un endpoint público).
        List<UUID> empresaIds = empresas.getContent().stream().map(Empresa::getId).toList();
        Map<UUID, ImaSnapshot> ultimosSnapshots = imaSnapshotRepository.findUltimosPorEmpresaIds(empresaIds).stream()
                .collect(Collectors.toMap(ImaSnapshot::getEmpresaId, Function.identity()));

        return empresas.map(empresa -> mapToBusquedaDTO(empresa, ultimosSnapshots.get(empresa.getId())));
    }

    private BusquedaPerfilPublicoDTO mapToBusquedaDTO(Empresa empresa, ImaSnapshot ultimoSnapshot) {
        return new BusquedaPerfilPublicoDTO(
                empresa.getNombreEmpresa(),
                empresa.getSlug(),
                empresa.getSectorIndustrial() != null ? empresa.getSectorIndustrial().name() : null,
                resolverNivelEcologico(empresa.getNivelEcologico(), ultimoSnapshot).nivel()
        );
    }

    /**
     * {@code Empresa.nivelEcologico} nunca lo asigna ningún flujo hoy (ver conversación de fix);
     * se conserva como override manual por compatibilidad, pero el caso real es el nivel derivado
     * del último IMA calculado para la empresa.
     */
    private NivelEcologicoInfo resolverNivelEcologico(Empresa empresa) {
        String nivelManual = empresa.getNivelEcologico();
        if (nivelManual != null && !nivelManual.isBlank()) {
            return new NivelEcologicoInfo(nivelManual, null);
        }

        ImaSnapshot ultimoSnapshot = imaSnapshotRepository
                .findFirstByEmpresaIdOrderByAnioDescMesDesc(empresa.getId())
                .orElse(null);
        return resolverNivelDesdeSnapshot(ultimoSnapshot);
    }

    private NivelEcologicoInfo resolverNivelEcologico(String nivelManual, ImaSnapshot ultimoSnapshot) {
        if (nivelManual != null && !nivelManual.isBlank()) {
            return new NivelEcologicoInfo(nivelManual, null);
        }
        return resolverNivelDesdeSnapshot(ultimoSnapshot);
    }

    private NivelEcologicoInfo resolverNivelDesdeSnapshot(ImaSnapshot ultimoSnapshot) {
        if (ultimoSnapshot == null) {
            return new NivelEcologicoInfo("Sin nivel", null);
        }

        String nivel = nivelDesdeIma(ultimoSnapshot.getIma());
        // "Sin nivel" no es un nivel alcanzado en una fecha: no le adjuntamos fechaActualizacion
        // para no dar a entender que hay una "vigencia" de un nivel que en realidad no existe.
        Instant fechaActualizacion = "Sin nivel".equals(nivel) ? null : ultimoSnapshot.getCalculatedAt();
        return new NivelEcologicoInfo(nivel, fechaActualizacion);
    }

    private String nivelDesdeIma(BigDecimal ima) {
        if (ima.compareTo(UMBRAL_PLATINO) >= 0) {
            return "Platino";
        }
        if (ima.compareTo(UMBRAL_ORO) >= 0) {
            return "Oro";
        }
        if (ima.compareTo(UMBRAL_PLATA) >= 0) {
            return "Plata";
        }
        if (ima.compareTo(UMBRAL_BRONCE) >= 0) {
            return "Bronce";
        }
        return "Sin nivel";
    }

    private record NivelEcologicoInfo(String nivel, Instant fechaActualizacion) {
    }
}
