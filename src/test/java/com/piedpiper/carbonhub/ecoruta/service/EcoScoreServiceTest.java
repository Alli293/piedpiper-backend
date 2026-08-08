package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.CertificacionActivaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EcoScoreResultado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.EstablecimientoRankeado;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de EcoScoreService: cálculo del EcoScore de un itinerario (PP-91),
 * redistribución de pesos ante componentes ausentes y clasificación ambiental.
 */
@ExtendWith(MockitoExtension.class)
class EcoScoreServiceTest {

    @Mock
    private ImaClient imaClient;

    @Mock
    private IndicadorAmbientalClient indicadorClient;

    private EcoScoreService ecoScoreService;

    @BeforeEach
    void setUp() {
        // PuntuacionAmbientalCalculator no tiene dependencias: se usa la implementación real.
        ecoScoreService = new EcoScoreService(imaClient, indicadorClient, new PuntuacionAmbientalCalculator());
    }

    @Nested
    @DisplayName("Cálculo con componentes disponibles")
    class ComponentesDisponibles {

        @Test
        @DisplayName("Los tres componentes disponibles: EcoScore = IMA*0.5 + indicadores*0.3 + actividad*0.2")
        void todosLosComponentesDisponibles() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(empresaId));

            when(imaClient.consultarIma(anyList())).thenReturn(Map.of(empresaId, ima(empresaId, 80)));
            when(indicadorClient.consultarIndicadores(anyList()))
                    .thenReturn(Map.of(empresaId, indicadorConCerts(empresaId, 3)));

            Itinerario itinerario = itinerarioConActividades(50);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // IMA_prom=80, indicadores_prom=min(3*20,100)=60, factor_actividad=50
            // EcoScore = 80*0.5 + 60*0.3 + 50*0.2 = 40 + 18 + 10 = 68.0
            assertThat(resultado).isNotNull();
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("68.0"));
            assertThat(resultado.getClasificacion()).isEqualTo(ClasificacionAmbiental.BUENA);
            assertThat(resultado.isParcial()).isFalse();
        }

        @Test
        @DisplayName("Falta IMA: peso se redistribuye entre indicadores y factor_actividad")
        void faltaIma() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(empresaId));

            when(imaClient.consultarIma(anyList())).thenReturn(Map.of());
            when(indicadorClient.consultarIndicadores(anyList()))
                    .thenReturn(Map.of(empresaId, indicadorConCerts(empresaId, 3)));

            Itinerario itinerario = itinerarioConActividades(50);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // indicadores_prom=60 (peso 0.3), factor_actividad=50 (peso 0.2), pesos redistribuidos sobre 0.5
            // EcoScore = (60*0.3 + 50*0.2) / 0.5 = 28 / 0.5 = 56.0
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("56.0"));
            assertThat(resultado.getClasificacion()).isEqualTo(ClasificacionAmbiental.MODERADA);
            assertThat(resultado.isParcial()).isTrue();
        }

        @Test
        @DisplayName("Faltan indicadores: peso se redistribuye entre IMA y factor_actividad")
        void faltanIndicadores() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(empresaId));

            when(imaClient.consultarIma(anyList())).thenReturn(Map.of(empresaId, ima(empresaId, 80)));
            when(indicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());

            Itinerario itinerario = itinerarioConActividades(50);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // EcoScore = (80*0.5 + 50*0.2) / 0.7 = 50 / 0.7 = 71.42857... → 71.4
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("71.4"));
            assertThat(resultado.getClasificacion()).isEqualTo(ClasificacionAmbiental.BUENA);
            assertThat(resultado.isParcial()).isTrue();
        }

        @Test
        @DisplayName("Sin factor_actividad (actividades sin estimación): peso se redistribuye entre IMA e indicadores")
        void faltaFactorActividad() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(empresaId));

            when(imaClient.consultarIma(anyList())).thenReturn(Map.of(empresaId, ima(empresaId, 80)));
            when(indicadorClient.consultarIndicadores(anyList()))
                    .thenReturn(Map.of(empresaId, indicadorConCerts(empresaId, 3)));

            Itinerario itinerario = itinerarioConActividades((Integer) null);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // EcoScore = (80*0.5 + 60*0.3) / 0.8 = 58 / 0.8 = 72.5
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("72.5"));
            assertThat(resultado.getClasificacion()).isEqualTo(ClasificacionAmbiental.BUENA);
            assertThat(resultado.isParcial()).isTrue();
        }

        @Test
        @DisplayName("Redondeo HALF_UP a 1 decimal sobre promedios no exactos")
        void redondeoHalfUp() {
            UUID e1 = UUID.randomUUID();
            UUID e2 = UUID.randomUUID();
            UUID e3 = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(e1), rankeado(e2), rankeado(e3));

            when(imaClient.consultarIma(anyList())).thenReturn(Map.of(
                    e1, ima(e1, 10), e2, ima(e2, 20), e3, ima(e3, 20)));
            when(indicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());

            Itinerario itinerario = itinerarioConActividades((Integer) null);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // IMA_prom = (10+20+20)/3 = 16.6666... → único componente disponible → EcoScore = 16.7
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("16.7"));
            assertThat(resultado.getClasificacion()).isEqualTo(ClasificacionAmbiental.MEJORABLE);
            assertThat(resultado.isParcial()).isTrue();
        }
    }

    @Nested
    @DisplayName("Sin datos disponibles")
    class SinDatos {

        @Test
        @DisplayName("Ningún componente disponible: no es posible calcular el EcoScore")
        void ningunComponenteDisponible() {
            when(imaClient.consultarIma(anyList())).thenReturn(Map.of());
            when(indicadorClient.consultarIndicadores(anyList())).thenReturn(Map.of());

            Itinerario itinerario = itinerarioConActividades((Integer) null);

            EcoScoreResultado resultado = ecoScoreService.calcular(List.of(), itinerario);

            assertThat(resultado).isNull();
        }

        @Test
        @DisplayName("Fallo en consulta de IMA se degrada con gracia (continúa sin IMA)")
        void fallosDeConsultaSeDegradanConGracia() {
            UUID empresaId = UUID.randomUUID();
            List<EstablecimientoRankeado> establecimientos = List.of(rankeado(empresaId));

            when(imaClient.consultarIma(anyList())).thenThrow(new RuntimeException("fallo simulado"));
            when(indicadorClient.consultarIndicadores(anyList()))
                    .thenReturn(Map.of(empresaId, indicadorConCerts(empresaId, 3)));

            Itinerario itinerario = itinerarioConActividades((Integer) null);

            EcoScoreResultado resultado = ecoScoreService.calcular(establecimientos, itinerario);

            // Solo indicadores disponible (peso 0.3) → EcoScore = 60.0
            assertThat(resultado).isNotNull();
            assertThat(resultado.getEcoScore()).isEqualByComparingTo(new BigDecimal("60.0"));
            assertThat(resultado.isParcial()).isTrue();
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private EstablecimientoRankeado rankeado(UUID empresaId) {
        EstablecimientoRankeado rankeado = new EstablecimientoRankeado();
        rankeado.setEmpresaId(empresaId);
        rankeado.setNombreEstablecimiento("Establecimiento " + empresaId);
        rankeado.setPuntuacionTuristica(BigDecimal.ONE);
        rankeado.setPuntuacionAmbiental(BigDecimal.ZERO);
        rankeado.setPuntuacionFinal(BigDecimal.ONE);
        return rankeado;
    }

    private IMADTO ima(UUID empresaId, int valor) {
        return new IMADTO(empresaId, BigDecimal.valueOf(valor), false, Instant.now());
    }

    /** Certificaciones con fecha antigua (sin bonus por recencia) para obtener scores predecibles. */
    private IndicadorAmbientalDTO indicadorConCerts(UUID empresaId, int cantidad) {
        List<CertificacionActivaDTO> certs = java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new CertificacionActivaDTO(UUID.randomUUID(), "Cert-" + i, "activa",
                        Instant.now().minus(365, ChronoUnit.DAYS)))
                .toList();
        return new IndicadorAmbientalDTO(empresaId, certs, Instant.now());
    }

    /** Construye un itinerario con un día y una actividad con la puntuación estimada indicada (o null). */
    private Itinerario itinerarioConActividades(Integer puntuacionEstimada) {
        ItinerarioActividad actividad = ItinerarioActividad.builder()
                .nombre("Actividad")
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .provincia(Provincia.SAN_JOSE)
                .orden(1)
                .puntuacionAmbientalEstimada(puntuacionEstimada)
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .numeroDia(1)
                .fecha(java.time.LocalDate.now())
                .orden(1)
                .actividades(List.of(actividad))
                .build();

        return Itinerario.builder()
                .dias(List.of(dia))
                .build();
    }
}
