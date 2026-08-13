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
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecomendacionAuditoresServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RecomendacionAuditoresIaService recomendacionAuditoresIaService;

    private RecomendacionAuditoresService service;

    @BeforeEach
    void configurar() {
        service = new RecomendacionAuditoresService(
                perfilAuditorRepository, usuarioRepository, recomendacionAuditoresIaService);

        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(ADMIN_ID)
                .empresa(Empresa.builder()
                        .id(EMPRESA_ID)
                        .nombreEmpresa("Acme S.A.")
                        .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                        .build())
                .build()));
        when(recomendacionAuditoresIaService.generarJustificaciones(
                any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(Map.of()));
    }

    @Test
    void devuelveComoMaximoCincoCandidatos() {
        List<PerfilAuditor> seis = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            seis.add(auditor("Aud" + i, BigDecimal.valueOf(5), 10, 0));
        }
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(seis);

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones()).hasSize(RecomendacionAuditoresService.MAXIMO_RECOMENDACIONES);
    }

    @Test
    void consultaSoloAuditoresCertificadosYActivos() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        service.recomendar(filtros(), ADMIN_ID);

        verify(perfilAuditorRepository).buscarCandidatosRecomendacion(
                eq(Rol.AUDITOR_CERTIFICADO), eq(EstadoUsuario.ACTIVO),
                eq(EspecialidadAuditor.MANUFACTURA), eq(ProvinciaCR.SAN_JOSE), eq(true));
    }

    /** La historia lo pone primero en el orden: coincidir con el tipo de auditoría manda sobre todo. */
    @Test
    void laCoincidenciaConElTipoDeAuditoriaPesaMasQueLaCalificacion() {
        PerfilAuditor sinEspecialidad = auditor("Sin", BigDecimal.valueOf(5), 100, 50,
                EspecialidadAuditor.TURISMO_SOSTENIBLE);
        PerfilAuditor conEspecialidad = auditor("Con", BigDecimal.valueOf(1), 0, 0,
                EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(sinEspecialidad, conEspecialidad));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones().get(0).getNombre()).isEqualTo("Con Auditor");
    }

    /**
     * Un auditor recién certificado llega con {@code auditoriasCompletadas} en nulo, porque
     * {@code MetricasReputacionAuditorService.dejarSinDatos()} representa "ninguna" con nulo y no
     * con cero. Es el caso más común en un sistema nuevo, y desempaquetarlo al ordenar tiraba el
     * endpoint entero con un 500.
     */
    @Test
    void unCandidatoSinAuditoriasCompletadasNoRompeElOrden() {
        PerfilAuditor recienCertificado = auditor("Nuevo", BigDecimal.valueOf(5), null, 0,
                EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor conHistorial = auditor("Veterano", BigDecimal.valueOf(5), 20, 0,
                EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(recienCertificado, conHistorial));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones()).hasSize(2);
        // El nulo cuenta como cero, así que el que sí tiene historial queda primero.
        assertThat(respuesta.getRecomendaciones().get(0).getNombre()).isEqualTo("Veterano Auditor");
    }

    /** Con todos los candidatos en nulo tampoco puede reventar: es el arranque del sistema. */
    @Test
    void todosLosCandidatosSinAuditoriasCompletadasSiguenSaliendo() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(
                        auditor("Uno", BigDecimal.valueOf(4), null, 0, EspecialidadAuditor.MANUFACTURA),
                        auditor("Dos", null, null, 0, EspecialidadAuditor.MANUFACTURA)));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones()).hasSize(2);
    }

    /**
     * {@code tipoAuditoria} y {@code especialidadBuscada} son campos distintos del mismo catálogo.
     * Si en las pruebas siempre coinciden, la consulta ya devuelve solo candidatos que cumplen el
     * primer criterio y ese nivel del orden nunca se ejercita de verdad.
     */
    @Test
    void cuandoElTipoDeAuditoriaDifiereDeLaEspecialidadBuscadaElOrdenLoDistingue() {
        PerfilAuditor soloEspecialidadBuscada = auditor("Solo", BigDecimal.valueOf(5), 100, 0,
                EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor ambas = auditor("Ambas", BigDecimal.valueOf(1), 0, 0,
                EspecialidadAuditor.MANUFACTURA, EspecialidadAuditor.ENERGIA_RENOVABLE);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(soloEspecialidadBuscada, ambas));

        RecomendarAuditoresRequestDTO filtros = filtros();
        filtros.setEspecialidadBuscada("MANUFACTURA");
        filtros.setTipoAuditoria("ENERGIA_RENOVABLE");

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros, ADMIN_ID);

        // Gana quien cubre el tipo de auditoría, aunque tenga peor calificación e historial.
        assertThat(respuesta.getRecomendaciones().get(0).getNombre()).isEqualTo("Ambas Auditor");
    }

    /** Segundo criterio: a igualdad de especialidad, gana quien ya auditó el sector de la empresa. */
    @Test
    void aIgualEspecialidadGanaQuienTieneExperienciaEnElSectorDeLaEmpresa() {
        PerfilAuditor sinSector = auditor("SinSector", BigDecimal.valueOf(5), 100, 0,
                EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor conSector = auditor("ConSector", BigDecimal.valueOf(1), 0, 7,
                EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(sinSector, conSector));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones().get(0).getNombre()).isEqualTo("ConSector Auditor");
    }

    /** Tercer y cuarto criterio, con los dos primeros empatados. */
    @Test
    void aIgualEspecialidadYSectorOrdenaPorCalificacionYLuegoPorAuditorias() {
        PerfilAuditor bajo = auditor("Bajo", BigDecimal.valueOf(3), 90, 5, EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor alto = auditor("Alto", BigDecimal.valueOf(4), 1, 5, EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor empatado = auditor("Empatado", BigDecimal.valueOf(4), 80, 5,
                EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(bajo, alto, empatado));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones())
                .extracting(AuditorRecomendadoResponseDTO::getNombre)
                .containsExactly("Empatado Auditor", "Alto Auditor", "Bajo Auditor");
    }

    /** Un auditor sin calificaciones no puede colarse arriba por tener el campo nulo. */
    @Test
    void unAuditorSinCalificacionQuedaDeUltimoYNoRompeElOrden() {
        PerfilAuditor sinCalificar = auditor("SinCalificar", null, 100, 5, EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor calificado = auditor("Calificado", BigDecimal.valueOf(2), 0, 5,
                EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(sinCalificar, calificado));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones().get(0).getNombre()).isEqualTo("Calificado Auditor");
    }

    @Test
    void sinCandidatosDevuelveListaVaciaYNoLlamaALaIa() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones()).isEmpty();
        // true y no false: la IA no falló, simplemente no había a quién justificar.
        assertThat(respuesta.isIaDisponible()).isTrue();
        verify(recomendacionAuditoresIaService, never())
                .generarJustificaciones(any(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void asignaCadaJustificacionAlAuditorQueLeCorresponde() {
        PerfilAuditor uno = auditor("Uno", BigDecimal.valueOf(5), 10, 5, EspecialidadAuditor.MANUFACTURA);
        PerfilAuditor dos = auditor("Dos", BigDecimal.valueOf(4), 10, 5, EspecialidadAuditor.MANUFACTURA);
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(uno, dos));
        when(recomendacionAuditoresIaService.generarJustificaciones(
                any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(Map.of(
                        uno.getAuditor().getId(), "Justificación de Uno.",
                        dos.getAuditor().getId(), "Justificación de Dos.")));

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones().get(0).getJustificacion()).isEqualTo("Justificación de Uno.");
        assertThat(respuesta.getRecomendaciones().get(1).getJustificacion()).isEqualTo("Justificación de Dos.");
        assertThat(respuesta.isIaDisponible()).isTrue();
    }

    /** Degradación controlada: la IA cae y el flujo de asignación sigue funcionando igual. */
    @Test
    void siLaIaFallaDevuelveLosCandidatosSinJustificacionYAvisaQueNoEstaDisponible() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(auditor("Uno", BigDecimal.valueOf(5), 10, 5,
                        EspecialidadAuditor.MANUFACTURA)));
        when(recomendacionAuditoresIaService.generarJustificaciones(
                any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.empty());

        RecomendacionAuditoresResponseDTO respuesta = service.recomendar(filtros(), ADMIN_ID);

        assertThat(respuesta.getRecomendaciones()).hasSize(1);
        assertThat(respuesta.getRecomendaciones().get(0).getJustificacion()).isNull();
        assertThat(respuesta.getRecomendaciones().get(0).getAuditorId()).isNotNull();
        assertThat(respuesta.isIaDisponible()).isFalse();
    }

    /**
     * El sector sale del token, nunca del cuerpo: si viniera de afuera, cualquiera pediría
     * recomendaciones haciéndose pasar por una empresa de otro sector.
     */
    @Test
    void elSectorQueViajaALaIaSaleDeLaEmpresaDelUsuarioAutenticado() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(auditor("Uno", BigDecimal.valueOf(5), 10, 5,
                        EspecialidadAuditor.MANUFACTURA)));

        service.recomendar(filtros(), ADMIN_ID);

        ArgumentCaptor<String> sector = ArgumentCaptor.forClass(String.class);
        verify(recomendacionAuditoresIaService).generarJustificaciones(
                eq(EMPRESA_ID), sector.capture(), anyString(), anyString(), anyList());
        assertThat(sector.getValue()).isEqualTo("MANUFACTURA");
    }

    /** Lo que se le manda a la IA son datos públicos del auditor, nunca el nombre de la empresa. */
    @Test
    void losCandidatosQueViajanALaIaNoLlevanDatosDeLaEmpresa() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(auditor("Uno", BigDecimal.valueOf(5), 10, 5,
                        EspecialidadAuditor.MANUFACTURA)));

        service.recomendar(filtros(), ADMIN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CandidatoIa>> captor = ArgumentCaptor.forClass(List.class);
        verify(recomendacionAuditoresIaService).generarJustificaciones(
                any(), anyString(), anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).allSatisfy(candidato ->
                assertThat(candidato.nombre()).doesNotContain("Acme"));
    }

    @Test
    void soloDisponiblesEsTruePorDefectoCuandoNoViene() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());
        RecomendarAuditoresRequestDTO sinBandera = filtros();
        sinBandera.setSoloDisponibles(null);

        service.recomendar(sinBandera, ADMIN_ID);

        verify(perfilAuditorRepository).buscarCandidatosRecomendacion(any(), any(), any(), any(), eq(true));
    }

    @Test
    void soloDisponiblesEnFalseIncluyeALosNoDisponibles() {
        when(perfilAuditorRepository.buscarCandidatosRecomendacion(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());
        RecomendarAuditoresRequestDTO sinFiltro = filtros();
        sinFiltro.setSoloDisponibles(false);

        service.recomendar(sinFiltro, ADMIN_ID);

        verify(perfilAuditorRepository).buscarCandidatosRecomendacion(any(), any(), any(), any(), eq(false));
    }

    @Test
    void unaEspecialidadFueraDelCatalogoDevuelve400() {
        RecomendarAuditoresRequestDTO invalidos = filtros();
        invalidos.setEspecialidadBuscada("BUCEO");

        assertThatThrownBy(() -> service.recomendar(invalidos, ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unTipoDeAuditoriaFueraDelCatalogoDevuelve400() {
        RecomendarAuditoresRequestDTO invalidos = filtros();
        invalidos.setTipoAuditoria("LO_QUE_SEA");

        assertThatThrownBy(() -> service.recomendar(invalidos, ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unaZonaFueraDelCatalogoDevuelve400() {
        RecomendarAuditoresRequestDTO invalidos = filtros();
        invalidos.setZonaGeografica("MADRID");

        assertThatThrownBy(() -> service.recomendar(invalidos, ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unUsuarioSinEmpresaConfiguradaRecibe422() {
        when(usuarioRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(Usuario.builder().id(ADMIN_ID).build()));

        assertThatThrownBy(() -> service.recomendar(filtros(), ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static RecomendarAuditoresRequestDTO filtros() {
        return new RecomendarAuditoresRequestDTO("MANUFACTURA", "MANUFACTURA", "SAN_JOSE", true);
    }

    private static PerfilAuditor auditor(String nombre, BigDecimal calificacion, Integer auditorias,
                                        int auditoriasEnManufactura,
                                        EspecialidadAuditor... especialidades) {
        Set<EspecialidadAuditor> set = new LinkedHashSet<>(List.of(especialidades));
        List<DistribucionSectorAuditor> distribucion = new ArrayList<>();
        if (auditoriasEnManufactura > 0) {
            distribucion.add(new DistribucionSectorAuditor(
                    "MANUFACTURA", auditoriasEnManufactura, BigDecimal.valueOf(100)));
        }
        return PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(Usuario.builder()
                        .id(UUID.randomUUID())
                        .nombre(nombre)
                        .apellidos("Auditor")
                        .build())
                .calificacionPromedio(calificacion)
                .auditoriasCompletadas(auditorias)
                .disponible(true)
                .especialidades(set)
                .distribucionSectores(distribucion)
                .build();
    }
}
