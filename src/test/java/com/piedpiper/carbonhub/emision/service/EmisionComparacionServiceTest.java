package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionComparacionServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private LimiteEmisionesRepository limiteEmisionesRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;

    @InjectMocks
    private EmisionComparacionService service;

    @Test
    void calculaPorcentajeCorrectoCuandoExisteLimite() {
        givenEmpresa();
        givenHuellaAnual("30000.000", 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getHuellaAcumuladaT()).isEqualByComparingTo("30.0000");
        assertThat(response.getLimiteT()).isEqualByComparingTo("50.0000");
        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("60.0");
        assertThat(response.getEstado()).isEqualTo("dentro");
    }

    @Test
    void retornaSinLimiteCuandoNoHayLimiteDeclarado() {
        givenEmpresa();
        givenHuellaAnual("60000.000", 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.empty());

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getHuellaAcumuladaT()).isEqualByComparingTo("60.0000");
        assertThat(response.getLimiteT()).isNull();
        assertThat(response.getPorcentajeConsumido()).isNull();
        assertThat(response.getEstado()).isEqualTo("sin_limite");
    }

    @Test
    void huellaCeroQuedaDentroDelLimite() {
        givenEmpresa();
        givenHuellaAnual(BigDecimal.ZERO, 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("0.0");
        assertThat(response.getEstado()).isEqualTo("dentro");
    }

    @Test
    void porcentajeMayorACienQuedaSuperado() {
        givenEmpresa();
        givenHuellaAnual("60000.000", 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("120.0");
        assertThat(response.getEstado()).isEqualTo("superado");
    }

    @Test
    void porcentajeOchentaQuedaCerca() {
        givenEmpresa();
        givenHuellaAnual("40000.000", 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("80.0");
        assertThat(response.getEstado()).isEqualTo("cerca");
    }

    @Test
    void porcentajeCienExactoQuedaAlcanzado() {
        givenEmpresa();
        givenHuellaAnual("50000.000", 2026);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("100.0");
        assertThat(response.getEstado()).isEqualTo("alcanzado");
    }

    @Test
    void usaAnioActualCuandoAnioEsNull() {
        int anioActual = Year.now().getValue();
        givenEmpresa();
        givenHuellaAnual(BigDecimal.ZERO, anioActual);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, anioActual))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, anioActual, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, null);

        assertThat(response.getAnio()).isEqualTo(anioActual);
    }

    @Test
    void anioInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.comparar(USUARIO_ID, 1899))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage()).isEqualTo("Año inválido.");
                });
    }

    @Test
    void usuarioSinEmpresaDevuelve422() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenThrow(ApiException.empresaNoConfigurada());

        assertThatThrownBy(() -> service.comparar(USUARIO_ID, 2026))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private void givenEmpresa() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
    }

    private void givenHuellaAnual(String carbonKg, int anio) {
        givenHuellaAnual(new BigDecimal(carbonKg), anio);
    }

    private void givenHuellaAnual(BigDecimal carbonKg, int anio) {
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(
                EMPRESA_ID, LocalDate.of(anio, 1, 1), LocalDate.of(anio + 1, 1, 1)))
                .thenReturn(carbonKg);
    }
}
