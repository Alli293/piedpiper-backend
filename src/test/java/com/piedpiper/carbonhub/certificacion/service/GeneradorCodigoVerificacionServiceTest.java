package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneradorCodigoVerificacionServiceTest {

    @Mock
    private CertificacionRepository certificacionRepository;

    private GeneradorCodigoVerificacionService service;

    private void prepararServicio() {
        service = new GeneradorCodigoVerificacionService(certificacionRepository);
    }

    @Test
    void generaUnCodigoConElFormatoEsperado() {
        prepararServicio();
        when(certificacionRepository.existsByCodigoVerificacion(anyString())).thenReturn(false);

        String codigo = service.generar();

        assertThat(codigo).matches("^CH-" + Year.now() + "-[0-9A-HJKMNP-TV-Z]{8}$");
        assertThat(GeneradorCodigoVerificacionService.formatoValido(codigo)).isTrue();
    }

    @Test
    void reintentaAnteUnaColisionYDevuelveElSiguienteCandidatoLibre() {
        prepararServicio();
        when(certificacionRepository.existsByCodigoVerificacion(anyString()))
                .thenReturn(true, false);

        String codigo = service.generar();

        assertThat(GeneradorCodigoVerificacionService.formatoValido(codigo)).isTrue();
    }

    @Test
    void lanzaErrorInternoSiAgotaLosIntentosSinEncontrarUnCodigoLibre() {
        prepararServicio();
        when(certificacionRepository.existsByCodigoVerificacion(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.generar())
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void formatoValidoRechazaCodigosMalFormados() {
        assertThat(GeneradorCodigoVerificacionService.formatoValido(null)).isFalse();
        assertThat(GeneradorCodigoVerificacionService.formatoValido("")).isFalse();
        assertThat(GeneradorCodigoVerificacionService.formatoValido("CH-2026-corto")).isFalse();
        // I, L, O y U no pertenecen al alfabeto Crockford base32 usado.
        assertThat(GeneradorCodigoVerificacionService.formatoValido("CH-2026-ILOU1234")).isFalse();
        assertThat(GeneradorCodigoVerificacionService.formatoValido("CH-26-8F4A19KD")).isFalse();
    }

    @Test
    void formatoValidoAceptaUnCodigoBienFormado() {
        assertThat(GeneradorCodigoVerificacionService.formatoValido("CH-2026-8F4A19KD")).isTrue();
    }
}
