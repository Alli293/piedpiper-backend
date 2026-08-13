package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private EmisionRepository emisionRepository;
    @Mock
    private ImaService imaService;
    @Mock
    private ImaEventosService imaEventosService;
    @Mock
    private ImaTendenciaBackfillService imaTendenciaBackfillService;

    @InjectMocks
    private ImaTendenciaService imaTendenciaService;

    private YearMonth mesActual;

    @BeforeEach
    void setUp() {
        mesActual = YearMonth.now();
        // Por defecto la detección no aporta eventos: los tests de serie/sector no dependen de ella.
        // Es lenient porque algunos tests fallan en la validación antes de invocar la detección.
        lenient().when(imaEventosService.detectar(any(), any(), any(), any())).thenReturn(List.of());
        // Por defecto la empresa no tiene emisiones previas en ningún mes: no se completa ningún
        // snapshot faltante, que es el comportamiento que ya asumían los tests existentes.
        lenient()
                .when(emisionRepository.existsByEmpresaIdAndFechaActividadLessThanEqual(any(), any()))
                .thenReturn(false);
    }

    /**
     * Agregado con promedio sectorial poblado: representa un mes en que el sector
     * alcanzó el umbral de empresas elegibles y por tanto ImaService expuso promedioIma.
     */
    private AgregadoSectorial agregadoConPromedio(YearMonth periodo, int cantidadEmpresas, String promedioIma) {
        return AgregadoSectorial.builder()
                .sector(SectorIndustrial.AGROINDUSTRIA)
                .anio(periodo.getYear())
                .mes(periodo.getMonthValue())
                .cantidadEmpresas(cantidadEmpresas)
                .promedioIma(new BigDecimal(promedioIma))
                .build();
    }

    /**
     * Agregado sin promedio (promedioIma == null): el sector no alcanzó el umbral,
     * exactamente como lo persiste ImaService cuando hay menos de 5 empresas elegibles.
     */
    private AgregadoSectorial agregadoSinPromedio(YearMonth periodo, int cantidadEmpresas) {
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

    private void mockEmpresa() {
        Empresa empresa = Empresa.builder()
                .id(EMPRESA_ID)
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .build();
        when(imaService.empresaDe(USUARIO_ID)).thenReturn(empresa);
    }

    private ImaSnapshot snapshot(YearMonth periodo, String ima) {
        return ImaSnapshot.builder()
                .empresaId(EMPRESA_ID)
                .anio(periodo.getYear())
                .mes(periodo.getMonthValue())
                .ima(new BigDecimal(ima))
                .build();
    }

    @Test
    void armaSerieDeDoceMesesPorDefecto() {
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        mockAgregados();

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
    void marcaCompletandoYDisparaElBackfillEnSegundoPlanoCuandoHayEmisionesSinSnapshot() {
        // La empresa tiene emisiones registradas para mesActual pero nadie abrió GET /api/ima
        // para ese mes puntual, así que imaSnapshotRepository no tiene esa fila todavía. El
        // backfill corre async (ImaTendenciaBackfillService): esta respuesta no lo espera, solo
        // lo dispara y avisa con completando=true para que el cliente vuelva a consultar.
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(emisionRepository.existsByEmpresaIdAndFechaActividadLessThanEqual(eq(EMPRESA_ID), any()))
                .thenReturn(true);
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(1, USUARIO_ID);

        verify(imaTendenciaBackfillService).completarMesesPendientes(USUARIO_ID, List.of(mesActual));
        assertThat(respuesta.getSerie().getLast().getImaEmpresa()).isNull();
        assertThat(respuesta.isCompletando()).isTrue();
    }

    @Test
    void noDisparaElBackfillNiMarcaCompletandoSinNingunaEmisionPrevia() {
        // Sin emisiones registradas en ningún mes de la ventana: son meses previos a que la
        // empresa existiera, así que deben quedar como hueco y no como un IMA calculado en cero.
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        mockAgregados();

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(3, USUARIO_ID);

        verify(imaTendenciaBackfillService, never()).completarMesesPendientes(any(), any());
        assertThat(respuesta.getSerie()).allSatisfy(punto -> assertThat(punto.getImaEmpresa()).isNull());
        assertThat(respuesta.isCompletando()).isFalse();
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

    @Test
    void invocaLaDeteccionDeEventosConLaSerieYPropagaElResultado() {
        mockEmpresa();
        when(imaSnapshotRepository.findVentana(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(snapshot(mesActual, "70.0")));
        mockAgregados(agregadoConPromedio(mesActual, 6, "60.0"));

        ImaEventoDTO evento = ImaEventoDTO.builder()
                .mes(mesActual.toString())
                .tipo(TipoEventoIma.CRUCE_SECTOR)
                .texto("evento de prueba")
                .build();
        when(imaEventosService.detectar(eq(EMPRESA_ID), any(), any(), any()))
                .thenReturn(List.of(evento));

        ImaTendenciaResponseDTO respuesta = imaTendenciaService.obtenerTendencia(2, USUARIO_ID);

        // La detección recibe exactamente la serie que construyó el servicio, no otra.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImaTendenciaPuntoDTO>> serieCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(imaEventosService).detectar(eq(EMPRESA_ID), serieCaptor.capture(), any(), any());
        assertThat(serieCaptor.getValue()).isSameAs(respuesta.getSerie());

        // Y los eventos detectados se propagan tal cual al response.
        assertThat(respuesta.getEventos()).containsExactly(evento);
    }
}