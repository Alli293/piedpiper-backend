package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.FiltrarSolicitudesAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.PaginaSolicitudesAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.DocumentoRespaldoRepository;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Listados de solicitudes de auditoria, uno por cada rol que las mira.
 *
 * <p>Son dos consultas distintas y no una con filtros porque la pertenencia se resuelve distinto:
 * la empresa ve las suyas por {@code empresa_id}, y el auditor ve las que tiene asignadas. Ninguno
 * de los dos recibe un parametro para elegir de quien listar; sale del usuario autenticado, asi que
 * no hay forma de pedir el listado de otro.</p>
 */
@Service
public class SolicitudAuditoriaListadoService {

    /** Fijado por la historia; no es configurable porque la pantalla lo asume para su paginador. */
    static final int TAMANIO_PAGINA = 25;

    /**
     * La pagina mas alta cuyo desplazamiento todavia cabe en un {@code int}, que es el limite que
     * impone Spring Data. Sale del tamanio de pagina en vez de ser un numero escrito a mano para
     * que siga siendo correcto si alguien cambia {@link #TAMANIO_PAGINA}.
     */
    static final int PAGINA_MAXIMA = Integer.MAX_VALUE / TAMANIO_PAGINA;

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRespaldoRepository documentoRespaldoRepository;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;

    public SolicitudAuditoriaListadoService(SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                            UsuarioRepository usuarioRepository,
                                            DocumentoRespaldoRepository documentoRespaldoRepository,
                                            SolicitudAuditoriaMapper solicitudAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoRespaldoRepository = documentoRespaldoRepository;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
    }

    /**
     * Listado paginado del usuario autenticado, con el dueño resuelto por su rol.
     *
     * <p>Ni la empresa ni el auditor pueden decir de quien quieren el listado: sale del token. El
     * administrador de plataforma si puede, porque su trabajo es justamente mirar el de otros, y
     * para el los ids son parametros explicitos.</p>
     */
    @Transactional(readOnly = true)
    public PaginaSolicitudesAuditoriaResponseDTO listar(FiltrarSolicitudesAuditoriaRequestDTO filtros,
                                                        UUID usuarioId) {
        Usuario usuario = usuarioDe(usuarioId);
        Set<EstadoSolicitudAuditoria> estados = estadosValidos(filtros.getFiltroEstado());
        Pageable pageable = paginaDe(filtros.getPagina());

        if (usuario.getRol() == Rol.ADMINISTRADOR_PLATAFORMA) {
            return comoAdministrador(filtros, estados, pageable);
        }
        if (usuario.getRol() == Rol.AUDITOR_CERTIFICADO) {
            // El id de empresa no aplica a este rol: enviarlo tambien es pedir un listado ajeno.
            exigirPropio(filtros.getIdAuditor(), usuario.getId());
            exigirPropio(filtros.getIdEmpresa(), null);
            return aPagina(paginarPorAuditor(usuario.getId(), estados, pageable));
        }

        UUID empresaId = empresaDe(usuario);
        exigirPropio(filtros.getIdEmpresa(), empresaId);
        exigirPropio(filtros.getIdAuditor(), null);
        return aPagina(paginarPorEmpresa(empresaId, estados, pageable));
    }

    /**
     * Pedir explicitamente el listado de otro se responde con 403 y no ignorando el parametro: en
     * silencio, quien lo pide se queda creyendo que esta viendo los datos del otro cuando en
     * realidad ve los propios, y eso es peor que un error claro.
     */
    private static void exigirPropio(UUID solicitado, UUID propio) {
        if (solicitado != null && !solicitado.equals(propio)) {
            throw ApiException.listadoAuditoriasAjeno();
        }
    }

    /**
     * Sin id explicito el administrador no tiene un listado propio que mirar: no es empresa ni
     * auditor. Devolverle el listado de "su empresa" seria mentirle con una lista vacia.
     */
    private PaginaSolicitudesAuditoriaResponseDTO comoAdministrador(
            FiltrarSolicitudesAuditoriaRequestDTO filtros,
            Set<EstadoSolicitudAuditoria> estados,
            Pageable pageable) {
        if (filtros.getIdEmpresa() != null) {
            return aPagina(paginarPorEmpresa(filtros.getIdEmpresa(), estados, pageable));
        }
        if (filtros.getIdAuditor() != null) {
            return aPagina(paginarPorAuditor(filtros.getIdAuditor(), estados, pageable));
        }
        throw ApiException.listadoAuditoriasSinDestinatario();
    }

    private Page<SolicitudAuditoria> paginarPorEmpresa(UUID empresaId,
                                                       Set<EstadoSolicitudAuditoria> estados,
                                                       Pageable pageable) {
        return solicitudAuditoriaRepository.paginarPorEmpresa(
                empresaId, estados.isEmpty(), estadosParaConsulta(estados), pageable);
    }

    private Page<SolicitudAuditoria> paginarPorAuditor(UUID auditorId,
                                                       Set<EstadoSolicitudAuditoria> estados,
                                                       Pageable pageable) {
        return solicitudAuditoriaRepository.paginarPorAuditor(
                auditorId,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                estados.isEmpty(),
                estadosParaConsulta(estados),
                pageable);
    }

    /**
     * Un valor de estado que no existe se ignora y el listado sale completo, en vez de fallar: es
     * un filtro de una pantalla, no un dato que el usuario tipea, y un enlace viejo con un estado
     * renombrado no deberia romperle la pagina a nadie.
     */
    private Set<EstadoSolicitudAuditoria> estadosValidos(List<String> filtro) {
        if (filtro == null) {
            return Set.of();
        }
        return filtro.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(SolicitudAuditoriaListadoService::estadoDe)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoSolicitudAuditoria.class)));
    }

    private static EstadoSolicitudAuditoria estadoDe(String valor) {
        return Arrays.stream(EstadoSolicitudAuditoria.values())
                .filter(estado -> estado.name().equalsIgnoreCase(valor))
                .findFirst()
                .orElse(null);
    }

    /**
     * La consulta nunca recibe una coleccion vacia: cuando no hay filtro la condicion se apaga con
     * la bandera y esta lista no se llega a evaluar, pero {@code in ()} sin elementos genera SQL
     * invalido aunque la rama este muerta.
     */
    private static Collection<EstadoSolicitudAuditoria> estadosParaConsulta(
            Set<EstadoSolicitudAuditoria> estados) {
        return estados.isEmpty() ? EnumSet.allOf(EstadoSolicitudAuditoria.class) : estados;
    }

    /**
     * La pagina llega en base 1 y Spring Data cuenta desde 0. Un valor ausente o menor que 1 cae en
     * la primera pagina sin error, como pide la historia.
     *
     * <p>El tope superior existe por la misma razon que el inferior, y ademas porque sin el la
     * peticion revienta: Spring Data calcula el desplazamiento como {@code (pagina - 1) * tamanio}
     * sobre un {@code int}, asi que un {@code ?pagina=999999999} lo desborda y sale un 500. Pasado
     * el tope se responde una pagina vacia, igual que cualquier otra pagina fuera de rango.</p>
     */
    private static Pageable paginaDe(Integer pagina) {
        int solicitada = pagina == null ? 1 : Math.clamp(pagina, 1, PAGINA_MAXIMA);
        return PageRequest.of(solicitada - 1, TAMANIO_PAGINA,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    private PaginaSolicitudesAuditoriaResponseDTO aPagina(Page<SolicitudAuditoria> pagina) {
        return new PaginaSolicitudesAuditoriaResponseDTO(
                aResumenes(pagina.getContent()),
                pagina.getTotalElements(),
                pagina.getNumber() + 1,
                pagina.getTotalPages(),
                TAMANIO_PAGINA);
    }

    private UUID empresaDe(Usuario usuario) {
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
    }

    /**
     * El conteo de adjuntos sale de una consulta agregada y no de {@code solicitud.getDocumentos()}:
     * esa coleccion es perezosa, asi que contarla por fila dispararia una consulta extra por
     * solicitud que ademas trae el contenido binario completo de cada PDF.
     */
    private List<SolicitudAuditoriaResumenResponseDTO> aResumenes(List<SolicitudAuditoria> solicitudes) {
        List<SolicitudAuditoriaResumenResponseDTO> resumenes =
                solicitudAuditoriaMapper.toResumenDtos(solicitudes);
        if (resumenes.isEmpty()) {
            return resumenes;
        }

        Map<UUID, Integer> conteos = new HashMap<>();
        documentoRespaldoRepository
                .contarPorSolicitud(solicitudes.stream().map(SolicitudAuditoria::getId).toList())
                .forEach(fila -> conteos.put((UUID) fila[0], ((Number) fila[1]).intValue()));

        resumenes.forEach(resumen ->
                resumen.setCantidadDocumentos(conteos.getOrDefault(resumen.getId(), 0)));
        return resumenes;
    }

    private Usuario usuarioDe(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
    }
}
