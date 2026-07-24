package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualResponseDTO.PuntoMensual;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionEvolucionServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;

    @InjectMocks
    private EmisionEvolucionService service;

    @Test
    void serieCompletaConDatosEnVariosMeses() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2026)).thenReturn(List.of(
                new Object[]{1, new BigDecimal("150.500")},
                new Object[]{3, new BigDecimal("200.000")},
                new Object[]{7, new BigDecimal("80.250")}
        ));

        EvolucionMensualResponseDTO resultado = service.obtenerEvolucion(2026, USUARIO_ID);

        assertThat(resultado.getAnio()).isEqualTo(2026);
        assertThat(resultado.getSerie()).hasSize(12);

        // Meses con datos
        assertThat(resultado.getSerie().get(0).getTotalCarbonKg())
                .isEqualByComparingTo(new BigDecimal("150.500"));
        assertThat(resultado.getSerie().get(2).getTotalCarbonKg())
                .isEqualByComparingTo(new BigDecimal("200.000"));
        assertThat(resultado.getSerie().get(6).getTotalCarbonKg())
                .isEqualByComparingTo(new BigDecimal("80.250"));

        // Meses sin datos
        assertThat(resultado.getSerie().get(1).getTotalCarbonKg())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getSerie().get(11).getTotalCarbonKg())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void anioSinDatosDevuelveDocePuntosEnCero() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2020)).thenReturn(List.of());

        EvolucionMensualResponseDTO resultado = service.obtenerEvolucion(2020, USUARIO_ID);

        assertThat(resultado.getAnio()).isEqualTo(2020);
        assertThat(resultado.getSerie()).hasSize(12);
        assertThat(resultado.getSerie())
                .allMatch(p -> p.getTotalCarbonKg().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void serieTieneDocePuntosConMesesOrdenados() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2025)).thenReturn(List.of());

        EvolucionMensualResponseDTO resultado = service.obtenerEvolucion(2025, USUARIO_ID);

        List<PuntoMensual> serie = resultado.getSerie();
        for (int i = 0; i < 12; i++) {
            assertThat(serie.get(i).getMes()).isEqualTo(i + 1);
        }
    }

    @Test
    void usuarioSinEmpresaLanzaExcepcion() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenThrow(ApiException.empresaNoConfigurada());

        assertThatThrownBy(() -> service.obtenerEvolucion(2026, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioNoExistenteLanzaExcepcion() {
        when(emisionEmpresaService.empresaId(USUARIO_ID))
                .thenThrow(ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        assertThatThrownBy(() -> service.obtenerEvolucion(2026, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void anioInvalidoDevuelveError() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);

        assertThatThrownBy(() -> service.obtenerEvolucion(1899, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
