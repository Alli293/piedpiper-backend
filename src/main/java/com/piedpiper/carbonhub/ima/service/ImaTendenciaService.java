package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaEventoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaPuntoDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaTendenciaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ImaTendenciaService {

    public static final int MESES_VENTANA_MAXIMA = 12;


    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final ImaService imaService;
    private final ImaEventosService imaEventosService;

    public ImaTendenciaService(ImaSnapshotRepository imaSnapshotRepository,
                               AgregadoSectorialRepository agregadoSectorialRepository,
                               ImaService imaService,
                               ImaEventosService imaEventosService) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.imaService = imaService;
        this.imaEventosService = imaEventosService;
    }

    @Transactional(readOnly = true)
    public ImaTendenciaResponseDTO obtenerTendencia(Integer mesesAtras, UUID usuarioId) {
        int ventana = resolverVentana(mesesAtras);
        Empresa empresa = imaService.empresaDe(usuarioId);

        YearMonth hasta = YearMonth.now(ZoneId.systemDefault());
        YearMonth desde = hasta.minusMonths(ventana - 1L);

        Map<YearMonth, BigDecimal> imaPorMes = indexarSnapshots(empresa.getId(), desde, hasta);
        Map<YearMonth, BigDecimal> promedioPorMes = indexarPromediosSectoriales(empresa, desde, hasta);

        List<ImaTendenciaPuntoDTO> serie = construirSerie(desde, ventana, imaPorMes, promedioPorMes);
        List<ImaEventoDTO> eventos = imaEventosService.detectar(empresa.getId(), serie, desde, hasta);

        return ImaTendenciaResponseDTO.builder()
                .mesesAtras(ventana)
                .serie(serie)
                .sinDatosSectoriales(promedioPorMes.isEmpty())
                .eventos(eventos)
                .build();
    }

    /** Valida la ventana solicitada; ausente equivale a la ventana máxima. */
    private int resolverVentana(Integer mesesAtras) {
        if (mesesAtras == null) {
            return MESES_VENTANA_MAXIMA;
        }
        if (mesesAtras < 1 || mesesAtras > MESES_VENTANA_MAXIMA) {
            throw ApiException.periodoImaInvalido(
                    "La ventana debe estar entre 1 y " + MESES_VENTANA_MAXIMA + " meses.");
        }
        return mesesAtras;
    }

    /**
     * Construye un punto por cada mes de la ventana. Los meses sin dato quedan en null:
     * no se rellenan con ceros para que la gráfica no invente valores.
     */
    private List<ImaTendenciaPuntoDTO> construirSerie(YearMonth desde, int ventana,
                                                      Map<YearMonth, BigDecimal> imaPorMes,
                                                      Map<YearMonth, BigDecimal> promedioPorMes) {
        List<ImaTendenciaPuntoDTO> serie = new ArrayList<>(ventana);
        for (int i = 0; i < ventana; i++) {
            YearMonth periodo = desde.plusMonths(i);
            serie.add(ImaTendenciaPuntoDTO.builder()
                    .mes(periodo.toString())
                    .imaEmpresa(imaPorMes.get(periodo))
                    .imaPromedioSector(promedioPorMes.get(periodo))
                    .build());
        }
        return serie;
    }

    private Map<YearMonth, BigDecimal> indexarSnapshots(UUID empresaId, YearMonth desde, YearMonth hasta) {
        List<ImaSnapshot> snapshots = imaSnapshotRepository.findVentana(
                empresaId, desde.getYear(), desde.getMonthValue(), hasta.getYear(), hasta.getMonthValue());

        Map<YearMonth, BigDecimal> porMes = new HashMap<>();
        for (ImaSnapshot snapshot : snapshots) {
            porMes.put(YearMonth.of(snapshot.getAnio(), snapshot.getMes()), snapshot.getIma());
        }
        return porMes;
    }

    /**
     * Toma el promedio sectorial ya persistido en AgregadoSectorial, que es la misma
     * población filtrada (empresas elegibles, sin snapshots parciales) que expone
     * ImaService en /api/ima y /api/ima/benchmark. Se descarta cualquier mes cuyo
     * promedioIma sea null: ese null es exactamente la señal de que el sector no
     * alcanzó el umbral de empresas elegibles, así que /tendencia no dibuja línea
     * sectorial donde /benchmark tampoco la mostraría.
     */
    private Map<YearMonth, BigDecimal> indexarPromediosSectoriales(Empresa empresa,
                                                                   YearMonth desde, YearMonth hasta) {
        List<AgregadoSectorial> agregados = agregadoSectorialRepository.findVentana(
                empresa.getSectorIndustrial(), desde.getYear(), desde.getMonthValue(),
                hasta.getYear(), hasta.getMonthValue());

        Map<YearMonth, BigDecimal> porMes = new HashMap<>();
        for (AgregadoSectorial agregado : agregados) {
            if (agregado.getPromedioIma() == null) {
                continue;
            }
            porMes.put(YearMonth.of(agregado.getAnio(), agregado.getMes()),
                    agregado.getPromedioIma().setScale(1, RoundingMode.HALF_UP));
        }
        return porMes;
    }

}