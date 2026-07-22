package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.PreferenciasViajeMapper;
import com.piedpiper.carbonhub.ecoruta.mappers.PreferenciasViajeMapperImpl;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenciasViajeServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private PreferenciasViajeRepository preferenciasViajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private PreferenciasViajeMapper mapper;
    private PreferenciasViajeService service;

    @BeforeEach
    void configurar() {
        mapper = new PreferenciasViajeMapperImpl();
        service = new PreferenciasViajeService(preferenciasViajeRepository, usuarioRepository, mapper);
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("ana.perez@example.com")
                .nombre("Ana")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private PreferenciasViajeRequestDTO requestValido() {
        return new PreferenciasViajeRequestDTO(
                5,
                LocalDate.now().plusDays(10),
                "FAMILIA",
                "MODERADO",
                List.of("NATURALEZA", "AVENTURA"),
                "SAN_JOSE",
                "San José, Costa Rica",
                true,
                null,
                false
        );
    }

    @Test
    void guardarCreaUnRegistroNuevoCuandoNoExisteUnoPrevio() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());
        when(preferenciasViajeRepository.saveAndFlush(any(PreferenciasViaje.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciasViajeResponseDTO response = service.guardar(USUARIO_ID, requestValido());

        assertThat(response.isRecienCreada()).isTrue();
        assertThat(response.isConversacionCompleta()).isTrue();
        assertThat(response.getTipoViaje()).isEqualTo("FAMILIA");
        assertThat(response.getProvinciaPreferida()).isEqualTo("SAN_JOSE");
        assertThat(response.getIntereses()).containsExactly("NATURALEZA", "AVENTURA");
    }

    @Test
    void guardarActualizaElRegistroExistenteEnVezDeCrearUnoNuevo() {
        PreferenciasViaje existente = PreferenciasViaje.builder()
                .id(UUID.randomUUID())
                .usuario(usuario())
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(existente));
        when(preferenciasViajeRepository.saveAndFlush(any(PreferenciasViaje.class)))
                .thenAnswer(i -> i.getArgument(0));

        PreferenciasViajeResponseDTO response = service.guardar(USUARIO_ID, requestValido());

        assertThat(response.isRecienCreada()).isFalse();
        ArgumentCaptor<PreferenciasViaje> captor = ArgumentCaptor.forClass(PreferenciasViaje.class);
        verify(preferenciasViajeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existente.getId());
    }

    @Test
    void guardarConTipoDeViajeFueraDeCatalogoLanza422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        PreferenciasViajeRequestDTO request = requestValido();
        request.setTipoViaje("GRUPO_GRANDE");

        assertThatThrownBy(() -> service.guardar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(preferenciasViajeRepository, never()).saveAndFlush(any());
    }

    @Test
    void guardarConProvinciaFueraDeCatalogoLanza422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        PreferenciasViajeRequestDTO request = requestValido();
        request.setProvinciaPreferida("ZONA_NORTE");

        assertThatThrownBy(() -> service.guardar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(preferenciasViajeRepository, never()).saveAndFlush(any());
    }

    @Test
    void guardarSinProvinciaEsValidoPorSerOpcional() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());
        when(preferenciasViajeRepository.saveAndFlush(any(PreferenciasViaje.class)))
                .thenAnswer(i -> i.getArgument(0));
        PreferenciasViajeRequestDTO request = requestValido();
        request.setProvinciaPreferida(null);

        PreferenciasViajeResponseDTO response = service.guardar(USUARIO_ID, request);

        assertThat(response.getProvinciaPreferida()).isNull();
    }

    @Test
    void guardarConInteresFueraDeCatalogoLanza422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        PreferenciasViajeRequestDTO request = requestValido();
        request.setIntereses(List.of("COMPRAS"));

        assertThatThrownBy(() -> service.guardar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(preferenciasViajeRepository, never()).saveAndFlush(any());
    }

    @Test
    void guardarConFalloAlGuardarLanzaErrorInterno() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());
        when(preferenciasViajeRepository.saveAndFlush(any(PreferenciasViaje.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo de base de datos"));

        assertThatThrownBy(() -> service.guardar(USUARIO_ID, requestValido()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void obtenerSinRegistroPrevioDevuelveVacio() {
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());

        assertThat(service.obtener(USUARIO_ID)).isEmpty();
    }

    @Test
    void obtenerConRegistroPrevioDevuelveLosDatosGuardados() {
        PreferenciasViaje existente = PreferenciasViaje.builder()
                .id(UUID.randomUUID())
                .usuario(usuario())
                .cantidadDias(5)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje.FAMILIA)
                .intereses(List.of(com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico.NATURALEZA))
                .buscarCercaDeMi(true)
                .build();
        when(preferenciasViajeRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.of(existente));

        Optional<PreferenciasViajeResponseDTO> response = service.obtener(USUARIO_ID);

        assertThat(response).isPresent();
        assertThat(response.get().getTipoViaje()).isEqualTo("FAMILIA");
        assertThat(response.get().isConversacionCompleta()).isTrue();
    }
}
