package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.emision.mappers.EmisionComparacionMapper;
import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import com.piedpiper.carbonhub.limite.repository.LimiteEmisionesRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionComparacionServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmisionRepository emisionRepository;
    @Mock
    private LimiteEmisionesRepository limiteEmisionesRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmisionComparacionMapper emisionComparacionMapper;

    @InjectMocks
    private EmisionComparacionService service;

    @BeforeEach
    void setUp() {
        when(emisionComparacionMapper.toDto(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new ComparacionEmisionesResponseDTO(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5)
                ));
    }

    @Test
    void calculaPorcentajeCorrectoCuandoExisteLimite() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(new BigDecimal("30000.000"));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getHuellaAcumuladaT()).isEqualByComparingTo("30.0000");
        assertThat(response.getLimiteT()).isEqualByComparingTo("50.0000");
        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("60.0");
        assertThat(response.getEstado()).isEqualTo("dentro");
    }

    @Test
    void retornaSinLimiteCuandoNoHayLimiteDeclarado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(new BigDecimal("60000.000"));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.empty());

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getHuellaAcumuladaT()).isEqualByComparingTo("60.0000");
        assertThat(response.getLimiteT()).isNull();
        assertThat(response.getPorcentajeConsumido()).isNull();
        assertThat(response.getEstado()).isEqualTo("sin_limite");
    }

    @Test
    void huellaCeroQuedaDentroDelLimite() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(BigDecimal.ZERO);
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("0.0");
        assertThat(response.getEstado()).isEqualTo("dentro");
    }

    @Test
    void porcentajeMayorACienQuedaSuperado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(emisionRepository.sumCarbonKgByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(new BigDecimal("60000.000"));
        when(limiteEmisionesRepository.findByEmpresaIdAndAnio(EMPRESA_ID, 2026))
                .thenReturn(Optional.of(new LimiteEmisiones(EMPRESA_ID, 2026, new BigDecimal("50.0000"))));

        ComparacionEmisionesResponseDTO response = service.comparar(USUARIO_ID, 2026);

        assertThat(response.getPorcentajeConsumido()).isEqualByComparingTo("120.0");
        assertThat(response.getEstado()).isEqualTo("superado");
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
    }
}
