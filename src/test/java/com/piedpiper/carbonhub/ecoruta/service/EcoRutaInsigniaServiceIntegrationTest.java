package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.config.CatalogoInsigniasEcoRuta;
import com.piedpiper.carbonhub.ecoruta.mappers.InsigniaUsuarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import com.piedpiper.carbonhub.ecoruta.repository.InsigniaUsuarioRepository;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;

@DataJpaTest
@Import({
        EcoRutaInsigniaService.class,
        CatalogoInsigniasEcoRuta.class,
        EcoRutaInsigniaServiceIntegrationTest.MapperTestConfig.class
})
class EcoRutaInsigniaServiceIntegrationTest {

    private static final Instant FECHA_EVENTO = Instant.parse("2026-07-15T20:32:00Z");

    @Autowired
    private EcoRutaInsigniaService service;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoSpyBean
    private InsigniaUsuarioRepository insigniaUsuarioRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void violacionDeRestriccionUnicaNoPropagaExcepcionAlLlamador() {
        Usuario usuario = usuarioRepository.saveAndFlush(usuarioActivo());
        insigniaUsuarioRepository.saveAndFlush(InsigniaUsuario.builder()
                .usuario(usuario)
                .idInsignia(2L)
                .eventoDesbloqueo(CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE)
                .fechaObtencion(FECHA_EVENTO)
                .build());
        doReturn(false).when(insigniaUsuarioRepository)
                .existsByUsuarioIdAndIdInsignia(usuario.getId(), 2L);

        assertThatNoException().isThrownBy(() -> service.evaluarYOtorgar(
                new EventoCertificacionRequestDTO(usuario.getId(),
                        CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE,
                        FECHA_EVENTO.plusSeconds(60))));
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("usuario.ecoruta@carbonhub.test")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    @TestConfiguration
    static class MapperTestConfig {

        @Bean
        InsigniaUsuarioMapper insigniaUsuarioMapper() {
            return insigniaUsuario -> null;
        }
    }
}
