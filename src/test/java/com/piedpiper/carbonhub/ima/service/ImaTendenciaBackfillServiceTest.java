package com.piedpiper.carbonhub.ima.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImaTendenciaBackfillServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private ImaService imaService;

    private ImaTendenciaBackfillService backfillService() {
        return new ImaTendenciaBackfillService(imaService);
    }

    @Test
    void calculaUnSnapshotPorCadaMesPendiente() {
        YearMonth marzo = YearMonth.of(2026, 3);
        YearMonth abril = YearMonth.of(2026, 4);

        backfillService().completarMesesPendientes(USUARIO_ID, List.of(marzo, abril));

        verify(imaService).obtenerIma(2026, 3, USUARIO_ID);
        verify(imaService).obtenerIma(2026, 4, USUARIO_ID);
    }

    @Test
    void unMesQueFallaNoDetieneElCalculoDeLosDemas() {
        YearMonth marzo = YearMonth.of(2026, 3);
        YearMonth abril = YearMonth.of(2026, 4);
        doThrow(new RuntimeException("fallo simulado"))
                .when(imaService).obtenerIma(2026, 3, USUARIO_ID);

        backfillService().completarMesesPendientes(USUARIO_ID, List.of(marzo, abril));

        verify(imaService).obtenerIma(2026, 4, USUARIO_ID);
    }

    @Test
    void listaVaciaNoInvocaElCalculo() {
        backfillService().completarMesesPendientes(USUARIO_ID, List.of());

        verify(imaService, never()).obtenerIma(anyInt(), anyInt(), any());
    }
}
