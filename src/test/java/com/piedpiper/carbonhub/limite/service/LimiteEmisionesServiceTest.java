package com.piedpiper.carbonhub.limite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.limite.mappers.LimiteEmisionesMapperImpl;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LimiteEmisionesServiceTest {
    private static final UUID EMPRESA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private LimiteEmisionesRepository repository;

    private LimiteEmisionesService service() {
        return new LimiteEmisionesService(repository, new LimiteEmisionesMapperImpl());
    }

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

        LimiteEmisionesResponseDTO response = service().guardarLimite(EMPRESA_ID, request);

        ArgumentCaptor<LimiteEmisiones> captor = ArgumentCaptor.forClass(LimiteEmisiones.class);
        verify(repository).findByEmpresaIdAndAnio(EMPRESA_ID, 2026);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(captor.getValue().getJustificacion()).isEqualTo("Meta anual");
        assertThat(response.getLimiteMt()).isEqualByComparingTo("50.0000");
        assertThat(response.getMensaje()).isEqualTo("Limite vigente del anio 2026: 50.0000 t CO2e.");
        assertThat(response.isRecienCreada()).isTrue();
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

        LimiteEmisionesResponseDTO response = service().guardarLimite(EMPRESA_ID, request);

        verify(repository).findByEmpresaIdAndAnio(EMPRESA_ID, 2026);
        verify(repository).save(existing);
        assertThat(existing.getLimiteMt()).isEqualByComparingTo("40.0000");
        assertThat(existing.getJustificacion()).isEqualTo("Meta actualizada");
        assertThat(response.getLimiteMt()).isEqualByComparingTo("40.0000");
        assertThat(response.isRecienCreada()).isFalse();
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

        LimiteEmisionesResponseDTO response = service().guardarLimite(EMPRESA_ID, request);

        ArgumentCaptor<LimiteEmisiones> captor = ArgumentCaptor.forClass(LimiteEmisiones.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJustificacion()).isNull();
        assertThat(response.getJustificacion()).isNull();
    }

    @Test
    void guardarLimiteConflictoDeDatos_lanza409() {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(LimiteEmisiones.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        LimiteEmisionesService servicio = service();
        assertThatThrownBy(() -> servicio.guardarLimite(EMPRESA_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
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

        List<LimiteEmisionesResponseDTO> response = service().listarLimites(EMPRESA_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getJustificacion()).isEqualTo("Meta anual");
        assertThat(response.getFirst().getMensaje()).isNull();
    }

    @Test
    void eliminarLimiteEliminaRegistroExistente() {
        LimiteEmisiones limite = new LimiteEmisiones(
                EMPRESA_ID,
                2026,
                new BigDecimal("50.0000")
        );
        when(repository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026)).thenReturn(Optional.of(limite));

        service().eliminarLimite(EMPRESA_ID, 2026);

        verify(repository).delete(limite);
    }
}
