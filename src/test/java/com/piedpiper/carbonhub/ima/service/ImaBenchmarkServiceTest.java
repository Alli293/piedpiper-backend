package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.dtos.BenchmarkSectorialResponseDTO;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.enums.PosicionBenchmark;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaBenchmarkServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private ImaService imaService;
    @Mock
    private AgregadoSectorialRepository agregadoSectorialRepository;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ImaBenchmarkService service;

    @Test
    void imaSobreElPromedioPorMasDeDosPuntosQuedaPorEncima() {
        prepararEscenario(propio("70.0"), agregadoConPromedios("60.0"));

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.isBenchmarkDisponible()).isTrue();
        assertThat(resultado.getIma().getPosicion()).isEqualTo(PosicionBenchmark.POR_ENCIMA);
        assertThat(resultado.getIma().getValorEmpresa()).isEqualByComparingTo(new BigDecimal("70.0"));
        assertThat(resultado.getIma().getPromedioSector()).isEqualByComparingTo(new BigDecimal("60.0"));
    }

    @Test
    void imaDentroDelRangoDeDosPuntosQuedaEnLinea() {
        prepararEscenario(propio("59.0"), agregadoConPromedios("60.0"));

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.getIma().getPosicion()).isEqualTo(PosicionBenchmark.EN_LINEA);
    }

    @Test
    void diferenciaExactaDeDosPuntosQuedaEnLinea() {
        prepararEscenario(propio("62.0"), agregadoConPromedios("60.0"));

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.getIma().getPosicion()).isEqualTo(PosicionBenchmark.EN_LINEA);
    }

    @Test
    void imaBajoElPromedioPorMasDeDosPuntosQuedaPorDebajo() {
        prepararEscenario(propio("57.5"), agregadoConPromedios("60.0"));

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.getIma().getPosicion()).isEqualTo(PosicionBenchmark.POR_DEBAJO);
    }

    @Test
    void sectorConMenosDeCincoEmpresasNoExponePromedios() {
        AgregadoSectorial agregado = agregadoBase();
        agregado.setCantidadEmpresas(4);
        prepararEscenario(propio("70.0"), agregado);

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.isBenchmarkDisponible()).isFalse();
        assertThat(resultado.getCantidadEmpresas()).isEqualTo(4);
        assertThat(resultado.getIma()).isNull();
        assertThat(resultado.getCobertura()).isNull();
        assertThat(resultado.getPuntajeIntensidadSectorial()).isNull();
        assertThat(resultado.getConsistencia()).isNull();
        verify(agregadoSectorialRepository, never()).save(any());
    }

    @Test
    void imaParcialDejaElPuntajeSinValorNiPosicion() {
        ImaResponseDTO parcial = ImaResponseDTO.builder()
                .ima(new BigDecimal("55.0"))
                .cobertura(new BigDecimal("50.0"))
                .consistencia(new BigDecimal("60.0"))
                .puntajeIntensidadSectorial(null)
                .parcial(true)
                .build();
        prepararEscenario(parcial, agregadoConPromedios("60.0"));

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        assertThat(resultado.isImaParcial()).isTrue();
        assertThat(resultado.getPuntajeIntensidadSectorial().getValorEmpresa()).isNull();
        assertThat(resultado.getPuntajeIntensidadSectorial().getPosicion()).isNull();
        assertThat(resultado.getPuntajeIntensidadSectorial().getPromedioSector())
                .isEqualByComparingTo(new BigDecimal("60.0"));
        assertThat(resultado.getCobertura().getPosicion()).isNotNull();
        assertThat(resultado.getConsistencia().getPosicion()).isNotNull();
    }

    @Test
    void agregadoSinPromediosLosCompletaYPersiste() {
        AgregadoSectorial agregado = agregadoBase();
        agregado.setPromedioIma(null);
        prepararEscenario(propio("70.0"), agregado);

        Empresa otra = Empresa.builder()
                .id(UUID.randomUUID())
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .cantidadEmpleados(10)
                .build();
        when(empresaRepository.findBySectorIndustrial(SectorIndustrial.SERVICIOS))
                .thenReturn(List.of(otra));
        when(emisionRepository.sumarCarbonKgEnVentana(any(UUID.class), any(), any()))
                .thenReturn(new BigDecimal("10000"));
        when(emisionRepository.contarCategoriasConRegistro(any(UUID.class), any(), any())).thenReturn(4L);
        when(emisionRepository.contarMesesConRegistro(any(UUID.class), any(), any())).thenReturn(12L);

        BenchmarkSectorialResponseDTO resultado = service.obtenerBenchmark(2026, 6, USUARIO_ID);

        verify(agregadoSectorialRepository).save(agregado);
        assertThat(agregado.getPromedioCobertura()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(agregado.getPromedioConsistencia()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(agregado.getPromedioPuntajeIntensidadSectorial())
                .isEqualByComparingTo(new BigDecimal("50.0"));
        assertThat(agregado.getPromedioIma()).isEqualByComparingTo(new BigDecimal("83.3"));
        assertThat(resultado.isBenchmarkDisponible()).isTrue();
    }

    private void prepararEscenario(ImaResponseDTO propio, AgregadoSectorial agregado) {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(imaService.obtenerIma(2026, 6, USUARIO_ID)).thenReturn(propio);
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(SectorIndustrial.SERVICIOS, 2026, 6))
                .thenReturn(Optional.of(agregado));
    }

    private ImaResponseDTO propio(String ima) {
        return ImaResponseDTO.builder()
                .ima(new BigDecimal(ima))
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("50.0"))
                .puntajeIntensidadSectorial(new BigDecimal("61.0"))
                .parcial(false)
                .build();
    }

    private AgregadoSectorial agregadoBase() {
        return AgregadoSectorial.builder()
                .sector(SectorIndustrial.SERVICIOS)
                .anio(2026)
                .mes(6)
                .cantidadEmpresas(6)
                .intensidadPromedio(new BigDecimal("1.000000"))
                .calculatedAt(Instant.now())
                .build();
    }

    private AgregadoSectorial agregadoConPromedios(String promedioIma) {
        AgregadoSectorial agregado = agregadoBase();
        agregado.setPromedioIma(new BigDecimal(promedioIma));
        agregado.setPromedioCobertura(new BigDecimal("70.0"));
        agregado.setPromedioPuntajeIntensidadSectorial(new BigDecimal("60.0"));
        agregado.setPromedioConsistencia(new BigDecimal("55.0"));
        return agregado;
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder()
                        .id(EMPRESA_ID)
                        .sectorIndustrial(SectorIndustrial.SERVICIOS)
                        .build())
                .build();
    }
}
