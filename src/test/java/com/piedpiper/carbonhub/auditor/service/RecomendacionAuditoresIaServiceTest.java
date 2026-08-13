package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresIaService.CandidatoIa;
import com.piedpiper.carbonhub.common.IaRateLimitService;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El camino feliz (llamada real a Gemini) no se puede probar unitariamente sin montar el
 * {@code ChatClient} completo; lo que sí se prueba aquí, y es lo que importa para la historia, es
 * que ningún fallo escape hacia el controlador y que el prompt no filtre datos de la empresa.
 */
class RecomendacionAuditoresIaServiceTest {

    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private static RecomendacionAuditoresIaService servicio(String apiKey, boolean cuotaDisponible) {
        var builder = mock(org.springframework.ai.chat.client.ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(org.springframework.ai.chat.client.ChatClient.class));
        IaRateLimitService rateLimit = mock(IaRateLimitService.class);
        when(rateLimit.reservar(any())).thenReturn(cuotaDisponible);
        return new RecomendacionAuditoresIaService(builder, rateLimit, apiKey);
    }

    private static List<CandidatoIa> candidatos() {
        return List.of(new CandidatoIa(UUID.randomUUID(), "Ana Mora",
                List.of("Manufactura"), "4.8", List.of("MANUFACTURA"), 12));
    }

    /** Sin API key no se intenta la llamada: se degrada en silencio y el listado sigue saliendo. */
    @Test
    void sinApiKeyDevuelveVacioSinLanzarExcepcion() {
        var resultado = servicio("", true).generarJustificaciones(
                EMPRESA_ID, "MANUFACTURA", "Manufactura", "SAN_JOSE", candidatos());

        assertThat(resultado).isEmpty();
    }

    @Test
    void conLaCuotaDeIaAgotadaDevuelveVacio() {
        var resultado = servicio("clave", false).generarJustificaciones(
                EMPRESA_ID, "MANUFACTURA", "Manufactura", "SAN_JOSE", candidatos());

        assertThat(resultado).isEmpty();
    }

    @Test
    void sinCandidatosNoIntentaLlamarALaIa() {
        var resultado = servicio("clave", true).generarJustificaciones(
                EMPRESA_ID, "MANUFACTURA", "Manufactura", "SAN_JOSE", List.of());

        assertThat(resultado).isEmpty();
    }

    /**
     * Criterio de privacidad de la historia: el prompt se arma solo con datos públicos del auditor
     * y el contexto de búsqueda. Ni el id de la empresa ni su nombre pueden aparecer.
     */
    @Test
    void elPromptNoContieneNiElIdNiElNombreDeLaEmpresa() {
        String prompt = servicio("clave", true).construirPromptUsuario(
                "MANUFACTURA", "Manufactura", "SAN_JOSE", candidatos());

        assertThat(prompt)
                .doesNotContain(EMPRESA_ID.toString())
                .doesNotContain("Acme")
                .contains("MANUFACTURA")
                .contains("Ana Mora");
    }

    /** El id del auditor tampoco viaja: la IA no lo necesita para redactar. */
    @Test
    void elPromptNoContieneElIdDelAuditor() {
        List<CandidatoIa> candidatos = candidatos();

        String prompt = servicio("clave", true).construirPromptUsuario(
                "MANUFACTURA", "Manufactura", "SAN_JOSE", candidatos);

        assertThat(prompt).doesNotContain(candidatos.get(0).auditorId().toString());
    }

    @Test
    void elPromptListaLosDatosPublicosDeCadaCandidato() {
        String prompt = servicio("clave", true).construirPromptUsuario(
                "MANUFACTURA", "Manufactura", "SAN_JOSE", candidatos());

        assertThat(prompt)
                .contains("calificación promedio: 4.8/5")
                .contains("auditorías completadas: 12")
                .contains("especialidades: Manufactura");
    }

    /** Un auditor sin distribución de sectores no puede dejar el prompt a medias. */
    @Test
    void unCandidatoSinSectoresSeDescribeComoSinDatos() {
        List<CandidatoIa> sinSectores = List.of(new CandidatoIa(
                UUID.randomUUID(), "Luis Rojas", List.of("Manufactura"), "sin calificaciones",
                List.of(), 0));

        String prompt = servicio("clave", true).construirPromptUsuario(
                "MANUFACTURA", "Manufactura", "SAN_JOSE", sinSectores);

        assertThat(prompt).contains("sectores auditados con mayor frecuencia: sin datos");
    }

    @Test
    void elMensajeDeSistemaPideEspanolDosOracionesYNoInventar() {
        assertThat(RecomendacionAuditoresIaService.SYSTEM_MESSAGE)
                .contains("español de Costa Rica")
                .contains("máximo 2 oraciones")
                .contains("No inventes")
                .contains("No menciones nombres de empresas");
    }
}
