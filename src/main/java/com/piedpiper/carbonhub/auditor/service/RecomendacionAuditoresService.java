package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorRecomendadoResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendacionAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.RecomendarAuditoresRequestDTO;
import com.piedpiper.carbonhub.auditor.models.entities.DistribucionSectorAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresIaService.CandidatoIa;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Recomendación de auditores (PP-57).
 *
 * <p>La selección y el orden son <b>deterministas y viven aquí</b>; la IA solo redacta la
 * justificación de una decisión ya tomada. Esa separación es intencional: si el orden dependiera del
 * modelo, la misma consulta daría resultados distintos entre llamadas y no habría forma de probarla.</p>
 *
 * <p>El sector y la empresa salen del usuario autenticado, nunca del cuerpo de la petición, así que
 * nadie puede pedir recomendaciones haciéndose pasar por otra empresa.</p>
 */
@Service
public class RecomendacionAuditoresService {

    static final int MAXIMO_RECOMENDACIONES = 5;
    private static final int TOP_SECTORES = 3;

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecomendacionAuditoresIaService recomendacionAuditoresIaService;

    public RecomendacionAuditoresService(
            PerfilAuditorRepository perfilAuditorRepository,
            UsuarioRepository usuarioRepository,
            RecomendacionAuditoresIaService recomendacionAuditoresIaService) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.usuarioRepository = usuarioRepository;
        this.recomendacionAuditoresIaService = recomendacionAuditoresIaService;
    }

    @Transactional(readOnly = true)
    public RecomendacionAuditoresResponseDTO recomendar(RecomendarAuditoresRequestDTO filtros,
                                                        UUID usuarioId) {
        Empresa empresa = empresaDe(usuarioId);
        EspecialidadAuditor especialidad = parsearEspecialidad(filtros.getEspecialidadBuscada());
        ProvinciaCR zona = parsearZona(filtros.getZonaGeografica());
        EspecialidadAuditor tipoAuditoria = parsearTipoAuditoria(filtros.getTipoAuditoria());
        boolean soloDisponibles = !Boolean.FALSE.equals(filtros.getSoloDisponibles());

        List<PerfilAuditor> candidatos = perfilAuditorRepository.buscarCandidatosRecomendacion(
                        Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO, especialidad, zona, soloDisponibles)
                .stream()
                .sorted(porIdoneidad(tipoAuditoria, empresa.getSectorIndustrial().name()))
                .limit(MAXIMO_RECOMENDACIONES)
                .toList();

        // Sin candidatos no se llama a la IA: no hay a quién justificar. La bandera queda en true
        // porque el aviso de "IA no disponible" solo aplica cuando la IA falló, no cuando no se usó.
        if (candidatos.isEmpty()) {
            return new RecomendacionAuditoresResponseDTO(List.of(), true);
        }

        Optional<Map<UUID, String>> justificaciones = recomendacionAuditoresIaService.generarJustificaciones(
                empresa.getId(),
                empresa.getSectorIndustrial().name(),
                tipoAuditoria.etiqueta(),
                zona.name(),
                candidatos.stream().map(this::aCandidatoIa).toList());

        Map<UUID, String> porAuditor = justificaciones.orElse(Map.of());
        List<AuditorRecomendadoResponseDTO> recomendaciones = candidatos.stream()
                .map(perfil -> aRecomendado(perfil, porAuditor.get(perfil.getAuditor().getId())))
                .toList();

        return new RecomendacionAuditoresResponseDTO(recomendaciones, justificaciones.isPresent());
    }

    /**
     * Orden pedido por la historia: (1) el auditor tiene la especialidad del tipo de auditoría,
     * (2) experiencia en el sector de la empresa, (3) calificación, (4) auditorías completadas.
     *
     * <p>Los cuatro criterios van de mejor a peor, y por eso los cuatro se invierten: por defecto
     * un comparador ordena de menor a mayor, y ahí quedarían primero los que no cumplen. La
     * calificación se compara aparte porque es la única que admite nulos (un auditor sin reseñas),
     * y esos van al final en vez de reventar.</p>
     */
    private Comparator<PerfilAuditor> porIdoneidad(EspecialidadAuditor tipoAuditoria, String sector) {
        Comparator<PerfilAuditor> porEspecialidad =
                Comparator.comparing(p -> cubreTipoAuditoria(p, tipoAuditoria));
        Comparator<PerfilAuditor> porSector =
                Comparator.comparingInt(p -> auditoriasEnSector(p, sector));
        Comparator<PerfilAuditor> porAuditorias =
                Comparator.comparingInt(PerfilAuditor::getAuditoriasCompletadas);

        return porEspecialidad.reversed()
                .thenComparing(porSector.reversed())
                .thenComparing(PerfilAuditor::getCalificacionPromedio,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(porAuditorias.reversed());
    }

    private boolean cubreTipoAuditoria(PerfilAuditor perfil, EspecialidadAuditor tipoAuditoria) {
        return perfil.getEspecialidades() != null && perfil.getEspecialidades().contains(tipoAuditoria);
    }

    /** Cuántas auditorías hizo el candidato en el sector de la empresa, 0 si nunca auditó ahí. */
    private int auditoriasEnSector(PerfilAuditor perfil, String sector) {
        if (perfil.getDistribucionSectores() == null) {
            return 0;
        }
        return perfil.getDistribucionSectores().stream()
                .filter(d -> sector.equalsIgnoreCase(d.getSector()))
                .mapToInt(DistribucionSectorAuditor::getCantidad)
                .findFirst()
                .orElse(0);
    }

    private CandidatoIa aCandidatoIa(PerfilAuditor perfil) {
        return new CandidatoIa(
                perfil.getAuditor().getId(),
                nombreDe(perfil.getAuditor()),
                etiquetasEspecialidades(perfil),
                perfil.getCalificacionPromedio() == null
                        ? "sin calificaciones" : perfil.getCalificacionPromedio().toPlainString(),
                top3Sectores(perfil),
                perfil.getAuditoriasCompletadas());
    }

    private AuditorRecomendadoResponseDTO aRecomendado(PerfilAuditor perfil, String justificacion) {
        return new AuditorRecomendadoResponseDTO(
                perfil.getAuditor().getId(),
                nombreDe(perfil.getAuditor()),
                perfil.getFotoPerfil(),
                etiquetasEspecialidades(perfil),
                perfil.getCalificacionPromedio(),
                perfil.isDisponible(),
                perfil.getAuditoriasCompletadas(),
                justificacion);
    }

    private List<String> etiquetasEspecialidades(PerfilAuditor perfil) {
        if (perfil.getEspecialidades() == null) {
            return List.of();
        }
        return perfil.getEspecialidades().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(EspecialidadAuditor::etiqueta)
                .toList();
    }

    private List<String> top3Sectores(PerfilAuditor perfil) {
        if (perfil.getDistribucionSectores() == null) {
            return List.of();
        }
        return perfil.getDistribucionSectores().stream()
                .sorted(Comparator.comparingInt(DistribucionSectorAuditor::getCantidad).reversed())
                .limit(TOP_SECTORES)
                .map(DistribucionSectorAuditor::getSector)
                .toList();
    }

    private String nombreDe(Usuario auditor) {
        String apellidos = auditor.getApellidos() == null ? "" : auditor.getApellidos();
        return (auditor.getNombre() + " " + apellidos).trim();
    }

    private Empresa empresaDe(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getSectorIndustrial() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa();
    }

    private EspecialidadAuditor parsearEspecialidad(String valor) {
        return EspecialidadAuditor.desde(valor)
                .orElseThrow(() -> ApiException.especialidadAuditorInvalida(valor));
    }

    /**
     * El tipo de auditoría comparte catálogo con las especialidades: la historia pide ordenar por
     * "coincidencia de especialidad con el tipoAuditoria", lo que solo tiene sentido si ambos hablan
     * del mismo vocabulario. Un catálogo aparte obligaría a mantener una tabla de equivalencias.
     */
    private EspecialidadAuditor parsearTipoAuditoria(String valor) {
        return EspecialidadAuditor.desde(valor)
                .orElseThrow(() -> ApiException.tipoAuditoriaRecomendacionInvalido(valor));
    }

    private ProvinciaCR parsearZona(String valor) {
        return ProvinciaCR.desde(valor)
                .orElseThrow(() -> ApiException.zonaAuditorInvalida(valor));
    }
}
