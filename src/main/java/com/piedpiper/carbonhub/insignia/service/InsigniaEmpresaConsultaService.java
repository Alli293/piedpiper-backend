package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoVerificacion;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCodigoVerificacionService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.mappers.InsigniaEmpresaMapper;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.perfilpublico.service.SlugResolverService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InsigniaEmpresaConsultaService {

    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final CatalogoInsigniaRepository catalogoInsigniaRepository;
    private final EmpresaRepository empresaRepository;
    private final EmisionEmpresaService emisionEmpresaService;
    private final InsigniaEmpresaMapper insigniaEmpresaMapper;
    private final InsigniaEmpresaOpenBadgesService insigniaEmpresaOpenBadgesService;
    private final SlugResolverService slugResolver;

    public InsigniaEmpresaConsultaService(InsigniaEmpresaRepository insigniaEmpresaRepository,
                                          CatalogoInsigniaRepository catalogoInsigniaRepository,
                                          EmpresaRepository empresaRepository,
                                          EmisionEmpresaService emisionEmpresaService,
                                          InsigniaEmpresaMapper insigniaEmpresaMapper,
                                          InsigniaEmpresaOpenBadgesService
                                                  insigniaEmpresaOpenBadgesService,
                                          SlugResolverService slugResolver) {
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
        this.empresaRepository = empresaRepository;
        this.emisionEmpresaService = emisionEmpresaService;
        this.insigniaEmpresaMapper = insigniaEmpresaMapper;
        this.insigniaEmpresaOpenBadgesService = insigniaEmpresaOpenBadgesService;
        this.slugResolver = slugResolver;
    }

    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarParaEmpresaAutenticada(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        return listarPorEmpresa(empresaId);
    }

    /**
     * Lista todas las insignias otorgadas a la empresa del slug. "Otorgada"
     * es sinonimo de "activa" en este dominio: a diferencia de las
     * certificaciones, las insignias empresariales (PP-60) son logros
     * permanentes una vez otorgados — no existe vencimiento ni revocacion en
     * el modelo actual (ver {@link InsigniaEmpresa}). Si ese concepto se
     * agrega en el futuro, este metodo es el punto para filtrar por estado.
     */
    @Transactional(readOnly = true)
    public List<InsigniaEmpresaResponseDTO> listarPorSlug(String slug) {
        Empresa empresa = slugResolver.resolver(slug);
        return listarPorEmpresa(empresa.getId());
    }

    /**
     * Verificacion publica por {@code codigoVerificacion}, contraparte de
     * {@code ConsultaCertificacionService#verificarPorCodigo} para insignias
     * (ver {@code VerificacionCredencialService}, que intenta primero
     * certificacion y cae aqui si no encuentra). Mismo criterio de
     * privacidad: un codigo mal formado y uno bien formado pero inexistente
     * devuelven el mismo 404.
     *
     * <p>{@code estado} siempre es {@code valida_vigente}: las insignias
     * empresariales no vencen ni se revocan en el modelo actual (ver
     * {@link #listarPorSlug}).
     */
    @Transactional(readOnly = true)
    public VerificacionCredencialDTO verificarPorCodigo(String codigo) {
        String codigoNormalizado = codigo == null ? null : codigo.toUpperCase(Locale.ROOT);
        if (!GeneradorCodigoVerificacionService.formatoValido(codigoNormalizado)) {
            throw ApiException.recursoNoEncontrado("Credencial no encontrada.");
        }

        InsigniaEmpresa insigniaEmpresa = insigniaEmpresaRepository
                .findByCodigoVerificacion(codigoNormalizado)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("Credencial no encontrada."));

        if (insigniaEmpresa.getEmpresa().getEstado() != EstadoEmpresa.ACTIVO) {
            throw ApiException.recursoNoEncontrado("Credencial no encontrada.");
        }

        CatalogoInsignia catalogo = catalogoInsigniaRepository
                .findByIdInsigniaAndNivelInsigniaAndActivaTrue(
                        insigniaEmpresa.getIdInsignia(), insigniaEmpresa.getNivelInsignia())
                .orElse(null);

        VerificacionCredencialDTO dto = new VerificacionCredencialDTO();
        dto.setEstado(EstadoVerificacion.VALIDA_VIGENTE.getCodigo());
        dto.setCategoria("INSIGNIA");
        dto.setNombreCertificacion(catalogo != null ? catalogo.getNombre() : "Insignia");
        dto.setNivelInsignia(insigniaEmpresa.getNivelInsignia());
        dto.setEmpresa(insigniaEmpresa.getEmpresa().getNombreEmpresa());
        dto.setEntidadCertificadora(insigniaEmpresaOpenBadgesService.emisorNombre());
        dto.setFechaEmision(insigniaEmpresa.getFechaObtencion());
        dto.setFechaConsulta(Instant.now());
        return dto;
    }

    private List<InsigniaEmpresaResponseDTO> listarPorEmpresa(UUID empresaId) {
        Map<ClaveCatalogoInsignia, CatalogoInsignia> catalogoPorClave =
                catalogoInsigniaRepository.findByActivaTrue()
                        .stream()
                        .collect(Collectors.toMap(
                                catalogo -> new ClaveCatalogoInsignia(
                                        catalogo.getIdInsignia(), catalogo.getNivelInsignia()),
                                Function.identity(),
                                (actual, duplicada) -> actual));
        return insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(empresaId)
                .stream()
                .map(insignia -> aDto(insignia, catalogoPorClave))
                .toList();
    }

    private InsigniaEmpresaResponseDTO aDto(InsigniaEmpresa insigniaEmpresa,
                                            Map<ClaveCatalogoInsignia, CatalogoInsignia>
                                                    catalogoPorClave) {
        InsigniaEmpresaResponseDTO dto = insigniaEmpresaMapper.toDto(insigniaEmpresa);
        CatalogoInsignia catalogo = catalogoPorClave.get(new ClaveCatalogoInsignia(
                insigniaEmpresa.getIdInsignia(), insigniaEmpresa.getNivelInsignia()));
        if (catalogo != null) {
            dto.setNombre(catalogo.getNombre());
            dto.setDescripcion(catalogo.getDescripcion());
            dto.setCriteriosObtencion(insigniaEmpresaOpenBadgesService.criterios(catalogo));
            dto.setEmisor(insigniaEmpresaOpenBadgesService.emisorNombre());
            dto.setReceptor(insigniaEmpresa.getEmpresa().getNombreEmpresa());
            dto.setUrlVerificacionPublica(insigniaEmpresaOpenBadgesService
                    .urlVerificacionPublica(insigniaEmpresa.getId()));
            dto.setUrlVerificacionJwt(insigniaEmpresaOpenBadgesService
                    .urlVerificacionJwt(insigniaEmpresa.getId()));
            dto.setUrlLinkedIn(insigniaEmpresaOpenBadgesService.urlLinkedIn(
                    insigniaEmpresa, catalogo));
        }
        return dto;
    }

    private record ClaveCatalogoInsignia(Long idInsignia, String nivelInsignia) {
    }
}
