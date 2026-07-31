package com.piedpiper.carbonhub.emision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionElectricidadMapperImpl;
import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionEnvioMapperImpl;
import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionFlotaMapperImpl;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapper;
import com.piedpiper.carbonhub.emision.mappers.EmisionVueloMapperImpl;
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
import com.piedpiper.carbonhub.ima.service.ImaCacheInvalidator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Spy
    private EmisionElectricidadMapper emisionElectricidadMapper = new EmisionElectricidadMapperImpl();
    @Spy
    private EmisionVueloMapper emisionVueloMapper = new EmisionVueloMapperImpl();
    @Spy
    private EmisionEnvioMapper emisionEnvioMapper = new EmisionEnvioMapperImpl();
    @Spy
    private EmisionFlotaMapper emisionFlotaMapper = new EmisionFlotaMapperImpl();
    @Mock
    private ImaCacheInvalidator imaCacheInvalidator;

    @InjectMocks
    private EmisionConsultaService service;

    @Test
    void listarUsaEmpresaDelUsuarioAutenticado() {
        EmisionElectricidad emision = EmisionElectricidad.builder()
                .id(EMISION_ID)
                .empresaId(EMPRESA_ID)
                .build();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdWithFilters(EMPRESA_ID, null, null))
                .thenReturn(List.of(emision));

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, null, null, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isInstanceOf(EmisionElectricidadResponseDTO.class);
        assertThat(response.get(0).getId()).isEqualTo(EMISION_ID);
        verify(emisionRepository).findAllByEmpresaIdWithFilters(EMPRESA_ID, null, null);
    }

    @Test
    void listarAplicaFiltrosDeCategoriaAnioYMes() {
        EmisionFlota flota = EmisionFlota.builder()
                .id(EMISION_ID)
                .empresaId(EMPRESA_ID)
                .build();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllFlotaByEmpresaIdWithFilters(EMPRESA_ID, 2026, 7))
                .thenReturn(List.of(flota));

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, CategoriaEmision.FLOTA, 2026, 7);

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isInstanceOf(EmisionFlotaResponseDTO.class);
        assertThat(response.get(0).getId()).isEqualTo(EMISION_ID);
        verify(emisionRepository).findAllFlotaByEmpresaIdWithFilters(EMPRESA_ID, 2026, 7);
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

        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findAllByEmpresaIdWithFilters(EMPRESA_ID, null, null))
                .thenReturn(List.of(electricidad, vuelo, envio, flota));

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID, null, null, null);

        assertThat(response).hasSize(4);
        assertThat(response.get(0)).isInstanceOf(EmisionElectricidadResponseDTO.class);
        assertThat(response.get(1)).isInstanceOf(EmisionVueloResponseDTO.class);
        assertThat(response.get(2)).isInstanceOf(EmisionEnvioResponseDTO.class);
        assertThat(response.get(3)).isInstanceOf(EmisionFlotaResponseDTO.class);
    }

    @Test
    void obtenerMapeaUnaEmisionDeFlota() {
        EmisionFlota flota = EmisionFlota.builder().id(EMISION_ID).empresaId(EMPRESA_ID).build();
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID)).thenReturn(Optional.of(flota));

        EmisionResponseDTO obtenida = service.obtener(EMISION_ID, USUARIO_ID);

        assertThat(obtenida).isInstanceOf(EmisionFlotaResponseDTO.class);
        assertThat(obtenida.getId()).isEqualTo(EMISION_ID);
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
