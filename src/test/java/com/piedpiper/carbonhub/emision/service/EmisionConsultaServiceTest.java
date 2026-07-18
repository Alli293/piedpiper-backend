package com.piedpiper.carbonhub.emision.service;

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
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.ima.service.ImaCacheInvalidator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionConsultaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID EMISION_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmisionElectricidadMapper emisionElectricidadMapper;
    @Mock
    private EmisionVueloMapper emisionVueloMapper;
    @Mock
    private EmisionEnvioMapper emisionEnvioMapper;
    @Mock
    private EmisionFlotaMapper emisionFlotaMapper;
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
        EmisionElectricidadResponseDTO dto = new EmisionElectricidadResponseDTO();
        dto.setId(EMISION_ID);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdOrderByCreatedAtDesc(EMPRESA_ID))
                .thenReturn(List.of(emision));
        when(emisionElectricidadMapper.toDto(emision)).thenReturn(dto);

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID);

        assertThat(response).containsExactly(dto);
        verify(emisionRepository).findAllByEmpresaIdOrderByCreatedAtDesc(EMPRESA_ID);
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

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findAllByEmpresaIdOrderByCreatedAtDesc(EMPRESA_ID))
                .thenReturn(List.of(electricidad, vuelo, envio, flota));
        when(emisionElectricidadMapper.toDto(electricidad)).thenReturn(electricidadDto);
        when(emisionVueloMapper.toDto(vuelo)).thenReturn(vueloDto);
        when(emisionEnvioMapper.toDto(envio)).thenReturn(envioDto);
        when(emisionFlotaMapper.toDto(flota)).thenReturn(flotaDto);

        List<EmisionResponseDTO> response = service.listar(USUARIO_ID);

        assertThat(response).containsExactly(electricidadDto, vueloDto, envioDto, flotaDto);
    }

    @Test
    void obtenerMapeaUnaEmisionDeFlota() {
        EmisionFlota flota = EmisionFlota.builder().id(EMISION_ID).empresaId(EMPRESA_ID).build();
        EmisionFlotaResponseDTO flotaDto = new EmisionFlotaResponseDTO();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID)).thenReturn(Optional.of(flota));
        when(emisionFlotaMapper.toDto(flota)).thenReturn(flotaDto);

        assertThat(service.obtener(EMISION_ID, USUARIO_ID)).isSameAs(flotaDto);
    }

    @Test
    void obtenerNoPermiteAccederAEmisionDeOtraEmpresa() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(EMISION_ID, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(emisionRepository).findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID);
    }

    @Test
    void eliminarUsaEmpresaDelUsuarioAutenticado() {
        EmisionElectricidad emision = EmisionElectricidad.builder()
                .id(EMISION_ID)
                .empresaId(EMPRESA_ID)
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.findByIdAndEmpresaId(EMISION_ID, EMPRESA_ID))
                .thenReturn(Optional.of(emision));

        service.eliminar(EMISION_ID, USUARIO_ID);

        verify(emisionRepository).delete(emision);
    }

    @Test
    void listarConUsuarioSinEmpresaDevuelve422() {
        Usuario usuarioSinEmpresa = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioSinEmpresa));

        assertThatThrownBy(() -> service.listar(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }
}
