package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCodigoVerificacionService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class InsigniaEmpresaEvaluacionService {

    private static final Logger log =
            LoggerFactory.getLogger(InsigniaEmpresaEvaluacionService.class);

    private final CatalogoInsigniaRepository catalogoInsigniaRepository;
    private final InsigniaEmpresaRepository insigniaEmpresaRepository;
    private final CertificacionRepository certificacionRepository;
    private final EmpresaRepository empresaRepository;
    private final InsigniaEmpresaRegistroService insigniaEmpresaRegistroService;
    private final GeneradorCodigoVerificacionService generadorCodigoVerificacionService;

    public InsigniaEmpresaEvaluacionService(CatalogoInsigniaRepository catalogoInsigniaRepository,
                                            InsigniaEmpresaRepository insigniaEmpresaRepository,
                                            CertificacionRepository certificacionRepository,
                                            EmpresaRepository empresaRepository,
                                            InsigniaEmpresaRegistroService
                                                    insigniaEmpresaRegistroService,
                                            GeneradorCodigoVerificacionService
                                                    generadorCodigoVerificacionService) {
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
        this.insigniaEmpresaRepository = insigniaEmpresaRepository;
        this.certificacionRepository = certificacionRepository;
        this.empresaRepository = empresaRepository;
        this.insigniaEmpresaRegistroService = insigniaEmpresaRegistroService;
        this.generadorCodigoVerificacionService = generadorCodigoVerificacionService;
    }

    public void evaluarPorNuevaCertificacion(UUID empresaId) {
        if (empresaId == null) {
            log.warn("Evaluacion de insignias empresariales omitida por empresa nula");
            return;
        }

        List<CatalogoInsignia> catalogo;
        try {
            catalogo = catalogoInsigniaRepository.findByActivaTrue()
                    .stream()
                    .sorted(Comparator.comparing(CatalogoInsignia::getIdInsignia)
                            .thenComparing(this::ordenNivel))
                    .toList();
        } catch (Exception e) {
            log.error("Error al consultar el catalogo de insignias para la empresa {}", empresaId, e);
            return;
        }

        for (CatalogoInsignia insignia : catalogo) {
            try {
                evaluarYOtorgar(empresaId, insignia);
            } catch (Exception e) {
                log.error("Error al evaluar la insignia {} nivel {} para la empresa {}",
                        insignia.getIdInsignia(), insignia.getNivelInsignia(), empresaId, e);
            }
        }
    }

    boolean cumpleRequisitos(UUID empresaId, CatalogoInsignia insignia) {
        NivelInsignia nivel = NivelInsignia.desde(insignia.getNivelInsignia())
                .orElse(null);
        if (nivel == null) {
            return false;
        }
        if (!cumpleProgresion(empresaId, insignia.getIdInsignia(), nivel)) {
            return false;
        }
        if (yaObtenida(empresaId, insignia.getIdInsignia(), nivel.getCodigo())) {
            return false;
        }

        long certificacionesActivas =
                certificacionRepository.countByEmpresaIdAndEstado(
                        empresaId, EstadoCertificacion.ACTIVA);
        if (certificacionesActivas < insignia.getCantidadMinimaCertificacionesActivas()) {
            return false;
        }

        if (insignia.getTiposCertificacionesRequeridas().isEmpty()) {
            return true;
        }

        long tiposPresentes = certificacionRepository.countDistinctTiposActivos(
                empresaId,
                EstadoCertificacion.ACTIVA,
                insignia.getTiposCertificacionesRequeridas());
        return tiposPresentes == insignia.getTiposCertificacionesRequeridas().size();
    }

    private void evaluarYOtorgar(UUID empresaId, CatalogoInsignia insignia) {
        NivelInsignia nivel = NivelInsignia.desde(insignia.getNivelInsignia())
                .orElse(null);
        if (nivel == null) {
            log.warn("Nivel de insignia empresarial invalido en catalogo: {}",
                    insignia.getNivelInsignia());
            return;
        }
        if (!cumpleRequisitos(empresaId, insignia)) {
            return;
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            log.warn("Otorgamiento de insignia omitido: la empresa {} no existe", empresaId);
            return;
        }

        insigniaEmpresaRegistroService.registrar(InsigniaEmpresa.builder()
                .empresa(empresa)
                .idInsignia(insignia.getIdInsignia())
                .nivelInsignia(nivel.getCodigo())
                .fechaObtencion(Instant.now())
                .codigoVerificacion(generadorCodigoVerificacionService.generar(
                        insigniaEmpresaRepository::existsByCodigoVerificacion))
                .build());
        log.info("Insignia empresarial {} nivel {} otorgada a la empresa {}.",
                insignia.getIdInsignia(), nivel.getCodigo(), empresaId);
    }

    private boolean cumpleProgresion(UUID empresaId, Long idInsignia, NivelInsignia nivel) {
        return nivel.anterior()
                .map(anterior -> yaObtenida(empresaId, idInsignia, anterior.getCodigo()))
                .orElse(true);
    }

    private boolean yaObtenida(UUID empresaId, Long idInsignia, String nivelInsignia) {
        return insigniaEmpresaRepository.existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
                empresaId, idInsignia, nivelInsignia);
    }

    private int ordenNivel(CatalogoInsignia insignia) {
        return NivelInsignia.desde(insignia.getNivelInsignia())
                .map(NivelInsignia::getOrden)
                .orElse(Integer.MAX_VALUE);
    }
}
