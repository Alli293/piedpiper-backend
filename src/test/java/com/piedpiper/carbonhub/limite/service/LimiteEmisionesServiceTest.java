package com.piedpiper.carbonhub.limite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimiteEmisionesServiceTest {
    private static final Long EMPRESA_ID = 7L;

    @Mock
    private LimiteEmisionesRepository repository;

    @InjectMocks
    private LimiteEmisionesService service;

    @Test
    void guardarLimiteCreaSiNoExiste() {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(LimiteEmisiones.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LimiteEmisionesResponseDTO response = service.guardarLimite(EMPRESA_ID, request);

        ArgumentCaptor<LimiteEmisiones> captor = ArgumentCaptor.forClass(LimiteEmisiones.class);
        verify(repository).findByEmpresaIdAndAnio(EMPRESA_ID, 2026);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(captor.getValue().getJustificacion()).isEqualTo("Meta anual");
        assertThat(response.limiteMt()).isEqualByComparingTo("50.0000");
    }

    @Test
    void guardarLimiteActualizaSiExiste() {
        LimiteEmisiones existing = new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"));
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("40.0000"),
                "Meta actualizada"
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        LimiteEmisionesResponseDTO response = service.guardarLimite(EMPRESA_ID, request);

        verify(repository).findByEmpresaIdAndAnio(EMPRESA_ID, 2026);
        verify(repository).save(existing);
        assertThat(existing.getLimiteMt()).isEqualByComparingTo("40.0000");
        assertThat(existing.getJustificacion()).isEqualTo("Meta actualizada");
        assertThat(response.limiteMt()).isEqualByComparingTo("40.0000");
    }

    @Test
    void guardarLimitePermiteJustificacionNula() {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                null
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(LimiteEmisiones.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LimiteEmisionesResponseDTO response = service.guardarLimite(EMPRESA_ID, request);

        ArgumentCaptor<LimiteEmisiones> captor = ArgumentCaptor.forClass(LimiteEmisiones.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJustificacion()).isNull();
        assertThat(response.justificacion()).isNull();
    }

    @Test
    void listarLimitesRetornaRegistrosDeLaEmpresa() {
        LimiteEmisiones limite = new LimiteEmisiones(
                EMPRESA_ID,
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(repository.findAllByEmpresaIdOrderByAnioDesc(EMPRESA_ID)).thenReturn(List.of(limite));

        List<LimiteEmisionesResponseDTO> response = service.listarLimites(EMPRESA_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().justificacion()).isEqualTo("Meta anual");
    }

    @Test
    void eliminarLimiteEliminaRegistroExistente() {
        LimiteEmisiones limite = new LimiteEmisiones(
                EMPRESA_ID,
                2026,
                new BigDecimal("50.0000")
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.of(limite));

        service.eliminarLimite(EMPRESA_ID, 2026);

        verify(repository).delete(limite);
    }
}
