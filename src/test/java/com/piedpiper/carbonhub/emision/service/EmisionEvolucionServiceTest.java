package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EvolucionMensualDTO.PuntoMensual;
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
import java.util.List;
import java.util.Optional;
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
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EmisionEvolucionService service;

    @Test
    void serieCompletaConDatosEnVariosMeses() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2026)).thenReturn(List.of(
                new Object[]{1, new BigDecimal("150.500")},
                new Object[]{3, new BigDecimal("200.000")},
                new Object[]{7, new BigDecimal("80.250")}
        ));

        EvolucionMensualDTO resultado = service.obtenerEvolucion(2026, USUARIO_ID);

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
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2020)).thenReturn(List.of());

        EvolucionMensualDTO resultado = service.obtenerEvolucion(2020, USUARIO_ID);

        assertThat(resultado.getAnio()).isEqualTo(2020);
        assertThat(resultado.getSerie()).hasSize(12);
        assertThat(resultado.getSerie())
                .allMatch(p -> p.getTotalCarbonKg().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void serieTieneDocePuntosConMesesOrdenados() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumarCarbonKgPorMes(EMPRESA_ID, 2025)).thenReturn(List.of());

        EvolucionMensualDTO resultado = service.obtenerEvolucion(2025, USUARIO_ID);

        List<PuntoMensual> serie = resultado.getSerie();
        for (int i = 0; i < 12; i++) {
            assertThat(serie.get(i).getMes()).isEqualTo(i + 1);
        }
    }

    @Test
    void usuarioSinEmpresaLanzaExcepcion() {
        Usuario sinEmpresa = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(sinEmpresa));

        assertThatThrownBy(() -> service.obtenerEvolucion(2026, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioNoExistenteLanzaExcepcion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEvolucion(2026, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }
}
