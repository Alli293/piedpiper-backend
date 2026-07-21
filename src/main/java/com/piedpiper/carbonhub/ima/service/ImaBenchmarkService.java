package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.BenchmarkDimensionDTO;
import com.piedpiper.carbonhub.ima.models.dtos.BenchmarkSectorialResponseDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.enums.PosicionBenchmark;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ImaBenchmarkService {

    private static final BigDecimal UMBRAL_POSICION = BigDecimal.valueOf(2);

    private final ImaService imaService;
    private final AgregadoSectorialRepository agregadoSectorialRepository;
    private final EmisionRepository emisionRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public ImaBenchmarkService(ImaService imaService,
                               AgregadoSectorialRepository agregadoSectorialRepository,
                               EmisionRepository emisionRepository,
                               EmpresaRepository empresaRepository,
                               UsuarioRepository usuarioRepository) {
        this.imaService = imaService;
        this.agregadoSectorialRepository = agregadoSectorialRepository;
        this.emisionRepository = emisionRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public BenchmarkSectorialResponseDTO obtenerBenchmark(int anio, int mes, UUID usuarioId) {
        Empresa empresa = resolverEmpresa(usuarioId);
        ImaResponseDTO propio = imaService.obtenerIma(anio, mes, usuarioId);

        AgregadoSectorial agregado = agregadoSectorialRepository
                .findBySectorAndAnioAndMes(empresa.getSectorIndustrial(), anio, mes)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo componer el benchmark sectorial."));

        if (agregado.getCantidadEmpresas() < ImaCalculos.UMBRAL_EMPRESAS_SECTOR) {
            return BenchmarkSectorialResponseDTO.builder()
                    .benchmarkDisponible(false)
                    .cantidadEmpresas(agregado.getCantidadEmpresas())
                    .imaParcial(propio.isParcial())
                    .build();
        }

        if (agregado.getPromedioIma() == null) {
            completarPromedios(agregado, anio, mes);
        }

        return BenchmarkSectorialResponseDTO.builder()
                .benchmarkDisponible(true)
                .cantidadEmpresas(agregado.getCantidadEmpresas())
                .imaParcial(propio.isParcial())
                .ima(dimension(propio.getIma(), agregado.getPromedioIma()))
                .cobertura(dimension(propio.getCobertura(), agregado.getPromedioCobertura()))
                .puntajeIntensidadSectorial(dimensionPuntaje(propio, agregado))
                .consistencia(dimension(propio.getConsistencia(), agregado.getPromedioConsistencia()))
                .build();
    }

    private void completarPromedios(AgregadoSectorial agregado, int anio, int mes) {
        LocalDate hasta = LocalDate.of(anio, mes, 1).plusMonths(1).minusDays(1);
        LocalDate desde = LocalDate.of(anio, mes, 1).minusMonths(ImaCalculos.MESES_VENTANA - 1);

        List<Empresa> elegibles = empresaRepository.findBySectorIndustrial(agregado.getSector()).stream()
                .filter(e -> e.getCantidadEmpleados() != null && e.getCantidadEmpleados() > 0)
                .toList();

        BigDecimal sumaCobertura = BigDecimal.ZERO;
        BigDecimal sumaConsistencia = BigDecimal.ZERO;
        BigDecimal sumaPuntaje = BigDecimal.ZERO;
        BigDecimal sumaIma = BigDecimal.ZERO;
        int contadas = 0;

        for (Empresa emp : elegibles) {
            BigDecimal carbonKg = emisionRepository.sumarCarbonKgEnVentana(emp.getId(), desde, hasta);
            if (carbonKg.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal cobertura = ImaCalculos.cobertura(
                    emisionRepository.contarCategoriasConRegistro(emp.getId(), desde, hasta));
            BigDecimal consistencia = ImaCalculos.consistencia(
                    emisionRepository.contarMesesConRegistro(emp.getId(), desde, hasta));
            BigDecimal intensidad = ImaCalculos.intensidadToneladasPorEmpleado(carbonKg, emp.getCantidadEmpleados());
            BigDecimal puntaje = ImaCalculos.puntajeIntensidadSectorial(intensidad, agregado.getIntensidadPromedio());
            BigDecimal ima = ImaCalculos.ima(cobertura, puntaje, consistencia);

            sumaCobertura = sumaCobertura.add(cobertura);
            sumaConsistencia = sumaConsistencia.add(consistencia);
            sumaPuntaje = sumaPuntaje.add(puntaje);
            sumaIma = sumaIma.add(ima);
            contadas++;
        }

        if (contadas == 0) {
            return;
        }

        agregado.setPromedioCobertura(promedio(sumaCobertura, contadas));
        agregado.setPromedioConsistencia(promedio(sumaConsistencia, contadas));
        agregado.setPromedioPuntajeIntensidadSectorial(promedio(sumaPuntaje, contadas));
        agregado.setPromedioIma(promedio(sumaIma, contadas));
        agregadoSectorialRepository.save(agregado);
    }

    private BigDecimal promedio(BigDecimal suma, int cantidad) {
        return suma.divide(BigDecimal.valueOf(cantidad), 1, RoundingMode.HALF_UP);
    }

    private BenchmarkDimensionDTO dimensionPuntaje(ImaResponseDTO propio, AgregadoSectorial agregado) {
        if (propio.getPuntajeIntensidadSectorial() == null) {
            return BenchmarkDimensionDTO.builder()
                    .promedioSector(agregado.getPromedioPuntajeIntensidadSectorial())
                    .build();
        }
        return dimension(propio.getPuntajeIntensidadSectorial(), agregado.getPromedioPuntajeIntensidadSectorial());
    }

    private BenchmarkDimensionDTO dimension(BigDecimal valorEmpresa, BigDecimal promedioSector) {
        return BenchmarkDimensionDTO.builder()
                .valorEmpresa(valorEmpresa)
                .promedioSector(promedioSector)
                .posicion(posicion(valorEmpresa, promedioSector))
                .build();
    }

    private PosicionBenchmark posicion(BigDecimal valorEmpresa, BigDecimal promedioSector) {
        BigDecimal diferencia = valorEmpresa.subtract(promedioSector);
        if (diferencia.compareTo(UMBRAL_POSICION) > 0) {
            return PosicionBenchmark.POR_ENCIMA;
        }
        if (diferencia.compareTo(UMBRAL_POSICION.negate()) < 0) {
            return PosicionBenchmark.POR_DEBAJO;
        }
        return PosicionBenchmark.EN_LINEA;
    }

    private Empresa resolverEmpresa(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw ApiException.accesoDenegado("El usuario autenticado no pertenece a una empresa.");
        }
        return empresa;
    }
}
