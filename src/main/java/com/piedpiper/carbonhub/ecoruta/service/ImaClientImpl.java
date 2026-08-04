package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación real del cliente de IMA.
 * Consulta el snapshot más reciente (hasta 3 meses atrás) para cada empresa.
 */
@Component
public class ImaClientImpl implements ImaClient {

    private static final Logger log = LoggerFactory.getLogger(ImaClientImpl.class);
    private static final int MAX_MESES_ATRAS = 3;

    private final ImaSnapshotRepository imaSnapshotRepository;

    public ImaClientImpl(ImaSnapshotRepository imaSnapshotRepository) {
        this.imaSnapshotRepository = imaSnapshotRepository;
    }

    @Override
    public Map<UUID, IMADTO> consultarIma(List<UUID> empresaIds) {
        log.debug("Consultando IMA para {} empresas", empresaIds.size());

        Map<UUID, IMADTO> resultado = new HashMap<>();
        YearMonth actual = YearMonth.now();

        for (UUID empresaId : empresaIds) {
            Optional<ImaSnapshot> snapshot = buscarSnapshotReciente(empresaId, actual);

            snapshot.ifPresent(s -> {
                IMADTO dto = new IMADTO(empresaId, s.getIma(), s.isParcial(), s.getCalculatedAt());
                resultado.put(empresaId, dto);
            });
        }

        log.debug("IMA obtenido para {}/{} empresas", resultado.size(), empresaIds.size());
        return resultado;
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
