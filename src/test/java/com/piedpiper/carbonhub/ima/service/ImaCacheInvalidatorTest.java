package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.repository.AgregadoSectorialRepository;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImaCacheInvalidatorTest {

    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID PAR_ID = UUID.randomUUID();

    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;
    @Mock
    private AgregadoSectorialRepository agregadoSectorialRepository;
    @Mock
    private EmpresaRepository empresaRepository;

    private ImaCacheInvalidator invalidator() {
        return new ImaCacheInvalidator(imaSnapshotRepository, agregadoSectorialRepository, empresaRepository);
    }

    @Test
    void invalidarBorraSnapshotsDeLaEmpresaYDeSusParesDeSector() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).sectorIndustrial(SectorIndustrial.SERVICIOS).build();
        Empresa par = Empresa.builder().id(PAR_ID).sectorIndustrial(SectorIndustrial.SERVICIOS).build();

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(empresaRepository.findBySectorIndustrial(SectorIndustrial.SERVICIOS))
                .thenReturn(List.of(empresa, par));

        invalidator().invalidar(EMPRESA_ID);

        verify(imaSnapshotRepository).deleteAllByEmpresaId(EMPRESA_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(imaSnapshotRepository).deleteAllByEmpresaIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly(PAR_ID);

        verify(agregadoSectorialRepository).deleteAll();
    }

    @Test
    void invalidarNoBorraParesCuandoLaEmpresaNoTieneOtrasEnSuSector() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).sectorIndustrial(SectorIndustrial.SERVICIOS).build();

        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(empresaRepository.findBySectorIndustrial(SectorIndustrial.SERVICIOS))
                .thenReturn(List.of(empresa));

        invalidator().invalidar(EMPRESA_ID);

        verify(imaSnapshotRepository).deleteAllByEmpresaId(EMPRESA_ID);
        verify(imaSnapshotRepository, never()).deleteAllByEmpresaIdIn(any());
        verify(agregadoSectorialRepository).deleteAll();
    }

    @Test
    void invalidarNoFallaCuandoLaEmpresaYaNoExiste() {
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.empty());

        invalidator().invalidar(EMPRESA_ID);

        verify(imaSnapshotRepository).deleteAllByEmpresaId(EMPRESA_ID);
        verify(imaSnapshotRepository, never()).deleteAllByEmpresaIdIn(any());
        verify(agregadoSectorialRepository).deleteAll();
    }
}
