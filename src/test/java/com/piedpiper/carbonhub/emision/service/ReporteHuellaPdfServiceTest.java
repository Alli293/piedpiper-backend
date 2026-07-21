package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.mappers.EmpresaMapper;
import com.piedpiper.carbonhub.empresa.models.dtos.EmpresaReporteDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteHuellaPdfServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private LimiteEmisionesRepository limiteEmisionesRepository;
    @Mock
    private EmpresaMapper empresaMapper;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ReporteHuellaPdfGenerator pdfGenerator;

    @InjectMocks
    private ReporteHuellaPdfService service;

    @Test
    void generaPdfConResumenAgregado() {
        givenEmpresaAsociada();
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)))
                .thenReturn(List.of(
                        emisionElectricidad("1000.000"),
                        emisionFlota("500.000")
                ));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("5.0000"))));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)))
                .thenReturn(new BigDecimal("1500.000"));
        when(pdfGenerator.generar(org.mockito.ArgumentMatchers.any())).thenReturn("%PDF".getBytes());

        byte[] pdf = service.generar(USUARIO_ID, 2026, null);

        ArgumentCaptor<ReporteHuellaPdfDTO> captor = ArgumentCaptor.forClass(ReporteHuellaPdfDTO.class);
        verify(pdfGenerator).generar(captor.capture());
        assertThat(pdf).isNotEmpty();
        assertThat(captor.getValue().totalKg()).isEqualByComparingTo("1500.000");
        assertThat(captor.getValue().totalT()).isEqualByComparingTo("1.5000");
        assertThat(captor.getValue().empresa()).isEqualTo("CarbonHub Demo");
        assertThat(captor.getValue().comparacion().porcentajeConsumido()).isEqualByComparingTo("30.0");
        assertThat(captor.getValue().sinDatos()).isFalse();
        assertThat(captor.getValue().generadoEn().getZone().getId()).isEqualTo("America/Costa_Rica");
    }

    @Test
    void generaPdfAunqueNoExistanEmisiones() {
        givenEmpresaAsociada();
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1))).thenReturn(List.of());
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.empty());
        when(pdfGenerator.generar(org.mockito.ArgumentMatchers.any())).thenReturn("%PDF".getBytes());

        byte[] pdf = service.generar(USUARIO_ID, 2026, 7);

        ArgumentCaptor<ReporteHuellaPdfDTO> captor = ArgumentCaptor.forClass(ReporteHuellaPdfDTO.class);
        verify(pdfGenerator).generar(captor.capture());
        assertThat(pdf).isNotEmpty();
        assertThat(captor.getValue().totalKg()).isEqualByComparingTo("0");
        assertThat(captor.getValue().sinDatos()).isTrue();
        assertThat(captor.getValue().comparacion().tieneLimite()).isFalse();
    }

    @Test
    void comparaReporteMensualContraAcumuladoAnual() {
        givenEmpresaAsociada();
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1)))
                .thenReturn(List.of(emisionElectricidad("100.000")));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("5.0000"))));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)))
                .thenReturn(new BigDecimal("2500.000"));
        when(pdfGenerator.generar(org.mockito.ArgumentMatchers.any())).thenReturn("%PDF".getBytes());

        service.generar(USUARIO_ID, 2026, 7);

        ArgumentCaptor<ReporteHuellaPdfDTO> captor = ArgumentCaptor.forClass(ReporteHuellaPdfDTO.class);
        verify(pdfGenerator).generar(captor.capture());
        assertThat(captor.getValue().totalKg()).isEqualByComparingTo("100.000");
        assertThat(captor.getValue().totalT()).isEqualByComparingTo("0.1000");
        assertThat(captor.getValue().comparacion().acumuladoT()).isEqualByComparingTo("2.5000");
        assertThat(captor.getValue().comparacion().porcentajeConsumido()).isEqualByComparingTo("50.0");
    }

    @Test
    void fallaSiUsuarioNoTieneEmpresaAsociada() {
        Usuario usuario = Usuario.builder()
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.generar(USUARIO_ID, 2026, null))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private void givenEmpresaAsociada() {
        Empresa empresa = empresa();
        Usuario usuario = Usuario.builder()
                .empresa(empresa)
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(empresaMapper.toReporteDto(empresa))
                .thenReturn(new EmpresaReporteDTO(EMPRESA_ID, "CarbonHub Demo"));
    }

    private Empresa empresa() {
        return Empresa.builder()
                .id(EMPRESA_ID)
                .nombreEmpresa("CarbonHub Demo")
                .build();
    }

    private Emision emisionElectricidad(String carbonKg) {
        return EmisionElectricidad.builder()
                .empresaId(EMPRESA_ID)
                .fechaActividad(LocalDate.of(2026, 7, 1))
                .titulo("Electricidad")
                .carbonKg(new BigDecimal(carbonKg))
                .build();
    }

    private Emision emisionFlota(String carbonKg) {
        return EmisionFlota.builder()
                .empresaId(EMPRESA_ID)
                .fechaActividad(LocalDate.of(2026, 7, 1))
                .titulo("Flota")
                .carbonKg(new BigDecimal(carbonKg))
                .build();
    }
}
