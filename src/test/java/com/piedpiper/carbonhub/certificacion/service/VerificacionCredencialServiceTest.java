package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionCredencialServiceTest {

    @Mock
    private ConsultaCertificacionService consultaCertificacionService;
    @Mock
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    private VerificacionCredencialService service;

    @BeforeEach
    void prepararServicio() {
        service = new VerificacionCredencialService(
                consultaCertificacionService, insigniaEmpresaConsultaService);
    }

    @Test
    void devuelveElResultadoDeCertificacionSinConsultarInsigniasCuandoLaEncuentra() {
        String codigo = "CH-2026-8F4A19KD";
        VerificacionCredencialDTO dto = new VerificacionCredencialDTO();
        dto.setCategoria("CERTIFICACION");
        when(consultaCertificacionService.verificarPorCodigo(codigo)).thenReturn(dto);

        VerificacionCredencialDTO resultado = service.verificar(codigo);

        assertThat(resultado.getCategoria()).isEqualTo("CERTIFICACION");
        verify(insigniaEmpresaConsultaService, never()).verificarPorCodigo(anyString());
    }

    @Test
    void caeAInsigniaCuandoElCodigoNoEsUnaCertificacion() {
        String codigo = "CH-2026-8F4A19KD";
        VerificacionCredencialDTO dto = new VerificacionCredencialDTO();
        dto.setCategoria("INSIGNIA");
        when(consultaCertificacionService.verificarPorCodigo(codigo))
                .thenThrow(ApiException.recursoNoEncontrado("Credencial no encontrada."));
        when(insigniaEmpresaConsultaService.verificarPorCodigo(codigo)).thenReturn(dto);

        VerificacionCredencialDTO resultado = service.verificar(codigo);

        assertThat(resultado.getCategoria()).isEqualTo("INSIGNIA");
    }

    @Test
    void propagaUn404DeInsigniaCuandoElCodigoNoExisteEnNingunDominio() {
        String codigo = "CH-2026-8F4A19KD";
        when(consultaCertificacionService.verificarPorCodigo(codigo))
                .thenThrow(ApiException.recursoNoEncontrado("Credencial no encontrada."));
        when(insigniaEmpresaConsultaService.verificarPorCodigo(codigo))
                .thenThrow(ApiException.recursoNoEncontrado("Credencial no encontrada."));

        assertThatThrownBy(() -> service.verificar(codigo))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void propagaUnErrorDeCertificacionQueNoEsUn404SinConsultarInsignias() {
        String codigo = "CH-2026-8F4A19KD";
        when(consultaCertificacionService.verificarPorCodigo(codigo))
                .thenThrow(ApiException.errorInterno("Fallo inesperado."));

        assertThatThrownBy(() -> service.verificar(codigo))
                .isInstanceOf(ApiException.class);
        verify(insigniaEmpresaConsultaService, never()).verificarPorCodigo(anyString());
    }
}
