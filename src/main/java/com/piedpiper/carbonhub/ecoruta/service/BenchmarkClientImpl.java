package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PosicionBenchmark;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación real del cliente de benchmarking.
 * Compara el IMA de cada empresa contra el promedio de su sector industrial.
 */
@Component
public class BenchmarkClientImpl implements BenchmarkClient {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkClientImpl.class);
    private static final int MAX_MESES_ATRAS = 3;

    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final EmpresaRepository empresaRepository;

    public BenchmarkClientImpl(ImaSnapshotRepository imaSnapshotRepository,
                               AgregadoSectorialRepository agregadoSectorialRepository,
                               EmpresaRepository empresaRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public Map<UUID, BenchmarkDTO> consultarBenchmark(List<UUID> empresaIds) {
        log.debug("Consultando benchmark para {} empresas", empresaIds.size());

        Map<UUID, BenchmarkDTO> resultado = new HashMap<>();
        YearMonth actual = YearMonth.now();

        for (UUID empresaId : empresaIds) {
            Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
            if (empresaOpt.isEmpty()) {
                continue;
            }

            Empresa empresa = empresaOpt.get();
            Optional<ImaSnapshot> snapshotOpt = buscarSnapshotReciente(empresaId, actual);
            if (snapshotOpt.isEmpty()) {
                continue;
            }

            ImaSnapshot snapshot = snapshotOpt.get();
            Optional<AgregadoSectorial> agregadoOpt = agregadoSectorialRepository
                    .findBySectorAndAnioAndMes(empresa.getSectorIndustrial(), snapshot.getAnio(), snapshot.getMes());

            if (agregadoOpt.isEmpty()) {
                continue;
            }

            AgregadoSectorial agregado = agregadoOpt.get();
            BigDecimal valorEmpresa = snapshot.getIma();
            BigDecimal promedioSector = agregado.getPromedioIma();

            PosicionBenchmark posicion = calcularPosicion(valorEmpresa, promedioSector);

            BenchmarkDTO dto = new BenchmarkDTO(
                    empresaId,
                    posicion,
                    valorEmpresa,
                    promedioSector,
                    Instant.now()
            );
            resultado.put(empresaId, dto);
        }

        log.debug("Benchmark obtenido para {}/{} empresas", resultado.size(), empresaIds.size());
        return resultado;
    }

    private PosicionBenchmark calcularPosicion(BigDecimal valorEmpresa, BigDecimal promedioSector) {
        if (promedioSector == null || promedioSector.compareTo(BigDecimal.ZERO) == 0) {
            return PosicionBenchmark.PROMEDIO;
        }

        BigDecimal umbralLider = promedioSector.multiply(new BigDecimal("1.3"));
        BigDecimal umbralDebajo = promedioSector.multiply(new BigDecimal("0.7"));

        if (valorEmpresa.compareTo(umbralLider) >= 0) {
            return PosicionBenchmark.LIDER;
        } else if (valorEmpresa.compareTo(promedioSector) >= 0) {
            return PosicionBenchmark.ARRIBA_PROMEDIO;
        } else if (valorEmpresa.compareTo(umbralDebajo) >= 0) {
            return PosicionBenchmark.PROMEDIO;
        } else {
            return PosicionBenchmark.DEBAJO_PROMEDIO;
        }
    }

    private Optional<ImaSnapshot> buscarSnapshotReciente(UUID empresaId, YearMonth desde) {
        for (int i = 0; i <= MAX_MESES_ATRAS; i++) {
            YearMonth periodo = desde.minusMonths(i);
            Optional<ImaSnapshot> snapshot = imaSnapshotRepository
                    .findByEmpresaIdAndAnioAndMes(empresaId, periodo.getYear(), periodo.getMonthValue());
            if (snapshot.isPresent()) {
                return snapshot;
            }
        }
        return Optional.empty();
    }
}
