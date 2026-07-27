package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ejercita la consulta del directorio contra una base real. Los tests de servicio mockean el
 * repositorio, así que no detectan errores de la propia query: el caso sin término de búsqueda
 * llegó a producir un fallo en Postgres porque el parámetro nulo quedaba sin tipo inferible.
 */
@DataJpaTest
class PerfilAuditorRepositoryIntegrationTest {

    private static final Set<EspecialidadAuditor> TODAS = EnumSet.allOf(EspecialidadAuditor.class);

    @Autowired
    private PerfilAuditorRepository perfilAuditorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void buscarDirectorioSinTerminoDeBusquedaDevuelveLosAuditoresActivos() {
        perfilAuditorRepository.saveAndFlush(perfilDe(usuarioAuditor("Ana", "Mora")));

        Page<PerfilAuditor> resultado = perfilAuditorRepository.buscarDirectorio(
                Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO, null,
                null, null, false, false, TODAS, PageRequest.of(0, 12));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getAuditor().getNombre()).isEqualTo("Ana");
    }

    @Test
    void buscarDirectorioConTerminoFiltraPorNombre() {
        perfilAuditorRepository.saveAndFlush(perfilDe(usuarioAuditor("Ana", "Mora")));
        perfilAuditorRepository.saveAndFlush(perfilDe(usuarioAuditor("Luis", "Rojas")));

        Page<PerfilAuditor> resultado = perfilAuditorRepository.buscarDirectorio(
                Rol.AUDITOR_CERTIFICADO, EstadoUsuario.ACTIVO, "roja",
                null, null, false, false, TODAS, PageRequest.of(0, 12));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getAuditor().getNombre()).isEqualTo("Luis");
    }

    private Usuario usuarioAuditor(String nombre, String apellidos) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(nombre.toLowerCase() + "@auditores.cr")
                .nombre(nombre)
                .apellidos(apellidos)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build());
    }

    private PerfilAuditor perfilDe(Usuario auditor) {
        return PerfilAuditor.builder()
                .auditor(auditor)
                .disponible(true)
                .auditoriasCompletadas(3)
                .totalResenas(2)
                .build();
    }
}
