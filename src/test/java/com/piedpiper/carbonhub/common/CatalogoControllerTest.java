package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void listarEspecialidadesDevuelve200ConTodosLosValores() throws Exception {
        mockMvc.perform(get("/api/catalogos/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(EspecialidadAuditor.values().length))
                .andExpect(jsonPath("$[0].valor").isNotEmpty())
                .andExpect(jsonPath("$[0].etiqueta").isNotEmpty());
    }

    @Test
    void listarZonasDevuelve200ConTodasLasProvincias() throws Exception {
        mockMvc.perform(get("/api/catalogos/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ProvinciaCR.values().length))
                .andExpect(jsonPath("$[0].valor").isNotEmpty())
                .andExpect(jsonPath("$[0].etiqueta").isNotEmpty());
    }
}
