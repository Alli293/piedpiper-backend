package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba, con transacciones reales de Spring (no mocks), que la cuota reservada por
 * {@link ItinerarioCuotaService} sobrevive el rollback de una transacción externa que falla
 * después — el bug que reportó nanoulloa en la revisión del PR #55: el test unitario original
 * construía el servicio a mano y nunca pasaba por el proxy transaccional de Spring, por lo que
 * nunca podía detectar un rollback real.
 */
@DataJpaTest
@Import({
        ItinerarioCuotaService.class,
        ItinerarioCuotaServiceIntegrationTest.SimulacionFalloConfig.class
})
class ItinerarioCuotaServiceIntegrationTest {

    @Autowired
    private ItinerarioCuotaService cuotaService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PreferenciasViajeRepository preferenciasViajeRepository;
    @Autowired
    private SimulacionDeGeneracionFallida simulacionDeGeneracionFallida;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void laCuotaReservadaSobreviveElRollbackDeLaTransaccionQueLlamaALaIA() {
        Usuario usuario = usuarioRepository.saveAndFlush(usuarioActivo());
        preferenciasViajeRepository.saveAndFlush(preferenciasDe(usuario));

        assertThatThrownBy(() -> simulacionDeGeneracionFallida.reservarYFallarComoSiLaIAHubieraFallado(usuario.getId()))
                .isInstanceOf(RuntimeException.class);

        PreferenciasViaje recargadas = preferenciasViajeRepository.findByUsuario_Id(usuario.getId()).orElseThrow();
        assertThat(recargadas.getItinerarioGeneracionContador()).isEqualTo(1);
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .email("viajero.ecoruta@carbonhub.test")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private PreferenciasViaje preferenciasDe(Usuario usuario) {
        return PreferenciasViaje.builder()
                .usuario(usuario)
                .cantidadDias(2)
                .fechaInicio(LocalDate.now().plusDays(10))
                .tipoViaje(TipoViaje.INDIVIDUAL)
                .itinerarioGeneracionContador(0)
                .build();
    }

    /**
     * Representa el método {@code EcoRutaItinerarioService.generar(...)} tal como quedó tras el
     * fix: reserva la cuota y luego "llama a la IA" (acá, simplemente falla) — sin
     * {@code @Transactional} propio, para no envolver la reserva de cuota en la misma transacción
     * que podría hacer rollback.
     */
    interface SimulacionDeGeneracionFallida {
        void reservarYFallarComoSiLaIAHubieraFallado(UUID usuarioId);
    }

    @TestConfiguration
    static class SimulacionFalloConfig {

        @Bean
        SimulacionDeGeneracionFallida simulacionDeGeneracionFallida(ItinerarioCuotaService cuotaService) {
            return new SimulacionDeGeneracionFallidaImpl(cuotaService);
        }

        @Service
        static class SimulacionDeGeneracionFallidaImpl implements SimulacionDeGeneracionFallida {

            private final ItinerarioCuotaService cuotaService;

            SimulacionDeGeneracionFallidaImpl(ItinerarioCuotaService cuotaService) {
                this.cuotaService = cuotaService;
            }

            /**
             * {@code @Transactional} a propósito: simula el escenario original del bug, donde
             * todo el flujo (reserva de cuota + llamada a la IA) corría bajo una misma transacción
             * que hacía rollback completo si la IA fallaba. Si {@link ItinerarioCuotaService}
             * no reservara en su propia transacción {@code REQUIRES_NEW}, este rollback también
             * deshacría el incremento del contador.
             */
            @Override
            @Transactional
            public void reservarYFallarComoSiLaIAHubieraFallado(UUID usuarioId) {
                cuotaService.reservarGeneracion(usuarioId);
                throw new RuntimeException("Simula el fallo de la llamada a Gemini");
            }
        }
    }
}
