package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Prueba de integracion contra Postgres real (requiere DB_URL/DB_USER/DB_PASSWORD, igual que
 * el resto de la suite). Verifica que el @Transactional de RegistroEmpresaCorreoService.registrar()
 * revierte de verdad la fila de Empresa cuando el segundo saveAndFlush (el de Usuario) falla,
 * no solo que el service lanza la excepcion esperada.
 *
 * usuarioRepository.existsByEmail(...) corre contra la BD real (no esta stubeado), mientras que
 * saveAndFlush si se fuerza a fallar, para que la unica diferencia con el flujo real sea el punto
 * de fallo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RegistroEmpresaCorreoServiceIntegrationTest {

    private static final String CORREO_CORPORATIVO = "rollback-test@acme-integracion.test";
    private static final String CEDULA_JURIDICA = "9-999-999999";
    private static final String EMAIL_ADMIN = "admin-rollback-test@acme-integracion.test";

    @Autowired
    private RegistroEmpresaCorreoService service;

    @Autowired
    private EmpresaRepository empresaRepository;

    @MockitoSpyBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    @AfterEach
    void limpiarDatosDePrueba() {
        empresaRepository.findByCorreoCorporativo(CORREO_CORPORATIVO).ifPresent(empresaRepository::delete);
        usuarioRepository.findByEmail(EMAIL_ADMIN).ifPresent(usuarioRepository::delete);
    }

    private RegistroEmpresaCorreoRequestDTO request() {
        return new RegistroEmpresaCorreoRequestDTO(
                "Acme Rollback Integracion", CEDULA_JURIDICA, SectorIndustrial.MANUFACTURA, "CR", 50,
                CORREO_CORPORATIVO, "Ana", "Perez", EMAIL_ADMIN,
                "clave123", "clave123", true);
    }

    @Test
    void siFallaElGuardadoDelUsuario_seRevierteLaFilaDeEmpresaEnLaBaseDeDatos() {
        doThrow(new RuntimeException("fallo simulado al guardar el usuario"))
                .when(usuarioRepository).saveAndFlush(any());

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(empresaRepository.existsByCorreoCorporativo(CORREO_CORPORATIVO)).isFalse();
        assertThat(empresaRepository.existsByCedulaJuridica(CEDULA_JURIDICA)).isFalse();
        assertThat(usuarioRepository.existsByEmail(EMAIL_ADMIN)).isFalse();
    }
}
