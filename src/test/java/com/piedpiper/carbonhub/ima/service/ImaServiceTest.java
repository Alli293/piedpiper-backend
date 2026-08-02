package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.mappers.ImaSnapshotMapperImpl;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;
    @Mock
    private AgregadoSectorialRepository agregadoSectorialRepository;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ImaInterpretacionService interpretacionService;

    private ImaService service() {
        return new ImaService(imaSnapshotRepository, agregadoSectorialRepository, emisionRepository,
                empresaRepository, usuarioRepository, new ImaSnapshotMapperImpl());
    }

    @Test
    void snapshotExistenteSeDevuelveSinRecalcular() {
        ImaSnapshot existente = ImaSnapshot.builder()
                .empresaId(EMPRESA_ID)
                .anio(2026)
                .mes(6)
                .cobertura(new BigDecimal("75.0"))
                .consistencia(new BigDecimal("50.0"))
                .puntajeIntensidadSectorial(new BigDecimal("60.0"))
                .ima(new BigDecimal("61.7"))
                .parcial(false)
                .calculatedAt(Instant.now())
                .build();

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(EMPRESA_ID, 2026, 6))
                .thenReturn(Optional.of(existente));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.getCobertura()).isEqualByComparingTo(new BigDecimal("75.0"));
        assertThat(resultado.getIma()).isEqualByComparingTo(new BigDecimal("61.7"));
        verify(emisionRepository, never()).contarCategoriasConRegistro(any(), any(), any());
    }

    @Test
    void coberturaCalculaCategoriasEntreCuatro() {
        configurarEmpresaConEmpleados(50);
        configurarAgregadoSectorialConUmbral();

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(12L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("5000"));
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.getCobertura()).isEqualByComparingTo(new BigDecimal("75.0"));
    }

    @Test
    void consistenciaCalculaMesesEntreDoce() {
        configurarEmpresaConEmpleados(50);
        configurarAgregadoSectorialConUmbral();

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(8L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("10000"));
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.getConsistencia()).isEqualByComparingTo(new BigDecimal("66.7"));
    }

    @Test
    void empresaSinEmpleadosGeneraImaParcial() {
        configurarEmpresaSinEmpleados();

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(2L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(6L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("3000"));
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.of(AgregadoSectorial.builder()
                        .cantidadEmpresas(2)
                        .calculatedAt(Instant.now())
                        .build()));
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.isParcial()).isTrue();
        assertThat(resultado.getPuntajeIntensidadSectorial()).isNull();
        assertThat(resultado.getMotivoParcial()).contains("empleados");
        assertThat(resultado.getIma()).isEqualByComparingTo(new BigDecimal("50.0"));
    }

    @Test
    void sectorConMenosDeCincoEmpresasGeneraImaParcial() {
        configurarEmpresaConEmpleados(50);

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(12L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("5000"));
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.of(AgregadoSectorial.builder()
                        .cantidadEmpresas(3)
                        .calculatedAt(Instant.now())
                        .build()));
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.isParcial()).isTrue();
        assertThat(resultado.getPuntajeIntensidadSectorial()).isNull();
        assertThat(resultado.getMotivoParcial()).contains("sector");
    }

    @Test
    void intensidadCeroFijaPuntajeEn100() {
        configurarEmpresaConEmpleados(50);
        configurarAgregadoSectorialConUmbral();

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(12L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.getPuntajeIntensidadSectorial())
                .isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void empresaSinEmisionesGeneraImaParcialConMensaje() {
        configurarEmpresaConEmpleados(50);

        when(emisionRepository.contarCategoriasConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(emisionRepository.contarMesesConRegistro(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(emisionRepository.sumarCarbonKgEnVentana(eq(EMPRESA_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.of(AgregadoSectorial.builder()
                        .cantidadEmpresas(3)
                        .calculatedAt(Instant.now())
                        .build()));
        when(imaSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ImaResponseDTO resultado = service().obtenerIma(2026, 6, USUARIO_ID);

        assertThat(resultado.isParcial()).isTrue();
        assertThat(resultado.getMotivoParcial()).contains("Aún no hay emisiones");
        assertThat(resultado.getCobertura()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getConsistencia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void usuarioSinEmpresaLanzaExcepcion() {
        Usuario sinEmpresa = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(sinEmpresa));

        var servicio = service();
        assertThatThrownBy(() -> servicio.obtenerIma(2026, 6, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --- helpers ---

    private void configurarEmpresaConEmpleados(int cantidad) {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(EMPRESA_ID, 2026, 6))
                .thenReturn(Optional.empty());
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(
                Empresa.builder()
                        .id(EMPRESA_ID)
                        .sectorIndustrial(SectorIndustrial.SERVICIOS)
                        .cantidadEmpleados(cantidad)
                        .build()));
    }

    private void configurarEmpresaSinEmpleados() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(imaSnapshotRepository.findByEmpresaIdAndAnioAndMes(EMPRESA_ID, 2026, 6))
                .thenReturn(Optional.empty());
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(
                Empresa.builder()
                        .id(EMPRESA_ID)
                        .sectorIndustrial(SectorIndustrial.SERVICIOS)
                        .cantidadEmpleados(null)
                        .build()));
    }

    private void configurarAgregadoSectorialConUmbral() {
        when(agregadoSectorialRepository.findBySectorAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.of(AgregadoSectorial.builder()
                        .cantidadEmpresas(10)
                        .intensidadPromedio(new BigDecimal("2.0"))
                        .calculatedAt(Instant.now())
                        .build()));
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }
}
