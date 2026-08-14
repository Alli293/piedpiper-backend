package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoInsigniasEmpresaBootstrapTest {

    @Mock
    private CatalogoInsigniaRepository catalogoInsigniaRepository;

    @Test
    void creaLaInsigniaDeExcelenciaClimaticaConTresNiveles() throws Exception {
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "bronce"))
                .thenReturn(Optional.empty());
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "plata"))
                .thenReturn(Optional.empty());
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "oro"))
                .thenReturn(Optional.empty());

        new CatalogoInsigniasEmpresaBootstrap(catalogoInsigniaRepository).run(null);

        ArgumentCaptor<CatalogoInsignia> captor = ArgumentCaptor.forClass(CatalogoInsignia.class);
        verify(catalogoInsigniaRepository, times(3)).save(captor.capture());
        List<CatalogoInsignia> guardadas = captor.getAllValues();
        assertThat(guardadas).extracting(CatalogoInsignia::getNivelInsignia)
                .containsExactly("bronce", "plata", "oro");
        assertThat(guardadas).allSatisfy(insignia -> {
            assertThat(insignia.getIdInsignia())
                    .isEqualTo(CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL);
            assertThat(insignia.getNombre()).isEqualTo("Excelencia climatica empresarial");
            assertThat(insignia.isActiva()).isTrue();
        });
        assertThat(guardadas.get(2).getTiposCertificacionesRequeridas())
                .containsExactlyInAnyOrder(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL,
                        TipoCertificacion.CARBONO_NEUTRAL,
                        TipoCertificacion.REDUCCION_EMISIONES);
    }

    @Test
    void noDuplicaUnaDefinicionExistente() throws Exception {
        CatalogoInsignia existente = CatalogoInsignia.builder()
                .idInsignia(CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL)
                .nivelInsignia("bronce")
                .build();
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "bronce"))
                .thenReturn(Optional.of(existente));
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "plata"))
                .thenReturn(Optional.of(existente));
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                CatalogoInsigniasEmpresaBootstrap.EXCELENCIA_CLIMATICA_EMPRESARIAL, "oro"))
                .thenReturn(Optional.of(existente));

        new CatalogoInsigniasEmpresaBootstrap(catalogoInsigniaRepository).run(null);

        verify(catalogoInsigniaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
