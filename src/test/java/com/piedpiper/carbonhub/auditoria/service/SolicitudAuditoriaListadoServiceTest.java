package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.FiltrarSolicitudesAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.PaginaSolicitudesAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.DocumentoRespaldoRepository;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudAuditoriaListadoServiceTest {

    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID ADMIN_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID PLATAFORMA_ID = UUID.fromString("a11ce000-2222-3333-4444-555566667777");
    private static final UUID OTRA_EMPRESA_ID = UUID.fromString("bbbbbbbb-2222-3333-4444-555566667777");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DocumentoRespaldoRepository documentoRespaldoRepository;

    private SolicitudAuditoriaListadoService service;

    @BeforeEach
    void configurar() {
        service = new SolicitudAuditoriaListadoService(
                solicitudAuditoriaRepository,
                usuarioRepository,
                documentoRespaldoRepository,
                new SolicitudAuditoriaMapperImpl());

        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(ADMIN_ID)
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .build()));
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(AUDITOR_ID)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build()));
        when(usuarioRepository.findById(PLATAFORMA_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(PLATAFORMA_ID)
                .rol(Rol.ADMINISTRADOR_PLATAFORMA)
                .build()));
        when(solicitudAuditoriaRepository.paginarPorEmpresa(any(), anyBoolean(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(conAuditor(), sinAuditor())));
        when(solicitudAuditoriaRepository.paginarPorAuditor(any(), any(), anyBoolean(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(conAuditor())));
        when(documentoRespaldoRepository.contarPorSolicitud(anyCollection()))
                .thenAnswer(invocacion -> {
                    Collection<?> ids = invocacion.getArgument(0);
                    // Solo la primera solicitud del listado tiene un adjunto.
                    Object primero = ids.iterator().next();
                    // List.<Object[]>of y no List.of: con un solo Object[] el varargs lo desarma
                    // y devolveria una lista de dos elementos en vez de una con un arreglo.
                    return List.<Object[]>of(new Object[] {primero, 1L});
                });
    }

    @Test
    void laEmpresaVeSusSolicitudesConElAuditorYElConteoDeAdjuntos() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = listar(ADMIN_ID).getContenido();

        assertThat(listado).hasSize(2);
        assertThat(listado.get(0).getNombreAuditor()).isEqualTo("Ana Auditora");
        assertThat(listado.get(0).getNombreEmpresa()).isEqualTo("Acme S.A.");
        assertThat(listado.get(0).getEstadoDescripcion()).isEqualTo("En revisión");
        assertThat(listado.get(0).getCantidadDocumentos()).isEqualTo(1);
    }

    /** Una solicitud sin auditor asignado es normal en el listado y no debe romper el mapeo. */
    @Test
    void unaSolicitudSinAuditorSeListaConElNombreEnNulo() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = listar(ADMIN_ID).getContenido();

        assertThat(listado.get(1).getNombreAuditor()).isNull();
        assertThat(listado.get(1).getIdAuditor()).isNull();
        assertThat(listado.get(1).getCantidadDocumentos()).isZero();
    }

    /**
     * El listado sale del usuario autenticado y no de un parametro: por eso el servicio consulta
     * por la empresa del usuario y nunca por una recibida desde afuera.
     */
    @Test
    void elListadoDeEmpresaConsultaPorLaEmpresaDelUsuarioAutenticado() {
        listar(ADMIN_ID).getContenido();

        verify(solicitudAuditoriaRepository).paginarPorEmpresa(eq(EMPRESA_ID), anyBoolean(), anyCollection(), any());
    }

    @Test
    void unUsuarioSinEmpresaConfiguradaRecibe422() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(
                Usuario.builder().id(ADMIN_ID).rol(Rol.ADMINISTRADOR_EMPRESA).build()));

        assertThatThrownBy(() -> listar(ADMIN_ID).getContenido())
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void elAuditorVeSuListadoResueltoPorSuRol() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = listar(AUDITOR_ID).getContenido();

        assertThat(listado).hasSize(1);
        assertThat(listado.get(0).getIdAuditor()).isEqualTo(AUDITOR_ID);
        verify(solicitudAuditoriaRepository).paginarPorAuditor(eq(AUDITOR_ID), any(), anyBoolean(), anyCollection(), any());
    }

    /**
     * El conteo tiene que salir de la consulta agregada y no de la coleccion de la entidad: contar
     * sobre la coleccion perezosa dispara una consulta por fila que trae el binario de cada PDF.
     */
    @Test
    void elConteoDeAdjuntosSaleDeUnaConsultaAgregadaYNoDeLaColeccion() {
        List<SolicitudAuditoriaResumenResponseDTO> listado = listar(ADMIN_ID).getContenido();

        verify(documentoRespaldoRepository).contarPorSolicitud(anyCollection());
        assertThat(listado.get(0).getCantidadDocumentos()).isEqualTo(1);
        assertThat(listado.get(1).getCantidadDocumentos()).isZero();
    }

    @Test
    void elResumenNoExponeElContenidoDeLosDocumentos() {
        assertThat(SolicitudAuditoriaResumenResponseDTO.class.getDeclaredFields())
                .noneMatch(campo -> campo.getType() == byte[].class || campo.getType() == List.class);
    }

    @Test
    void laPrimeraPaginaTrae25RegistrosOrdenadosPorFechaDeCreacionDescendente() {
        listar(ADMIN_ID);

        Pageable pageable = capturarPageableDeEmpresa();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("fechaCreacion"))
                .isNotNull()
                .satisfies(orden -> assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC));
    }

    /** La pagina llega en base 1 desde el cliente y Spring Data cuenta desde 0. */
    @Test
    void laPaginaDosSeTraduceALaPaginaUnoDeSpringData() {
        listar(ADMIN_ID, filtros(null, 2));

        assertThat(capturarPageableDeEmpresa().getPageNumber()).isEqualTo(1);
    }

    /**
     * La historia lo pide explicitamente: una pagina invalida no es un error, es la primera. Un
     * enlace mal copiado no deberia devolverle un 400 a nadie.
     */
    @Test
    void unaPaginaInvalidaCaeEnLaPrimeraSinError() {
        for (Integer invalida : new Integer[] {null, 0, -3}) {
            listar(ADMIN_ID, filtros(null, invalida));
        }

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudAuditoriaRepository, org.mockito.Mockito.atLeastOnce())
                .paginarPorEmpresa(any(), anyBoolean(), anyCollection(), captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(
                pageable -> assertThat(pageable.getPageNumber()).isZero());
    }

    /**
     * La pagina 2 de 60 resultados, que es una pagina intermedia: {@code PageImpl} recalcula el
     * total cuando la pagina pedida es la ultima, asi que un escenario de borde probaria mas sobre
     * Spring Data que sobre este servicio.
     */
    @Test
    void laRespuestaInformaLaPaginaActualEnBaseUnoYElTotal() {
        when(solicitudAuditoriaRepository.paginarPorEmpresa(any(), anyBoolean(), anyCollection(), any()))
                .thenReturn(paginaDe(veinticincoSolicitudes(), 1, 60));

        PaginaSolicitudesAuditoriaResponseDTO pagina = listar(ADMIN_ID, filtros(null, 2));

        assertThat(pagina.getPaginaActual()).isEqualTo(2);
        assertThat(pagina.getTotalResultados()).isEqualTo(60);
        assertThat(pagina.getTotalPaginas()).isEqualTo(3);
        assertThat(pagina.getContenido()).hasSize(25);
    }

    private static List<SolicitudAuditoria> veinticincoSolicitudes() {
        return java.util.stream.IntStream.range(0, 25)
                .mapToObj(i -> base(EstadoSolicitudAuditoria.EN_REVISION))
                .toList();
    }

    @Test
    void elFiltroDeEstadoLlegaALaConsulta() {
        listar(ADMIN_ID, filtros(List.of("EN_REVISION", "reporte_cargado"), 1));

        ArgumentCaptor<Collection<EstadoSolicitudAuditoria>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(any(), eq(false), captor.capture(), any());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                EstadoSolicitudAuditoria.EN_REVISION, EstadoSolicitudAuditoria.REPORTE_CARGADO);
    }

    /**
     * Un valor que no pertenece al catalogo se ignora y el listado sale completo. La bandera en
     * {@code true} es lo que apaga la condicion del filtro en la consulta.
     */
    @Test
    void unEstadoInventadoSeIgnoraYDevuelveElListadoCompleto() {
        listar(ADMIN_ID, filtros(List.of("ESTADO_QUE_NO_EXISTE"), 1));

        verify(solicitudAuditoriaRepository).paginarPorEmpresa(any(), eq(true), anyCollection(), any());
    }

    /** Mezclado con uno valido, el invalido se cae y el valido sigue filtrando. */
    @Test
    void unEstadoInvalidoNoAnulaAlValidoQueLoAcompania() {
        listar(ADMIN_ID, filtros(List.of("ESTADO_QUE_NO_EXISTE", "EN_REVISION"), 1));

        ArgumentCaptor<Collection<EstadoSolicitudAuditoria>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(any(), eq(false), captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(EstadoSolicitudAuditoria.EN_REVISION);
    }

    /**
     * La consulta nunca recibe una coleccion vacia aunque el filtro se haya descartado entero:
     * {@code in ()} sin elementos es SQL invalido, incluso en una rama que la bandera apaga.
     */
    @Test
    void sinFiltroLaConsultaNoRecibeUnaColeccionVacia() {
        listar(ADMIN_ID);

        ArgumentCaptor<Collection<EstadoSolicitudAuditoria>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(any(), eq(true), captor.capture(), any());
        assertThat(captor.getValue()).isNotEmpty();
    }

    /**
     * El auditor consulta por la union: su asignacion vigente mas el historial. Sin el estado de
     * asignacion la consulta no podria distinguir "yo gestione esto" de cualquier otra transicion.
     */
    @Test
    void elListadoDelAuditorConsultaTambienPorElHistorialDeAsignacion() {
        listar(AUDITOR_ID);

        verify(solicitudAuditoriaRepository).paginarPorAuditor(
                eq(AUDITOR_ID),
                eq(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO),
                anyBoolean(), anyCollection(), any());
    }

    /**
     * Pedir el listado de otro se rechaza con 403 en vez de devolver el propio en silencio: asi
     * quien lo pide se entera, en lugar de creer que esta viendo datos ajenos.
     */
    @Test
    void unaEmpresaQuePideElListadoDeOtraRecibe403() {
        FiltrarSolicitudesAuditoriaRequestDTO ajeno = new FiltrarSolicitudesAuditoriaRequestDTO();
        ajeno.setIdEmpresa(OTRA_EMPRESA_ID);

        assertThatThrownBy(() -> listar(ADMIN_ID, ajeno))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** Mandar el id propio es redundante pero no es un ataque: se atiende igual. */
    @Test
    void unaEmpresaPuedeMandarSuPropioIdSinQueLoRechacen() {
        FiltrarSolicitudesAuditoriaRequestDTO propio = new FiltrarSolicitudesAuditoriaRequestDTO();
        propio.setIdEmpresa(EMPRESA_ID);

        listar(ADMIN_ID, propio);

        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(eq(EMPRESA_ID), anyBoolean(), anyCollection(), any());
    }

    @Test
    void unAuditorQuePideElListadoDeOtroRecibe403() {
        FiltrarSolicitudesAuditoriaRequestDTO ajeno = new FiltrarSolicitudesAuditoriaRequestDTO();
        ajeno.setIdAuditor(UUID.randomUUID());

        assertThatThrownBy(() -> listar(AUDITOR_ID, ajeno))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** El id de empresa no aplica al auditor, asi que enviarlo tambien es pedir lo ajeno. */
    @Test
    void unAuditorQueMandaUnIdDeEmpresaRecibe403() {
        FiltrarSolicitudesAuditoriaRequestDTO conEmpresa = new FiltrarSolicitudesAuditoriaRequestDTO();
        conEmpresa.setIdEmpresa(EMPRESA_ID);

        assertThatThrownBy(() -> listar(AUDITOR_ID, conEmpresa))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elAdministradorDePlataformaSiPuedeConsultarElListadoDeOtraEmpresa() {
        FiltrarSolicitudesAuditoriaRequestDTO deOtra = new FiltrarSolicitudesAuditoriaRequestDTO();
        deOtra.setIdEmpresa(OTRA_EMPRESA_ID);

        listar(PLATAFORMA_ID, deOtra);

        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(eq(OTRA_EMPRESA_ID), anyBoolean(), anyCollection(), any());
    }

    @Test
    void elAdministradorDePlataformaTambienPuedeConsultarPorAuditor() {
        FiltrarSolicitudesAuditoriaRequestDTO deUnAuditor = new FiltrarSolicitudesAuditoriaRequestDTO();
        deUnAuditor.setIdAuditor(AUDITOR_ID);

        listar(PLATAFORMA_ID, deUnAuditor);

        verify(solicitudAuditoriaRepository)
                .paginarPorAuditor(eq(AUDITOR_ID), any(), anyBoolean(), anyCollection(), any());
    }

    /** No es empresa ni auditor: sin decir de quien, no hay listado que devolverle. */
    @Test
    void elAdministradorDePlataformaSinIdRecibe400() {
        assertThatThrownBy(() -> listar(PLATAFORMA_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Pageable capturarPageableDeEmpresa() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudAuditoriaRepository)
                .paginarPorEmpresa(any(), anyBoolean(), anyCollection(), captor.capture());
        return captor.getValue();
    }

    private static FiltrarSolicitudesAuditoriaRequestDTO filtros(List<String> estados, Integer pagina) {
        FiltrarSolicitudesAuditoriaRequestDTO filtros = new FiltrarSolicitudesAuditoriaRequestDTO();
        filtros.setFiltroEstado(estados);
        filtros.setPagina(pagina);
        return filtros;
    }

    private static Page<SolicitudAuditoria> paginaDe(List<SolicitudAuditoria> contenido,
                                                     int paginaBaseCero,
                                                     long total) {
        return new PageImpl<>(contenido, PageRequest.of(paginaBaseCero, 25), total);
    }

    private PaginaSolicitudesAuditoriaResponseDTO listar(UUID usuarioId) {
        return service.listar(new FiltrarSolicitudesAuditoriaRequestDTO(), usuarioId);
    }

    private PaginaSolicitudesAuditoriaResponseDTO listar(UUID usuarioId,
                                                         FiltrarSolicitudesAuditoriaRequestDTO filtros) {
        return service.listar(filtros, usuarioId);
    }

    private static SolicitudAuditoria conAuditor() {
        SolicitudAuditoria solicitud = base(EstadoSolicitudAuditoria.EN_REVISION);
        solicitud.setAuditor(Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build());
        solicitud.setFechaAsignacion(Instant.parse("2026-07-01T10:00:00Z"));
        solicitud.agregarDocumento(DocumentoRespaldo.builder().nombreArchivo("uno.pdf").build());
        return solicitud;
    }

    private static SolicitudAuditoria sinAuditor() {
        return base(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    private static SolicitudAuditoria base(EstadoSolicitudAuditoria estado) {
        return SolicitudAuditoria.builder()
                .id(UUID.randomUUID())
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2025, 1, 1))
                .periodoFin(LocalDate.of(2025, 12, 31))
                .estado(estado)
                .fechaCreacion(Instant.parse("2026-06-02T14:32:00Z"))
                .documentos(new ArrayList<>())
                .build();
    }
}
