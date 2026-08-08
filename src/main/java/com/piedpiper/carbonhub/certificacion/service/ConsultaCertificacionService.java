package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsultaCertificacionService {

    private final CertificacionRepository certificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoTiposCertificacion catalogoTiposCertificacion;
    private final CertificacionMapper certificacionMapper;
    private final GeneradorCredencialOpenBadges generadorCredencialOpenBadges;

    public ConsultaCertificacionService(CertificacionRepository certificacionRepository,
                                        UsuarioRepository usuarioRepository,
                                        CatalogoTiposCertificacion catalogoTiposCertificacion,
                                        CertificacionMapper certificacionMapper,
                                        GeneradorCredencialOpenBadges generadorCredencialOpenBadges) {
        this.certificacionRepository = certificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoTiposCertificacion = catalogoTiposCertificacion;
        this.certificacionMapper = certificacionMapper;
        this.generadorCredencialOpenBadges = generadorCredencialOpenBadges;
    }

    @Transactional(readOnly = true)
    public List<CertificacionResumenResponseDTO> listar(UUID usuarioId) {
        UUID empresaId = empresaDelUsuario(usuarioId);
        return certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(empresaId).stream()
                .map(this::aResumenDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificacionResponseDTO detalle(UUID usuarioId, UUID certificacionId) {
        UUID empresaId = empresaDelUsuario(usuarioId);
        // Se filtra por empresa en la consulta para no revelar la existencia de
        // certificaciones de otras empresas con un 403 en vez de un 404.
        return certificacionRepository.findByIdAndEmpresaId(certificacionId, empresaId)
                .map(this::aDto)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "La certificacion no existe."));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> descargarJsonLd(UUID usuarioId, UUID certificacionId) {
        UUID empresaId = empresaDelUsuario(usuarioId);
        // Igual que detalle(): se filtra por empresa en la consulta y se
        // devuelve 404 (no 403) para una certificacion de otra empresa, para no
        // revelar con un 403 que ese id existe pero es ajeno.
        Certificacion certificacion = certificacionRepository.findByIdAndEmpresaId(certificacionId, empresaId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La certificacion no existe."));
        return generadorCredencialOpenBadges.decodificar(certificacion.getCredencialJwt());
    }

    /**
     * Publico, sin resolucion de usuario ni filtro de empresa a proposito: el
     * punto de esta URL es que cualquiera con el enlace pueda verificarla (ver
     * {@code CertificacionEmisorController}).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verificarPublica(UUID certificacionId) {
        Certificacion certificacion = certificacionRepository.findById(certificacionId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La certificacion no existe."));
        return generadorCredencialOpenBadges.decodificar(certificacion.getCredencialJwt());
    }

    /**
     * VC-JWT crudo (sin decodificar) de una certificacion, mismo criterio de
     * acceso que {@link #verificarPublica}. Es el artefacto que esperan los
     * validadores de OpenBadges 3.0: el documento de {@code verificarPublica}
     * ya no trae la firma, asi que no sirve para validar por si solo.
     */
    @Transactional(readOnly = true)
    public String verificacionJwt(UUID certificacionId) {
        Certificacion certificacion = certificacionRepository.findById(certificacionId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La certificacion no existe."));
        return certificacion.getCredencialJwt();
    }

    /**
     * Certificaciones activas y vigentes (no vencidas) de una empresa, para el
     * perfil publico. Sin resolucion de usuario ni auth a proposito: lo llama
     * {@code perfilpublico}, que ya resolvio el {@code empresaId} a partir de
     * un slug sin sesion. Vive aqui y no en {@code perfilpublico} para que ese
     * dominio no dependa directamente de {@link CertificacionRepository} ni
     * de {@link CertificacionMapper}.
     *
     * <p>No hay un {@code estado} de "vencida": vencer no se persiste, se
     * calcula comparando {@code fechaVencimiento} contra hoy en cada consulta,
     * asi que no hace falta un job que mantenga ese estado sincronizado.
     */
    @Transactional(readOnly = true)
    public List<CertificacionPublicaResponseDTO> listarActivasPublicasPorEmpresa(UUID empresaId) {
        return certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        empresaId, EstadoCertificacion.ACTIVA, LocalDate.now())
                .stream()
                .map(this::aPublicaDto)
                .toList();
    }

    private UUID empresaDelUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "Solo el administrador de la empresa puede consultar certificaciones."));
        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
    }

    private CertificacionResponseDTO aDto(Certificacion certificacion) {
        CertificacionResponseDTO dto = certificacionMapper.toDto(certificacion);
        dto.setRecienEmitida(false);
        dto.setVigente(esVigente(certificacion));
        dto.setUrlVerificacion(generadorCredencialOpenBadges.urlVerificacion(certificacion.getId()));
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }

    private CertificacionResumenResponseDTO aResumenDto(Certificacion certificacion) {
        CertificacionResumenResponseDTO dto = certificacionMapper.toResumenDto(certificacion);
        dto.setVigente(esVigente(certificacion));
        dto.setUrlVerificacion(generadorCredencialOpenBadges.urlVerificacion(certificacion.getId()));
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        return dto;
    }

    /**
     * {@code fechaVencimiento} estrictamente posterior a hoy: misma semantica
     * que {@code exp} en el VC-JWT (vence a medianoche UTC de ese dia).
     */
    private static boolean esVigente(Certificacion certificacion) {
        return certificacion.getFechaVencimiento().isAfter(LocalDate.now());
    }

    private CertificacionPublicaResponseDTO aPublicaDto(Certificacion certificacion) {
        CertificacionPublicaResponseDTO dto = certificacionMapper.toPublicaDto(certificacion);
        catalogoTiposCertificacion.buscar(certificacion.getTipo())
                .ifPresent(definicion -> dto.setNombreCertificacion(definicion.nombre()));
        dto.setNombreAuditor(certificacion.getAuditor().nombreCompleto());
        return dto;
    }
}
