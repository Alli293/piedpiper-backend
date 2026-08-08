package com.piedpiper.carbonhub.meta.service;

import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.meta.mappers.MetaMapperImpl;
import com.piedpiper.carbonhub.meta.models.dtos.CrearMetaRequestDTO;
import com.piedpiper.carbonhub.meta.models.dtos.MetaResponseDTO;
import com.piedpiper.carbonhub.meta.models.entities.Meta;
import com.piedpiper.carbonhub.meta.models.enums.EstadoMeta;
import com.piedpiper.carbonhub.meta.repository.MetaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.now(ZonasHorarias.COSTA_RICA);

    @Mock
    private MetaRepository metaRepository;
    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private EmpresaRepository empresaRepository;

    private MetaService service;

    @BeforeEach
    void setUp() {
        service = new MetaService(
                metaRepository, emisionRepository, emisionEmpresaService, empresaRepository, new MetaMapperImpl());
        lenient().when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        lenient().when(empresaRepository.getReferenceById(EMPRESA_ID))
                .thenReturn(Empresa.builder().id(EMPRESA_ID).build());
        lenient().when(metaRepository.saveAndFlush(any(Meta.class))).thenAnswer(inv -> {
            Meta meta = inv.getArgument(0);
            meta.setId(UUID.randomUUID());
            return meta;
        });
    }

    @Test
    void persisteLaMetaConLosCamposCorrectos() {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Reducir huella total", new BigDecimal("50.0000"), HOY.plusMonths(6));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.crear(USUARIO_ID, request);

        ArgumentCaptor<Meta> captor = ArgumentCaptor.forClass(Meta.class);
        verify(metaRepository).saveAndFlush(captor.capture());
        Meta guardada = captor.getValue();
        assertThat(guardada.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        assertThat(guardada.getNombreMeta()).isEqualTo("Reducir huella total");
        assertThat(guardada.getValorObjetivoHuellaT()).isEqualByComparingTo("50.0000");
        assertThat(guardada.getFechaLimite()).isEqualTo(HOY.plusMonths(6));
        assertThat(guardada.getEstado()).isEqualTo(EstadoMeta.ACTIVA);
        assertThat(guardada.getFechaCreacion()).isNotNull();
    }

    @Test
    void fechaLimitePasadaLanzaApiException422SinGuardarNada() {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Meta invalida", new BigDecimal("50.0000"), HOY.minusDays(1));

        assertThatThrownBy(() -> service.crear(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(metaRepository, never()).saveAndFlush(any());
    }

    @Test
    void fechaLimiteHoyEsValida() {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO("Meta de hoy", new BigDecimal("50.0000"), HOY);
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.crear(USUARIO_ID, request);

        verify(metaRepository).saveAndFlush(any());
    }

    @Test
    void unErrorDeBaseDeDatosAlGuardarNoDejaRegistroParcialYMuestraMensajePropio() {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Reducir huella total", new BigDecimal("50.0000"), HOY.plusMonths(6));
        when(metaRepository.saveAndFlush(any(Meta.class)))
                .thenThrow(new DataIntegrityViolationException("boom"));

        assertThatThrownBy(() -> service.crear(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("Ocurrió un error al guardar la meta. Por favor, intenta nuevamente.");
                });
    }

    @Test
    void calculaElProgresoComoHuellaActualEntreValorObjetivoPorCien() {
        Meta meta = metaActiva(new BigDecimal("50.0000"), HOY.plusMonths(3));
        when(metaRepository.findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(EMPRESA_ID, EstadoMeta.ACTIVA))
                .thenReturn(List.of(meta));
        // 30 toneladas = 30000 kg
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(new BigDecimal("30000"));

        List<MetaResponseDTO> resultado = service.listar(USUARIO_ID, "mes_actual", null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHuellaActualT()).isEqualByComparingTo("30.0000");
        assertThat(resultado.get(0).getProgresoPorcentaje()).isEqualTo(60);
    }

    @Test
    void unaMetaConFechaLimitePasadaSeMarcaVencidaPeroSigueEnElListado() {
        Meta meta = metaActiva(new BigDecimal("95.0000"), HOY.minusDays(10));
        when(metaRepository.findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(EMPRESA_ID, EstadoMeta.ACTIVA))
                .thenReturn(List.of(meta));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        List<MetaResponseDTO> resultado = service.listar(USUARIO_ID, "mes_actual", null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isVencida()).isTrue();
    }

    @Test
    void unaMetaConFechaLimiteFuturaNoSeMarcaVencida() {
        Meta meta = metaActiva(new BigDecimal("95.0000"), HOY.plusDays(10));
        when(metaRepository.findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(EMPRESA_ID, EstadoMeta.ACTIVA))
                .thenReturn(List.of(meta));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndFechaActividadEntre(eq(EMPRESA_ID), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        List<MetaResponseDTO> resultado = service.listar(USUARIO_ID, "mes_actual", null);

        assertThat(resultado.get(0).isVencida()).isFalse();
    }

    @Test
    void listaSoloLasMetasDeLaEmpresaAutenticada() {
        when(metaRepository.findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(EMPRESA_ID, EstadoMeta.ACTIVA))
                .thenReturn(List.of());

        List<MetaResponseDTO> resultado = service.listar(USUARIO_ID, null, null);

        assertThat(resultado).isEmpty();
        verify(metaRepository).findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(EMPRESA_ID, EstadoMeta.ACTIVA);
    }

    @Test
    void unAnioInvalidoLanzaApiException400() {
        int anioInvalido = HOY.getYear() + 5;

        assertThatThrownBy(() -> service.listar(USUARIO_ID, "mes_actual", anioInvalido))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Meta metaActiva(BigDecimal valorObjetivoHuellaT, LocalDate fechaLimite) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Meta.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .nombreMeta("Meta de prueba")
                .valorObjetivoHuellaT(valorObjetivoHuellaT)
                .fechaLimite(fechaLimite)
                .estado(EstadoMeta.ACTIVA)
                .fechaCreacion(Instant.now())
                .build();
    }
}
