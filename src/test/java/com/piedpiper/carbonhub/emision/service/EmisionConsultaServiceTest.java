package com.piedpiper.carbonhub.emision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionElectricidadResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionVueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EmisionConsultaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID EMISION_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private EmisionElectricidadMapper emisionElectricidadMapper;
    @Mock
    private EmisionVueloMapper emisionVueloMapper;
    @Mock
    private EmisionEnvioMapper emisionEnvioMapper;
    @Mock
    private EmisionFlotaMapper emisionFlotaMapper;

    @InjectMocks
    private EmisionConsultaService service;

    @Test
    void listarUsaEmpresaDelUsuarioAutenticado() {
        EmisionElectricidad emision = EmisionElectricidad.builder()
                .id(EMISION_ID)
                .empresaId(EMPRESA_ID)
                .build();
        EmisionElectricidadResponseDTO dto = new EmisionElectricidadResponseDTO();
        dto.setId(EMISION_ID);
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdOrderByFechaActividadDescCreatedAtDesc(EMPRESA_ID))
                .thenReturn(List.of(emision));
        when(emisionElectricidadMapper.toDto(emision)).thenReturn(dto);

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, null, null, null);

        assertThat(response).containsExactly(dto);
        verify(emisionRepository).findAllByEmpresaIdOrderByFechaActividadDescCreatedAtDesc(EMPRESA_ID);
    }

    @Test
    void listarAplicaFiltrosDeCategoriaAnioYMes() {
        EmisionElectricidad electricidad = EmisionElectricidad.builder()
                .empresaId(EMPRESA_ID)
                .fechaActividad(LocalDate.of(2026, 7, 1))
                .build();
        EmisionFlota flota = EmisionFlota.builder()
                .empresaId(EMPRESA_ID)
                .fechaActividad(LocalDate.of(2026, 7, 2))
                .build();
        EmisionFlota flotaOtroMes = EmisionFlota.builder()
                .empresaId(EMPRESA_ID)
                .fechaActividad(LocalDate.of(2026, 8, 2))
                .build();
        EmisionFlotaResponseDTO dto = new EmisionFlotaResponseDTO();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdAndFechaActividadGreaterThanEqualAndFechaActividadLessThanOrderByFechaActividadDescCreatedAtDesc(
                EMPRESA_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
                .thenReturn(List.of(electricidad, flota, flotaOtroMes));
        when(emisionFlotaMapper.toDto(flota)).thenReturn(dto);

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, CategoriaEmision.FLOTA, 2026, 7);

        assertThat(response).containsExactly(dto);
        verify(emisionRepository)
                .findAllByEmpresaIdAndFechaActividadGreaterThanEqualAndFechaActividadLessThanOrderByFechaActividadDescCreatedAtDesc(
                        EMPRESA_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
    }

    @Test
    void listarRechazaMesFueraDeRango() {
        assertThatThrownBy(() -> service.listar(USUARIO_ID, null, null, 13))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listarMapeaLasCuatroCategoriasDeEmision() {
        EmisionElectricidad electricidad = EmisionElectricidad.builder().empresaId(EMPRESA_ID).build();
        EmisionVuelo vuelo = EmisionVuelo.builder().empresaId(EMPRESA_ID).build();
        EmisionEnvio envio = EmisionEnvio.builder().empresaId(EMPRESA_ID).build();
        EmisionFlota flota = EmisionFlota.builder().empresaId(EMPRESA_ID).build();

        EmisionElectricidadResponseDTO electricidadDto = new EmisionElectricidadResponseDTO();
        EmisionVueloResponseDTO vueloDto = new EmisionVueloResponseDTO();
        EmisionEnvioResponseDTO envioDto = new EmisionEnvioResponseDTO();
        EmisionFlotaResponseDTO flotaDto = new EmisionFlotaResponseDTO();

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdOrderByFechaActividadDescCreatedAtDesc(EMPRESA_ID))
                .thenReturn(List.of(electricidad, vuelo, envio, flota));
        when(emisionElectricidadMapper.toDto(electricidad)).thenReturn(electricidadDto);
        when(emisionVueloMapper.toDto(vuelo)).thenReturn(vueloDto);
        when(emisionEnvioMapper.toDto(envio)).thenReturn(envioDto);
        when(emisionFlotaMapper.toDto(flota)).thenReturn(flotaDto);

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, null, null, null);

        assertThat(response).containsExactly(electricidadDto, vueloDto, envioDto, flotaDto);
    }

    @Test
    void obtenerMapeaUnaEmisionDeFlota() {
        EmisionFlota flota = EmisionFlota.builder().id(EMISION_ID).empresaId(EMPRESA_ID).build();
        EmisionFlotaResponseDTO flotaDto = new EmisionFlotaResponseDTO();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID)).thenReturn(Optional.of(flota));
        when(emisionFlotaMapper.toDto(flota)).thenReturn(flotaDto);

        assertThat(service.obtener(EMISION_ID, USUARIO_ID)).isSameAs(flotaDto);
    }

    @Test
    void obtenerNoPermiteAccederAEmisionDeOtraEmpresa() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(EMISION_ID, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(emisionRepository).findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID);
    }

    @Test
    void eliminarEmisionInexistenteODeOtraEmpresaDevuelve404() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(EMISION_ID, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eliminarUsaEmpresaDelUsuarioAutenticado() {
        EmisionElectricidad emision = EmisionElectricidad.builder()
                .id(EMISION_ID)
                .empresaId(EMPRESA_ID)
                .build();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID))
                .thenReturn(Optional.of(emision));

        service.eliminar(EMISION_ID, USUARIO_ID);

        verify(emisionRepository).delete(emision);
    }

    @Test
    void listarConUsuarioSinEmpresaDevuelve422() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenThrow(ApiException.empresaNoConfigurada());

        assertThatThrownBy(() -> service.listar(USUARIO_ID, null, null, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
