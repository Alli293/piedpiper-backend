package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResumenResponseDTO.ResumenCategoriaDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionResumenServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EmisionResumenService service;

    @Test
    void agrupaPorCategoriaYSumaCarbonKg() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdAndFechaActividadBetween(
                EMPRESA_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(
                        electricidad("250.000"),
                        electricidad("250.000"),
                        flota("300.000"),
                        envio("200.000")));

        EmisionResumenResponseDTO resumen = service.resumen(2026, null, USUARIO_ID);

        assertThat(resumen.getTotalKg()).isEqualByComparingTo("1000.000");
        assertThat(resumen.getTotalT()).isEqualByComparingTo("1.000");
        assertThat(resumen.getCategorias())
                .extracting(ResumenCategoriaDTO::getCategoria)
                .containsExactly(CategoriaEmision.ELECTRICIDAD, CategoriaEmision.FLOTA,
                        CategoriaEmision.VUELO, CategoriaEmision.ENVIO);
        assertThat(categoria(resumen, CategoriaEmision.ELECTRICIDAD).getTotalKg())
                .isEqualByComparingTo("500.000");
        assertThat(categoria(resumen, CategoriaEmision.ELECTRICIDAD).getPorcentaje())
                .isEqualByComparingTo("50.0");
        assertThat(categoria(resumen, CategoriaEmision.FLOTA).getPorcentaje())
                .isEqualByComparingTo("30.0");
        assertThat(categoria(resumen, CategoriaEmision.ENVIO).getPorcentaje())
                .isEqualByComparingTo("20.0");
        assertThat(categoria(resumen, CategoriaEmision.VUELO).getTotalKg())
                .isEqualByComparingTo("0");
        assertThat(categoria(resumen, CategoriaEmision.VUELO).getPorcentaje())
                .isEqualByComparingTo("0.0");
    }

    @Test
    void redondeaElPorcentajeAUnDecimal() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdAndFechaActividadBetween(
                EMPRESA_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(electricidad("1.000"), flota("2.000")));

        EmisionResumenResponseDTO resumen = service.resumen(2026, null, USUARIO_ID);

        assertThat(categoria(resumen, CategoriaEmision.ELECTRICIDAD).getPorcentaje())
                .isEqualByComparingTo("33.3");
        assertThat(categoria(resumen, CategoriaEmision.FLOTA).getPorcentaje())
                .isEqualByComparingTo("66.7");
    }

    @Test
    void listaVaciaRetornaTotalesYPorcentajesEnCero() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdAndFechaActividadBetween(
                EMPRESA_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of());

        EmisionResumenResponseDTO resumen = service.resumen(2026, null, USUARIO_ID);

        assertThat(resumen.getTotalKg()).isEqualByComparingTo("0");
        assertThat(resumen.getTotalT()).isEqualByComparingTo("0");
        assertThat(resumen.getCategorias()).hasSize(4);
        assertThat(resumen.getCategorias())
                .allSatisfy(categoria -> {
                    assertThat(categoria.getTotalKg()).isEqualByComparingTo("0");
                    assertThat(categoria.getPorcentaje()).isEqualByComparingTo("0.0");
                });
    }

    @Test
    void periodoMensualConsultaSoloElRangoDelMes() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdAndFechaActividadBetween(
                EMPRESA_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(envio("10.000")));

        EmisionResumenResponseDTO resumen = service.resumen(2026, 2, USUARIO_ID);

        assertThat(resumen.getMes()).isEqualTo(2);
        assertThat(resumen.getTotalKg()).isEqualByComparingTo("10.000");
        verify(emisionRepository).findAllByEmpresaIdAndFechaActividadBetween(
                EMPRESA_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void mesFueraDeRangoLanzaBadRequest() {
        assertThatThrownBy(() -> service.resumen(2026, 13, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(emisionRepository);
    }

    @Test
    void anioInvalidoLanzaBadRequest() {
        assertThatThrownBy(() -> service.resumen(1, null, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(emisionRepository);
    }

    @Test
    void usuarioSinEmpresaLanzaEmpresaNoConfigurada() {
        when(usuarioRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(Usuario.builder().id(USUARIO_ID).build()));

        assertThatThrownBy(() -> service.resumen(2026, null, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private static ResumenCategoriaDTO categoria(EmisionResumenResponseDTO resumen,
                                                 CategoriaEmision categoria) {
        return resumen.getCategorias().stream()
                .filter(item -> item.getCategoria() == categoria)
                .findFirst()
                .orElseThrow();
    }

    private static EmisionElectricidad electricidad(String carbonKg) {
        return EmisionElectricidad.builder()
                .empresaId(EMPRESA_ID)
                .carbonKg(new BigDecimal(carbonKg))
                .build();
    }

    private static EmisionFlota flota(String carbonKg) {
        return EmisionFlota.builder()
                .empresaId(EMPRESA_ID)
                .carbonKg(new BigDecimal(carbonKg))
                .build();
    }

    private static EmisionEnvio envio(String carbonKg) {
        return EmisionEnvio.builder()
                .empresaId(EMPRESA_ID)
                .carbonKg(new BigDecimal(carbonKg))
                .build();
    }

    private static Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }
}
