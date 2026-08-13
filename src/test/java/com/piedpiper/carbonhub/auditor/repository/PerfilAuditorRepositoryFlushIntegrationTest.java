package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba contra una base real el comentario de revisión sobre {@code PerfilAuditorService.crear()}:
 * {@code PerfilAuditor.id} usa {@code GenerationType.UUID} (asignado en memoria, no
 * {@code IDENTITY}), así que {@code save()} no dispara el INSERT de inmediato — Hibernate puede
 * diferirlo hasta el flush/commit de la transacción, y con eso la violación de
 * {@code uk_perfiles_auditor_auditor} no ocurre donde el try/catch la espera. Un test con Mockito
 * no puede detectar esto (el repositorio está mockeado), de ahí que haga falta este test de
 * integración contra una base real. Mismo patrón que {@code MetaRepositoryFlushIntegrationTest}.
 */
@DataJpaTest
class PerfilAuditorRepositoryFlushIntegrationTest {

    @Autowired
    private PerfilAuditorRepository perfilAuditorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void saveNoFallaDeInmediatoParaUnAuditorConPerfilExistente() {
        Usuario auditor = auditorConPerfil();
        PerfilAuditor duplicado = PerfilAuditor.builder().auditor(auditor).build();

        // Con GenerationType.UUID, Hibernate puede diferir el INSERT: save() sola no garantiza que
        // la violacion del unique constraint aparezca aca.
        assertThatCode(() -> perfilAuditorRepository.save(duplicado)).doesNotThrowAnyException();
    }

    @Test
    void saveAndFlushFallaDeInmediatoParaUnAuditorConPerfilExistente() {
        Usuario auditor = auditorConPerfil();
        PerfilAuditor duplicado = PerfilAuditor.builder().auditor(auditor).build();

        // saveAndFlush SI fuerza el INSERT dentro de esta llamada: es lo que permite que
        // PerfilAuditorService.crear() capture la violacion del unique constraint en su propio
        // try/catch, en vez de dejarla escapar sin control a un flush posterior.
        assertThatThrownBy(() -> perfilAuditorRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataAccessException.class);
    }

    private Usuario auditorConPerfil() {
        Usuario auditor = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("auditor@correo.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build());
        perfilAuditorRepository.saveAndFlush(PerfilAuditor.builder().auditor(auditor).build());
        return auditor;
    }
}
