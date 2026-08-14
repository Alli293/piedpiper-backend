package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorRecomendadoResponseDTO;
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
import java.util.UUID;

/**
 * Parte de base de datos de la recomendación de auditores (PP-57): resuelve la empresa del usuario,
 * consulta los candidatos, los ordena y los deja convertidos en datos planos.
 *
 * <p>Vive separada de {@link RecomendacionAuditoresService} a propósito: esa clase llama a Gemini, y
 * {@code docs/CONVENTIONS.md} §4.5 pide que un efecto externo no corra dentro de una transacción,
 * porque retiene una conexión del pool todo lo que tarde el modelo. Que sea un bean aparte y no solo
 * un método aparte importa: por el proxy de {@code @Transactional}, un método transaccional invocado
 * como {@code this.metodo(...)} desde la propia clase no abre transacción, y acá hace falta una
 * activa de principio a fin porque {@code especialidades} y {@code distribucionSectores} son
 * colecciones {@code LAZY} que se recorren al ordenar y al mapear.</p>
 *
 * <p>Por eso {@link #seleccionar} devuelve {@link Seleccion}: solo DTOs, strings y UUIDs. Ninguna
 * entidad ni colección perezosa cruza el límite de la transacción.</p>
 */
@Service
public class RecomendacionAuditoresConsultaService {

    static final int MAXIMO_RECOMENDACIONES = 5;
    private static final int TOP_SECTORES = 3;

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final UsuarioRepository usuarioRepository;

    public RecomendacionAuditoresConsultaService(
            PerfilAuditorRepository perfilAuditorRepository,
            UsuarioRepository usuarioRepository) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Seleccion seleccionar(RecomendarAuditoresRequestDTO filtros, UUID usuarioId) {
        Empresa empresa = empresaDe(usuarioId);
        EspecialidadAuditor especialidad = parsearEspecialidad(filtros.getEspecialidadBuscada());
        ProvinciaCR zona = parsearZona(filtros.getZonaGeografica());
        EspecialidadAuditor tipoAuditoria = parsearTipoAuditoria(filtros.getTipoAuditoria());
        boolean soloDisponibles = !Boolean.FALSE.equals(filtros.getSoloDisponibles());
        String sector = empresa.getSectorIndustrial().name();

        List<PerfilAuditor> candidatos = perfilAuditorRepository.buscarCandidatosRecomendacion(
                        Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO, especialidad, zona, soloDisponibles)
                .stream()
                .sorted(porIdoneidad(tipoAuditoria, sector))
                .limit(MAXIMO_RECOMENDACIONES)
                .toList();

        return new Seleccion(
                empresa.getId(),
                sector,
                tipoAuditoria.etiqueta(),
                zona.name(),
                candidatos.stream().map(this::aRecomendado).toList(),
                candidatos.stream().map(this::aCandidatoIa).toList());
    }

    /**
     * Orden pedido por la historia: (1) el auditor tiene la especialidad del tipo de auditoría,
     * (2) experiencia en el sector de la empresa, (3) calificación, (4) auditorías completadas.
     *
     * <p>Los cuatro criterios van de mejor a peor, y por eso los cuatro se invierten: por defecto
     * un comparador ordena de menor a mayor, y ahí quedarían primero los que no cumplen.</p>
     *
     * <p>Dos de los criterios admiten nulos y ninguno puede reventar el orden. La calificación es
     * nula mientras el auditor no tenga reseñas, y va al final con {@code nullsLast}. Las auditorías
     * completadas también son nulas: {@code MetricasReputacionAuditorService.dejarSinDatos()}
     * representa "todavía no completó ninguna" con {@code null} y no con cero, así que un auditor
     * recién certificado —el caso más común en un sistema nuevo— llega hasta acá con el campo en
     * nulo. Desempaquetarlo con {@code comparingInt} lanzaría {@code NullPointerException} y el
     * endpoint respondería 500, que es justo lo que el diseño de esta historia promete evitar.</p>
     */
    private Comparator<PerfilAuditor> porIdoneidad(EspecialidadAuditor tipoAuditoria, String sector) {
        Comparator<PerfilAuditor> porEspecialidad =
                Comparator.comparing(p -> cubreTipoAuditoria(p, tipoAuditoria));
        Comparator<PerfilAuditor> porSector =
                Comparator.comparingInt(p -> auditoriasEnSector(p, sector));
        Comparator<PerfilAuditor> porAuditorias =
                Comparator.comparingInt(RecomendacionAuditoresConsultaService::auditoriasCompletadasDe);

        return porEspecialidad.reversed()
                .thenComparing(porSector.reversed())
                .thenComparing(PerfilAuditor::getCalificacionPromedio,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(porAuditorias.reversed());
    }

    private boolean cubreTipoAuditoria(PerfilAuditor perfil, EspecialidadAuditor tipoAuditoria) {
        return perfil.getEspecialidades() != null && perfil.getEspecialidades().contains(tipoAuditoria);
    }

    /** Un auditor sin auditorías completadas se ordena como cero, que es lo que el nulo significa. */
    private static int auditoriasCompletadasDe(PerfilAuditor perfil) {
        return perfil.getAuditoriasCompletadas() == null ? 0 : perfil.getAuditoriasCompletadas();
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
                        ? null : perfil.getCalificacionPromedio().toPlainString(),
                top3Sectores(perfil),
                auditoriasCompletadasDe(perfil));
    }

    /** La tarjeta sale sin justificación: la agrega {@link RecomendacionAuditoresService} con la IA. */
    private AuditorRecomendadoResponseDTO aRecomendado(PerfilAuditor perfil) {
        return new AuditorRecomendadoResponseDTO(
                perfil.getAuditor().getId(),
                nombreDe(perfil.getAuditor()),
                perfil.getFotoPerfil(),
                etiquetasEspecialidades(perfil),
                perfil.getCalificacionPromedio(),
                perfil.isDisponible(),
                auditoriasCompletadasDe(perfil),
                null);
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

    /**
     * Lo que la transacción le entrega a {@link RecomendacionAuditoresService}: las tarjetas ya
     * armadas, la vista que viaja a la IA y el contexto de la búsqueda. Todo plano a propósito, para
     * que nada obligue a reabrir una sesión de Hibernate después.
     */
    public record Seleccion(
            UUID empresaId,
            String sector,
            String tipoAuditoria,
            String zona,
            List<AuditorRecomendadoResponseDTO> recomendados,
            List<CandidatoIa> paraIa) {
    }
}
