package com.piedpiper.carbonhub.dashboard.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenHuellaDashboardResponseDTO;
import com.piedpiper.carbonhub.dashboard.service.DashboardHuellaService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardHuellaService dashboardHuellaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void obtenerHuellaMesActualDevuelve200() throws Exception {
        configurarTokenValido();
        when(dashboardHuellaService.obtenerResumen(USUARIO_ID, "mes_actual", 2021))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        new BigDecimal("5.2360"),
                        new BigDecimal("30.9"),
                        true
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .header("Authorization", "Bearer token-valido")
                        .param("periodo", "mes_actual")
                        .param("anio", "2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huellaTotalT").value(5.2360))
                .andExpect(jsonPath("$.variacionPorcentual").value(30.9))
                .andExpect(jsonPath("$.tieneDatos").value(true));

        verify(dashboardHuellaService).obtenerResumen(USUARIO_ID, "mes_actual", 2021);
    }

    @Test
    void periodoInvalidoUsaMesActualPorDefecto() throws Exception {
        configurarTokenValido();
        when(dashboardHuellaService.obtenerResumen(USUARIO_ID, "otro", 2021))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        BigDecimal.ZERO,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .header("Authorization", "Bearer token-valido")
                        .param("periodo", "otro")
                        .param("anio", "2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoSeleccionado").value("mes_actual"))
                .andExpect(jsonPath("$.tieneDatos").value(false));
    }

    @Test
    void periodoOmitidoUsaMesActualPorDefecto() throws Exception {
        configurarTokenValido();
        when(dashboardHuellaService.obtenerResumen(USUARIO_ID, null, null))
                .thenReturn(new ResumenHuellaDashboardResponseDTO(
                        "mes_actual",
                        BigDecimal.ZERO,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/dashboard/huella")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoSeleccionado").value("mes_actual"));

        verify(dashboardHuellaService).obtenerResumen(eq(USUARIO_ID), isNull(), isNull());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/dashboard/huella")
                        .param("periodo", "mes_actual"))
                .andExpect(status().isUnauthorized());
    }

    private void configurarTokenValido() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn(USUARIO_ID.toString());
        when(jwtService.parsear("token-valido")).thenReturn(claims);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(USUARIO_ID)
                .email("admin@carbonhub.test")
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build()));
    }
}
