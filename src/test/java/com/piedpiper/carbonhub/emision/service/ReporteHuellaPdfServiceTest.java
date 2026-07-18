package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ReporteHuellaPdfDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
    private UsuarioRepository usuarioRepository;
    @Mock
    private ReporteHuellaPdfGenerator pdfGenerator;

    @InjectMocks
    private ReporteHuellaPdfService service;

    @Test
    void generaPdfConResumenAgregado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByCategoriaAndAnio(EMPRESA_ID, 2026))
                .thenReturn(List.of(
                        new Object[]{"ELECTRICIDAD", new BigDecimal("1000.000")},
                        new Object[]{"FLOTA", new BigDecimal("500.000")}
                ));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("5.0000"))));
        when(pdfGenerator.generar(org.mockito.ArgumentMatchers.any())).thenReturn("%PDF".getBytes());

        byte[] pdf = service.generar(USUARIO_ID, 2026, null);

        ArgumentCaptor<ReporteHuellaPdfDTO> captor = ArgumentCaptor.forClass(ReporteHuellaPdfDTO.class);
        verify(pdfGenerator).generar(captor.capture());
        assertThat(pdf).isNotEmpty();
        assertThat(captor.getValue().totalKg()).isEqualByComparingTo("1500.000");
        assertThat(captor.getValue().totalT()).isEqualByComparingTo("1.5000");
        assertThat(captor.getValue().comparacion().porcentajeConsumido()).isEqualByComparingTo("30.0");
        assertThat(captor.getValue().sinDatos()).isFalse();
    }

    @Test
    void generaPdfAunqueNoExistanEmisiones() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByCategoriaAndMes(EMPRESA_ID, 2026, 7)).thenReturn(List.of());
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

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("CarbonHub Demo").build())
                .build();
    }
}
