package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository.PromedioSectorialMensual;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaTendenciaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;
    @Mock
    private AgregadoSectorialRepository agregadoSectorialRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ImaTendenciaService imaTendenciaService;

    private YearMonth mesActual;

    @BeforeEach
    void setUp() {
        mesActual = YearMonth.from(LocalDate.now());
    }

    private AgregadoSectorial agregado(YearMonth periodo, int cantidadEmpresas) {
        return AgregadoSectorial.builder()
                .sector(SectorIndustrial.AGROINDUSTRIA)
                .anio(periodo.getYear())
                .mes(periodo.getMonthValue())
                .cantidadEmpresas(cantidadEmpresas)
                .build();
    }

    private void mockAgregados(AgregadoSectorial... agregados) {
        when(agregadoSectorialRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(agregados));
    }

    private void mockUsuarioConEmpresa() {
        Empresa empresa = Empresa.builder()
                .id(EMPRESA_ID)
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .build();
        Usuario usuario = Usuario.builder().id(USUARIO_ID).empresa(empresa).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    private ImaSnapshot snapshot(YearMonth periodo, String ima) {
        return ImaSnapshot.builder()
                .empresaId(EMPRESA_ID)
                .anio(periodo.getYear())
                .mes(periodo.getMonthValue())
                .ima(new BigDecimal(ima))
                .build();
    }

    private PromedioSectorialMensual promedio(YearMonth periodo, String ima) {
        return new PromedioSectorialMensual() {
            @Override
            public Integer getAnio() {
                return periodo.getYear();
            }

            @Override
            public Integer getMes() {
                return periodo.getMonthValue();
            }

            @Override
            public BigDecimal getPromedioIma() {
                return new BigDecimal(ima);
            }
        };
    }

    @Test
    void armaSerieDeDoceMesesPorDefecto() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(null, USUARIO_ID);

        assertThat(respuesta.getMesesAtras()).isEqualTo(12);
        assertThat(respuesta.getSerie()).hasSize(12);
        assertThat(respuesta.getSerie().getLast().getMes()).isEqualTo(mesActual.toString());
    }

    @Test
    void tomaLosValoresDeLosSnapshotsPersistidos() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "71.5")));
        mockAgregados(agregado(mesActual, 8));
        when(imaSnapshotRepository.promediarImaPorSector(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(promedio(mesActual, "64.25")));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(3, USUARIO_ID);

        ImaTendenciaPuntoDTO ultimo = respuesta.getSerie().getLast();
        assertThat(respuesta.getSerie()).hasSize(3);
        assertThat(ultimo.getImaEmpresa()).isEqualByComparingTo("71.5");
        assertThat(ultimo.getImaPromedioSector()).isEqualByComparingTo("64.3");
        assertThat(respuesta.isSinDatosSectoriales()).isFalse();
    }

    @Test
    void omitePuntosSinDatoYNoRellenaConCeros() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(2, USUARIO_ID);

        assertThat(respuesta.getSerie().getFirst().getImaEmpresa()).isNull();
        assertThat(respuesta.getSerie().getLast().getImaEmpresa()).isEqualByComparingTo("70.0");
    }

    @Test
    void noDibujaLineaSectorialCuandoElSectorNoAlcanzaCincoEmpresas() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados(agregado(mesActual, 4));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(1, USUARIO_ID);

        assertThat(respuesta.getSerie().getLast().getImaPromedioSector()).isNull();
        assertThat(respuesta.isSinDatosSectoriales()).isTrue();
    }

    @Test
    void sinHistorialDevuelveSerieCompletaConPuntosNulos() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(6, USUARIO_ID);

        assertThat(respuesta.getSerie()).hasSize(6);
        assertThat(respuesta.getSerie()).allSatisfy(punto -> {
            assertThat(punto.getImaEmpresa()).isNull();
            assertThat(punto.getImaPromedioSector()).isNull();
        });
    }

    @Test
    void ventanaCeroEsRechazadaPorElServicio() {
        assertThatThrownBy(() -> imaTendenciaService.obtenerTendencia(0, USUARIO_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void ventanaMayorALaMaximaEsRechazadaPorElServicio() {
        assertThatThrownBy(() -> imaTendenciaService.obtenerTendencia(13, USUARIO_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void noDibujaLineaSectorialSiElAgregadoNoAlcanzaElUmbralAunqueHayaSnapshots() {
        // Hay promedio calculable, pero AgregadoSectorial reporta 4 empresas elegibles:
        // debe primar el umbral de ImaService para no divergir de /api/ima.
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados(agregado(mesActual, 4));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(1, USUARIO_ID);

        assertThat(respuesta.getSerie().getLast().getImaPromedioSector()).isNull();
        assertThat(respuesta.isSinDatosSectoriales()).isTrue();
    }

    @Test
    void usuarioSinEmpresaLanzaEmpresaNoConfigurada() {
        Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> imaTendenciaService.obtenerTendencia(12, USUARIO_ID))
                .isInstanceOf(ApiException.class);
    }
}
