package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImaTendenciaService {

    public static final int MESES_VENTANA_MAXIMA = 12;


    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final UsuarioRepository usuarioRepository;

    public ImaTendenciaService(ImaSnapshotRepository imaSnapshotRepository,
                               AgregadoSectorialRepository agregadoSectorialRepository,
                               UsuarioRepository usuarioRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public ImaTendenciaResponseDTO obtenerTendencia(Integer mesesAtras, UUID usuarioId) {
        int ventana = resolverVentana(mesesAtras);
        Empresa empresa = resolverEmpresa(usuarioId);

        YearMonth hasta = YearMonth.from(LocalDate.now());
        YearMonth desde = hasta.minusMonths(ventana - 1L);

        Map<YearMonth, BigDecimal> imaPorMes = indexarSnapshots(empresa.getId(), desde, hasta);
        Map<YearMonth, BigDecimal> promedioPorMes = indexarPromediosSectoriales(empresa, desde, hasta);

        List<ImaTendenciaPuntoDTO> serie = construirSerie(desde, ventana, imaPorMes, promedioPorMes);

        return ImaTendenciaResponseDTO.builder()
                .mesesAtras(ventana)
                .serie(serie)
                .sinDatosSectoriales(promedioPorMes.isEmpty())
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
     * Solo se incluyen los meses en que el sector alcanzó el mínimo de empresas elegibles.
     * La elegibilidad se toma de AgregadoSectorial, que es la misma población que usa
     * ImaService: así /tendencia nunca muestra línea sectorial en un período donde
     * /api/ima informa que el sector no tiene suficientes empresas.
     */
    private Map<YearMonth, BigDecimal> indexarPromediosSectoriales(Empresa empresa,
                                                                   YearMonth desde, YearMonth hasta) {
        Set<YearMonth> periodosHabilitados = periodosConSectorElegible(empresa, desde, hasta);
        if (periodosHabilitados.isEmpty()) {
            return Map.of();
        }

        List<PromedioSectorialMensual> promedios = imaSnapshotRepository.promediarImaPorSector(
                empresa.getSectorIndustrial(), desde.getYear(), desde.getMonthValue(),
                hasta.getYear(), hasta.getMonthValue());

        Map<YearMonth, BigDecimal> porMes = new HashMap<>();
        for (PromedioSectorialMensual promedio : promedios) {
            YearMonth periodo = YearMonth.of(promedio.getAnio(), promedio.getMes());
            if (periodosHabilitados.contains(periodo)) {
                porMes.put(periodo, promedio.getPromedioIma().setScale(1, RoundingMode.HALF_UP));
            }
        }
        return porMes;
    }

    private Set<YearMonth> periodosConSectorElegible(Empresa empresa, YearMonth desde, YearMonth hasta) {
        List<AgregadoSectorial> agregados = agregadoSectorialRepository.findVentana(
                empresa.getSectorIndustrial(), desde.getYear(), desde.getMonthValue(),
                hasta.getYear(), hasta.getMonthValue());

        Set<YearMonth> periodos = new HashSet<>();
        for (AgregadoSectorial agregado : agregados) {
            if (agregado.getCantidadEmpresas() != null
                    && agregado.getCantidadEmpresas() >= ImaCalculos.UMBRAL_EMPRESAS_SECTOR) {
                periodos.add(YearMonth.of(agregado.getAnio(), agregado.getMes()));
            }
        }
        return periodos;
    }

    private Empresa resolverEmpresa(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa();
    }
}
