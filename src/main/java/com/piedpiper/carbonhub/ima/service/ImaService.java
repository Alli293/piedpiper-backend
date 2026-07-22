package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ImaService {

    private static final int TOTAL_CATEGORIAS = 4;
    private static final int MESES_VENTANA = 12;
    private static final int UMBRAL_EMPRESAS_SECTOR = 5;

    private final ImaSnapshotRepository imaSnapshotRepository;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final EmisionRepository emisionRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImaInterpretacionService interpretacionService;
    private final com.piedpiper.carbonhub.ima.mappers.ImaSnapshotMapper imaSnapshotMapper;

    public ImaService(ImaSnapshotRepository imaSnapshotRepository,
                      AgregadoSectorialRepository agregadoSectorialRepository,
                      EmisionRepository emisionRepository,
                      EmpresaRepository empresaRepository,
                      UsuarioRepository usuarioRepository,
                      ImaInterpretacionService interpretacionService,
                      com.piedpiper.carbonhub.ima.mappers.ImaSnapshotMapper imaSnapshotMapper) {
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.emisionRepository = emisionRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.interpretacionService = interpretacionService;
        this.imaSnapshotMapper = imaSnapshotMapper;
    }

    @Transactional
    public ImaResponseDTO obtenerIma(Integer anio, Integer mes, UUID usuarioId) {
        UUID empresaId = resolverEmpresaId(usuarioId);

        // Buscar snapshot en caché
        return imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(empresaId, anio, mes)
                .map(this::toDto)
                .orElseGet(() -> calcularYPersistir(empresaId, anio, mes));
    }

    private ImaResponseDTO calcularYPersistir(UUID empresaId, int anio, int mes) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> ApiException.errorInterno("Empresa no encontrada."));

        // Ventana: últimos 12 meses terminando en el mes del período (inclusive)
        LocalDate hasta = LocalDate.of(anio, mes, 1).plusMonths(1).minusDays(1);
        LocalDate desde = LocalDate.of(anio, mes, 1).minusMonths(MESES_VENTANA - 1);

        // Cobertura
        long categoriasPresentes = emisionRepository.contarCategoriasConRegistro(empresaId, desde, hasta);
        BigDecimal cobertura = BigDecimal.valueOf(categoriasPresentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(TOTAL_CATEGORIAS), 1, RoundingMode.HALF_UP);

        // Consistencia
        long mesesConDatos = emisionRepository.contarMesesConRegistro(empresaId, desde, hasta);
        BigDecimal consistencia = BigDecimal.valueOf(mesesConDatos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(MESES_VENTANA), 1, RoundingMode.HALF_UP);

        // Intensidad
        BigDecimal totalCarbonKg = emisionRepository.sumarCarbonKgEnVentana(empresaId, desde, hasta);
        BigDecimal intensidadToneladas = null;
        Integer cantidadEmpleados = empresa.getCantidadEmpleados();

        if (cantidadEmpleados != null && cantidadEmpleados > 0) {
            // t CO₂e por empleado
            intensidadToneladas = totalCarbonKg
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(cantidadEmpleados), 6, RoundingMode.HALF_UP);
        }

        // Puntaje de intensidad sectorial
        BigDecimal puntajeIntensidad = null;
        boolean parcial = false;
        String motivoParcial = null;

        SectorIndustrial sector = empresa.getSectorIndustrial();
        AgregadoSectorial agregado = calcularAgregadoSectorial(sector, anio, mes, desde, hasta);

        if (cantidadEmpleados == null || cantidadEmpleados <= 0) {
            parcial = true;
            motivoParcial = "Completa el número de empleados de tu empresa para calcular tu Puntaje de intensidad sectorial.";
        } else if (agregado.getCantidadEmpresas() < UMBRAL_EMPRESAS_SECTOR) {
            parcial = true;
            motivoParcial = "Tu sector aún no tiene suficientes empresas (mínimo 5) para calcular el Puntaje de intensidad sectorial ni el benchmark.";
        } else {
            // Calcular puntaje: min(100, max(0, 50 × intensidadPromedio / intensidad))
            if (intensidadToneladas.compareTo(BigDecimal.ZERO) == 0) {
                puntajeIntensidad = BigDecimal.valueOf(100);
            } else {
                BigDecimal intensidadPromedio = agregado.getIntensidadPromedio();
                puntajeIntensidad = BigDecimal.valueOf(50)
                        .multiply(intensidadPromedio)
                        .divide(intensidadToneladas, 1, RoundingMode.HALF_UP);
                puntajeIntensidad = puntajeIntensidad.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
            }
        }

        // Verificar si hay emisiones
        if (categoriasPresentes == 0 && mesesConDatos == 0) {
            parcial = true;
            motivoParcial = "Aún no hay emisiones registradas para calcular tu IMA completo.";
        }

        // IMA
        BigDecimal ima;
        if (puntajeIntensidad != null) {
            ima = cobertura.add(puntajeIntensidad).add(consistencia)
                    .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
        } else {
            ima = cobertura.add(consistencia)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
        }

        Instant now = Instant.now();

        ImaSnapshot snapshot = ImaSnapshot.builder()
                .empresaId(empresaId)
                .anio(anio)
                .mes(mes)
                .cobertura(cobertura)
                .puntajeIntensidadSectorial(puntajeIntensidad)
                .consistencia(consistencia)
                .ima(ima)
                .parcial(parcial)
                .motivoParcial(motivoParcial)
                .intensidad(intensidadToneladas)
                .calculatedAt(now)
                .build();

        snapshot = imaSnapshotRepository.save(snapshot);

        // Generar interpretación por IA después del commit
        final UUID snapshotId = snapshot.getId();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            interpretacionService.generarInterpretacion(snapshotId);
                        }
                    });
        }

        return toDto(snapshot);
    }

    private AgregadoSectorial calcularAgregadoSectorial(SectorIndustrial sector, int anio, int mes,
                                                         LocalDate desde, LocalDate hasta) {
        return agregadoSectorialRepository.findBySectorAndAnioAndMes(sector, anio, mes)
                .orElseGet(() -> generarAgregadoSectorial(sector, anio, mes, desde, hasta));
    }

    private AgregadoSectorial generarAgregadoSectorial(SectorIndustrial sector, int anio, int mes,
                                                        LocalDate desde, LocalDate hasta) {
        // Pre-filtrar empresas del sector con empleados, luego una query por empresa elegible
        List<Empresa> empresasSector = empresaRepository.findAll().stream()
                .filter(e -> e.getSectorIndustrial() == sector)
                .filter(e -> e.getCantidadEmpleados() != null && e.getCantidadEmpleados() > 0)
                .toList();

        // Calcular intensidad por empresa (solo las que tienen emisiones en la ventana)
        List<BigDecimal> intensidades = new java.util.ArrayList<>();
        for (Empresa emp : empresasSector) {
            BigDecimal carbonKg = emisionRepository.sumarCarbonKgEnVentana(emp.getId(), desde, hasta);
            if (carbonKg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal intensidad = carbonKg
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(emp.getCantidadEmpleados()), 6, RoundingMode.HALF_UP);
                intensidades.add(intensidad);
            }
        }

        int cantidadEmpresas = intensidades.size();

        AgregadoSectorial.AgregadoSectorialBuilder builder = AgregadoSectorial.builder()
                .sector(sector)
                .anio(anio)
                .mes(mes)
                .cantidadEmpresas(cantidadEmpresas)
                .calculatedAt(Instant.now());

        if (cantidadEmpresas >= UMBRAL_EMPRESAS_SECTOR) {
            BigDecimal sumaIntensidad = intensidades.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal intensidadPromedio = sumaIntensidad
                    .divide(BigDecimal.valueOf(cantidadEmpresas), 6, RoundingMode.HALF_UP);
            builder.intensidadPromedio(intensidadPromedio);
        }

        AgregadoSectorial agregado = builder.build();
        return agregadoSectorialRepository.save(agregado);
    }

    private UUID resolverEmpresaId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return usuario.getEmpresa().getId();
    }

    private ImaResponseDTO toDto(ImaSnapshot snapshot) {
        return imaSnapshotMapper.toDto(snapshot);
    }
}
