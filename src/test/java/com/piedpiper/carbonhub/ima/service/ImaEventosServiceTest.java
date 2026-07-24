package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository.CategoriaMensual;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.enums.TipoEventoIma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaEventosServiceTest {

    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;

    @InjectMocks
    private ImaEventosService imaEventosService;

    private ImaTendenciaPuntoDTO punto(String mes, String empresa, String sector) {
        return ImaTendenciaPuntoDTO.builder()
                .mes(mes)
                .imaEmpresa(empresa == null ? null : new BigDecimal(empresa))
                .imaPromedioSector(sector == null ? null : new BigDecimal(sector))
                .build();
    }

    private CategoriaMensual categoria(int anio, int mes, CategoriaEmision categoria) {
        return new CategoriaMensual() {
            @Override
            public Integer getAnio() {
                return anio;
            }

            @Override
            public Integer getMes() {
                return mes;
            }

            @Override
            public CategoriaEmision getCategoria() {
                return categoria;
            }
        };
    }

    private void mockCategorias(CategoriaMensual... filas) {
        when(emisionRepository.listarCategoriasPorMes(eq(EMPRESA_ID), any(LocalDate.class)))
                .thenReturn(List.of(filas));
    }

    private List<ImaEventoDTO> detectar(List<ImaTendenciaPuntoDTO> serie) {
        YearMonth desde = YearMonth.parse(serie.getFirst().getMes());
        YearMonth hasta = YearMonth.parse(serie.getLast().getMes());
        return imaEventosService.detectar(EMPRESA_ID, serie, desde, hasta);
    }

    @Test
    void detectaCruceSectorCuandoElImaSuperaElPromedio() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "55.0", "60.0"),
                punto("2026-02", "65.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.CRUCE_SECTOR)
                .singleElement()
                .satisfies(evento -> {
                    assertThat(evento.getMes()).isEqualTo("2026-02");
                    assertThat(evento.getTexto())
                            .isEqualTo("En febrero 2026 tu IMA superó el promedio de tu sector.");
                });
    }

    @Test
    void detectaCruceSectorCuandoElImaQuedaPorDebajo() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "65.0", "60.0"),
                punto("2026-02", "55.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.CRUCE_SECTOR)
                .singleElement()
                .extracting(ImaEventoDTO::getTexto)
                .isEqualTo("En febrero 2026 tu IMA quedó por debajo del promedio de tu sector.");
    }

    @Test
    void noEmiteCruceSectorSinLineaSectorial() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "55.0", null),
                punto("2026-02", "65.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).noneMatch(e -> e.getTipo() == TipoEventoIma.CRUCE_SECTOR);
    }

    @Test
    void noEmiteCruceSectorAtravesandoUnHueco() {
        // enero por debajo del sector, febrero es un hueco (sin datos), marzo por encima.
        // No debe emitirse cruce: enero y marzo no son meses consecutivos.
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 3, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "55.0", "60.0"),
                punto("2026-02", null, null),
                punto("2026-03", "65.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).noneMatch(e -> e.getTipo() == TipoEventoIma.CRUCE_SECTOR);
    }

    @Test
    void emiteCruceSectorEntreMesesConsecutivos() {
        // Control del test anterior: sin hueco de por medio, el cruce sí se detecta.
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "55.0", "60.0"),
                punto("2026-02", "65.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.CRUCE_SECTOR)
                .singleElement()
                .satisfies(e -> assertThat(e.getMes()).isEqualTo("2026-02"));
    }

    @Test
    void emiteUnUnicoMarcadorDeMayorVariacion() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 3, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "50.0", null),
                punto("2026-02", "52.0", null),
                punto("2026-03", "70.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.MAYOR_VARIACION)
                .singleElement()
                .satisfies(evento -> {
                    assertThat(evento.getMes()).isEqualTo("2026-03");
                    assertThat(evento.getTexto()).contains("+18");
                });
    }

    @Test
    void noEmiteMayorVariacionCuandoLaSerieEsPlana() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", null),
                punto("2026-02", "60.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).noneMatch(e -> e.getTipo() == TipoEventoIma.MAYOR_VARIACION);
    }

    @Test
    void detectaHuecoDeDatosEnElMesSinRegistros() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 3, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", null),
                punto("2026-02", "60.0", null),
                punto("2026-03", "60.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.HUECO_DATOS)
                .singleElement()
                .satisfies(evento -> {
                    assertThat(evento.getMes()).isEqualTo("2026-02");
                    assertThat(evento.getTexto())
                            .isEqualTo("En febrero 2026 no registraste emisiones; bajó tu consistencia.");
                });
    }

    @Test
    void detectaNuevaCategoriaEnElPrimerRegistroDeVuelo() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 2, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.VUELO));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", null),
                punto("2026-02", "65.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getTipo() == TipoEventoIma.NUEVA_CATEGORIA
                        && e.getMes().equals("2026-02"))
                .singleElement()
                .extracting(ImaEventoDTO::getTexto)
                .isEqualTo("En febrero 2026 empezaste a registrar vuelos.");
    }

    @Test
    void noEmiteNuevaCategoriaSiYaSeRegistrabaAntesDeLaVentana() {
        // VUELO ya existía en 2025, fuera de la ventana que arranca en 2026-01.
        mockCategorias(categoria(2025, 11, CategoriaEmision.VUELO),
                categoria(2026, 1, CategoriaEmision.VUELO), categoria(2026, 2, CategoriaEmision.VUELO));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", null),
                punto("2026-02", "65.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).noneMatch(e -> e.getTipo() == TipoEventoIma.NUEVA_CATEGORIA);
    }

    @Test
    void listaTodosLosEventosCuandoCoincidenEnElMismoMes() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 2, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.VUELO));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "55.0", "60.0"),
                punto("2026-02", "75.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos)
                .filteredOn(e -> e.getMes().equals("2026-02"))
                .extracting(ImaEventoDTO::getTipo)
                .containsExactlyInAnyOrder(TipoEventoIma.CRUCE_SECTOR,
                        TipoEventoIma.MAYOR_VARIACION, TipoEventoIma.NUEVA_CATEGORIA);
    }

    @Test
    void sinEventosDetectablesDevuelveListaVacia() {
        // ELECTRICIDAD ya se registraba antes de la ventana, así que no es
        // categoría nueva; la serie es plana y nunca cruza al sector.
        mockCategorias(categoria(2025, 12, CategoriaEmision.ELECTRICIDAD),
                categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 2, CategoriaEmision.ELECTRICIDAD));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", "60.0"),
                punto("2026-02", "60.0", "60.0"));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).isEmpty();
    }

    @Test
    void devuelveLosEventosOrdenadosCronologicamente() {
        mockCategorias(categoria(2026, 1, CategoriaEmision.ELECTRICIDAD), categoria(2026, 3, CategoriaEmision.VUELO));
        List<ImaTendenciaPuntoDTO> serie = List.of(
                punto("2026-01", "60.0", null),
                punto("2026-02", "61.0", null),
                punto("2026-03", "80.0", null));

        List<ImaEventoDTO> eventos = detectar(serie);

        assertThat(eventos).extracting(ImaEventoDTO::getMes).isSorted();
    }
}
