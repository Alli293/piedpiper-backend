package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaTendenciaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ImaEventosService imaEventosService;

    @InjectMocks
    private ImaTendenciaService imaTendenciaService;

    private YearMonth mesActual;

    @BeforeEach
    void setUp() {
        mesActual = YearMonth.from(LocalDate.now());
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

    private PromedioSectorialMensual promedio(YearMonth periodo, String ima, long empresas) {
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

            @Override
            public long getCantidadEmpresas() {
                return empresas;
            }
        };
    }

    @Test
    void armaSerieDeDoceMesesPorDefecto() {
        mockUsuarioConEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(imaSnapshotRepository.promediarImaPorSector(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(null, USUARIO_ID);

        assertThat(respuesta.getMesesAtras()).isEqualTo(12);
        assertThat(respuesta.getSerie()).hasSize(12);
        assertThat(respuesta.getSerie().getLast().getMes()).isEqualTo(mesActual.toString());
    }

    @Test
    void tomaElPromedioSectorialDelAgregadoPersistido() {
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "71.5")));
        mockAgregados(agregadoConPromedio(mesActual, 8, "64.25"));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(3, USUARIO_ID);

        ImaTendenciaPuntoDTO ultimo = respuesta.getSerie().getLast();
        assertThat(respuesta.getSerie()).hasSize(3);
        assertThat(ultimo.getImaEmpresa()).isEqualByComparingTo("71.5");
        assertThat(ultimo.getImaPromedioSector()).isEqualByComparingTo("64.3");
        assertThat(respuesta.isSinDatosSectoriales()).isFalse();
    }

    @Test
    void omitePuntosSinDatoYNoRellenaConCeros() {
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(2, USUARIO_ID);

        assertThat(respuesta.getSerie().getFirst().getImaEmpresa()).isNull();
        assertThat(respuesta.getSerie().getLast().getImaEmpresa()).isEqualByComparingTo("70.0");
    }

    @Test
    void noDibujaLineaSectorialCuandoElAgregadoNoTienePromedio() {
        // El sector no alcanzó el umbral: ImaService persistió el agregado con promedioIma null.
        // La tendencia no debe inventar una línea sectorial que /api/ima no expondría.
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados(agregadoSinPromedio(mesActual, 4));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(1, USUARIO_ID);

        assertThat(respuesta.getSerie().getLast().getImaPromedioSector()).isNull();
        assertThat(respuesta.isSinDatosSectoriales()).isTrue();
    }

    @Test
    void mezclaMesesConYSinPromedioSectorial() {
        // Un mes elegible y el mes anterior sin promedio: solo el elegible dibuja línea sectorial,
        // usando el mismo valor que /benchmark ya persistió en el agregado.
        YearMonth mesAnterior = mesActual.minusMonths(1);
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesAnterior, "60.0"), snapshot(mesActual, "72.0")));
        mockAgregados(agregadoSinPromedio(mesAnterior, 3), agregadoConPromedio(mesActual, 6, "65.0"));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(2, USUARIO_ID);

        assertThat(respuesta.getSerie().getFirst().getImaPromedioSector()).isNull();
        assertThat(respuesta.getSerie().getLast().getImaPromedioSector()).isEqualByComparingTo("65.0");
        assertThat(respuesta.isSinDatosSectoriales()).isFalse();
    }

    @Test
    void sinHistorialDevuelveSerieCompletaConPuntosNulos() {
        mockEmpresa();
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
    void propagaEmpresaNoConfiguradaDeImaService() {
        when(imaService.empresaDe(USUARIO_ID)).thenThrow(ApiException.empresaNoConfigurada());

        assertThatThrownBy(() -> imaTendenciaService.obtenerTendencia(12, USUARIO_ID))
                .isInstanceOf(ApiException.class);
    }
}
