package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsigniaEmpresaEvaluacionServiceTest {

    private static final UUID ID_EMPRESA =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private CatalogoInsigniaRepository catalogoInsigniaRepository;
    @Mock
    private InsigniaEmpresaRepository insigniaEmpresaRepository;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private InsigniaEmpresaRegistroService insigniaEmpresaRegistroService;

    private InsigniaEmpresaEvaluacionService service;

    @BeforeEach
    void prepararServicio() {
        service = new InsigniaEmpresaEvaluacionService(
                catalogoInsigniaRepository,
                insigniaEmpresaRepository,
                certificacionRepository,
                empresaRepository,
                insigniaEmpresaRegistroService);
    }

    @Test
    void otorgaBronceCuandoCumpleRequisitosDelCatalogo() {
        CatalogoInsignia bronce = insignia(1L, "bronce", 1,
                Set.of(TipoCertificacion.CARBONO_NEUTRAL));
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(bronce));
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "bronce"))
                .thenReturn(false);
        when(certificacionRepository.countByEmpresaIdAndEstado(
                ID_EMPRESA, EstadoCertificacion.ACTIVA))
                .thenReturn(1L);
        when(certificacionRepository.countDistinctTiposActivos(
                ID_EMPRESA, EstadoCertificacion.ACTIVA, bronce.getTiposCertificacionesRequeridas()))
                .thenReturn(1L);
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        ArgumentCaptor<InsigniaEmpresa> captor =
                ArgumentCaptor.forClass(InsigniaEmpresa.class);
        verify(insigniaEmpresaRegistroService).registrar(captor.capture());
        assertThat(captor.getValue().getIdInsignia()).isEqualTo(1L);
        assertThat(captor.getValue().getNivelInsignia()).isEqualTo("bronce");
        assertThat(captor.getValue().getFechaObtencion()).isNotNull();
    }

    @Test
    void noOtorgaPlataSiNoExisteBroncePrevio() {
        CatalogoInsignia plata = insignia(1L, "plata", 2, Set.of());
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(plata));
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "bronce"))
                .thenReturn(false);

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        verify(insigniaEmpresaRegistroService, never()).registrar(any());
    }

    @Test
    void otorgaPlataConBroncePrevioYRequisitosCumplidos() {
        CatalogoInsignia plata = insignia(1L, "plata", 2, Set.of());
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(plata));
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "bronce"))
                .thenReturn(true);
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "plata"))
                .thenReturn(false);
        when(certificacionRepository.countByEmpresaIdAndEstado(
                ID_EMPRESA, EstadoCertificacion.ACTIVA))
                .thenReturn(2L);
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        ArgumentCaptor<InsigniaEmpresa> captor =
                ArgumentCaptor.forClass(InsigniaEmpresa.class);
        verify(insigniaEmpresaRegistroService).registrar(captor.capture());
        assertThat(captor.getValue().getNivelInsignia()).isEqualTo("plata");
    }

    @Test
    void noDuplicaInsigniaYaOtorgada() {
        CatalogoInsignia bronce = insignia(1L, "bronce", 1, Set.of());
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(bronce));
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "bronce"))
                .thenReturn(true);

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        verify(insigniaEmpresaRegistroService, never()).registrar(any());
    }

    @Test
    void falloAlRegistrarUnaInsigniaPermiteEvaluarLasDemas() {
        CatalogoInsignia primera = insignia(1L, "bronce", 1, Set.of());
        CatalogoInsignia segunda = insignia(2L, "bronce", 1, Set.of());
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(primera, segunda));
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 1L, "bronce"))
                .thenReturn(false);
        when(insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                ID_EMPRESA, 2L, "bronce"))
                .thenReturn(false);
        when(certificacionRepository.countByEmpresaIdAndEstado(
                ID_EMPRESA, EstadoCertificacion.ACTIVA))
                .thenReturn(1L);
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        doThrow(new IllegalStateException("fallo parcial"))
                .doNothing()
                .when(insigniaEmpresaRegistroService).registrar(any(InsigniaEmpresa.class));

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        verify(insigniaEmpresaRegistroService, times(2)).registrar(any());
    }

    @Test
    void nivelInvalidoEsRechazadoPorElProcesoDeOtorgamiento() {
        CatalogoInsignia invalida = insignia(1L, "diamante", 1, Set.of());
        when(catalogoInsigniaRepository.findByActivaTrueOrderByIdInsigniaAscNivelInsigniaAsc())
                .thenReturn(List.of(invalida));

        service.evaluarPorNuevaCertificacion(ID_EMPRESA);

        verify(insigniaEmpresaRegistroService, never()).registrar(any());
    }

    private CatalogoInsignia insignia(Long idInsignia,
                                      String nivel,
                                      Integer cantidadMinima,
                                      Set<TipoCertificacion> tipos) {
        return CatalogoInsignia.builder()
                .idInsignia(idInsignia)
                .nombre("Insignia " + idInsignia)
                .descripcion("Descripcion de prueba")
                .nivelInsignia(nivel)
                .cantidadMinimaCertificacionesActivas(cantidadMinima)
                .tiposCertificacionesRequeridas(tipos)
                .activa(true)
                .build();
    }
}
