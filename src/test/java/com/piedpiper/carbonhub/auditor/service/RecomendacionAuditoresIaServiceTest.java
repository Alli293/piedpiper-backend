package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.service.RecomendacionAuditoresIaService.CandidatoIa;
import com.piedpiper.carbonhub.common.IaRateLimitService;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
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
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        IaRateLimitService rateLimit = mock(IaRateLimitService.class);
        when(rateLimit.reservar(any())).thenReturn(cuotaDisponible);
        return new RecomendacionAuditoresIaService(builder, rateLimit, apiKey);
    }

    private static List<CandidatoIa> candidatos() {
        return List.of(new CandidatoIa(UUID.randomUUID(), "Ana Mora",
                List.of("Manufactura"), "4.8", List.of("MANUFACTURA"), 12));
    }

    private static RecomendacionAuditoresIaService.JustificacionIa justificacion(String nombre,
                                                                                 String texto) {
        return new RecomendacionAuditoresIaService.JustificacionIa(nombre, texto);
    }

    private static Map<UUID, String> emparejar(
            List<CandidatoIa> candidatos,
            List<RecomendacionAuditoresIaService.JustificacionIa> justificaciones) {
        return servicio("clave", true).emparejar(candidatos, justificaciones);
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

    /**
     * Un auditor recién certificado no tiene sectores ni calificación, y ninguna de las dos
     * ausencias puede dejar el prompt a medias.
     *
     * <p>La escala {@code /5} solo va detrás de un número: concatenarla sin mirar producía
     * "calificación promedio: sin calificaciones/5", y por el javadoc del comparador de
     * {@link RecomendacionAuditoresConsultaService} ese es el caso más común en un sistema nuevo.</p>
     */
    @Test
    void unCandidatoSinSectoresNiCalificacionSeDescribeSinTextoRaro() {
        List<CandidatoIa> sinDatos = List.of(new CandidatoIa(
                UUID.randomUUID(), "Luis Rojas", List.of("Manufactura"), null, List.of(), 0));

        String prompt = servicio("clave", true).construirPromptUsuario(
                "MANUFACTURA", "Manufactura", "SAN_JOSE", sinDatos);

        assertThat(prompt)
                .contains("sectores auditados con mayor frecuencia: sin datos")
                .contains("calificación promedio: sin calificaciones,")
                .doesNotContain("sin calificaciones/5");
    }

    /**
     * Lo que protege este bloque: si el modelo devolviera las justificaciones en otro orden, cada
     * auditor recibiría la de otro. Texto plausible, atribuido a quien no es, y sin ninguna señal.
     */
    @Test
    void unaJustificacionQueNoCorrespondeAlCandidatoSeDescarta() {
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10),
                new CandidatoIa(luis, "Luis Rojas", List.of("Manufactura"), "4.0", List.of(), 5));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion("Luis Rojas", "Texto de Luis."),
                justificacion("Ana Mora", "Texto de Ana.")));

        assertThat(resultado).isEmpty();
    }

    @Test
    void cadaJustificacionEnSuPosicionSeAsignaAlAuditorCorrecto() {
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10),
                new CandidatoIa(luis, "Luis Rojas", List.of("Manufactura"), "4.0", List.of(), 5));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion("Ana Mora", "Texto de Ana."),
                justificacion("Luis Rojas", "Texto de Luis.")));

        assertThat(resultado).containsEntry(ana, "Texto de Ana.").containsEntry(luis, "Texto de Luis.");
    }

    /**
     * El caso que un prefijo de cadena no detectaba: "ana morales" empieza con "ana mora", asi que
     * comparando cadenas se daba por bueno el cruce entre dos personas distintas. Con apellidos
     * compuestos es habitual, y es exactamente lo que este control existe para impedir.
     */
    @Test
    void dosCandidatosCuyoNombreEmpiezaIgualNoSeConfundenEntreSi() {
        UUID ana = UUID.randomUUID();
        UUID anaMorales = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10),
                new CandidatoIa(anaMorales, "Ana Morales", List.of("Manufactura"), "4.0", List.of(), 5));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion("Ana Morales", "Texto de Morales."),
                justificacion("Ana Mora", "Texto de Mora.")));

        assertThat(resultado).isEmpty();
    }

    /**
     * Comprobar solo contra el candidato de esa posicion no alcanza si el nombre es ambiguo entre
     * varios: "Ana" encaja con "Ana Mora" y con "Ana Solis", asi que aceptarlo seria confiar otra
     * vez en el orden. Ante ambiguedad se descarta.
     */
    @Test
    void unNombreQueEncajaConVariosCandidatosSeDescartaPorAmbiguo() {
        UUID mora = UUID.randomUUID();
        UUID solis = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(mora, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10),
                new CandidatoIa(solis, "Ana Solis", List.of("Manufactura"), "4.0", List.of(), 5));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion("Ana", "Texto ambiguo."),
                justificacion("Ana", "Otro texto ambiguo.")));

        assertThat(resultado).isEmpty();
    }

    /** Con un solo candidato el nombre corto no es ambiguo, asi que el texto se aprovecha. */
    @Test
    void unNombreCortoConUnSoloCandidatoSiSeAsigna() {
        UUID ana = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(justificacion("Ana", "Texto.")));

        assertThat(resultado).containsEntry(ana, "Texto.");
    }

    /** Caso que planteo Andres: nombre de una sola palabra, sin apellidos. */
    @Test
    void unAuditorConNombreDeUnaSolaPalabraSeEmparejaIgual() {
        UUID rojas = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(rojas, "Rojas", List.of("Manufactura"), "4.5", List.of(), 3));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(justificacion("Rojas", "Texto.")));

        assertThat(resultado).containsEntry(rojas, "Texto.");
    }

    /** Y si el modelo lo devuelve truncado a una letra, no se atribuye nada. */
    @Test
    void unNombreTruncadoQueNoCoincideSeDescarta() {
        UUID rojas = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(rojas, "Rojas", List.of("Manufactura"), "4.5", List.of(), 3));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(justificacion("R", "Texto.")));

        assertThat(resultado).isEmpty();
    }

    /** El modelo suele acortar el nombre completo; descartar por eso tiraria texto bueno. */
    @Test
    void unNombreAcortadoOConTildesDistintasSigueContandoComoElMismoAuditor() {
        UUID ana = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora Vargas", List.of("Manufactura"), "4.8", List.of(), 10));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion("ana mora", "Texto de Ana.")));

        assertThat(resultado).containsEntry(ana, "Texto de Ana.");
    }

    /** Si el modelo no devuelve el nombre no hay cruce que detectar, y el texto se aprovecha. */
    @Test
    void siLaRespuestaNoTraeNombreLaJustificacionSeAsignaIgual() {
        UUID ana = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(
                justificacion(null, "Texto de Ana.")));

        assertThat(resultado).containsEntry(ana, "Texto de Ana.");
    }

    @Test
    void unaJustificacionVaciaNoSeAsigna() {
        UUID ana = UUID.randomUUID();
        List<CandidatoIa> candidatos = List.of(
                new CandidatoIa(ana, "Ana Mora", List.of("Manufactura"), "4.8", List.of(), 10));

        Map<UUID, String> resultado = emparejar(candidatos, List.of(justificacion("Ana Mora", "   ")));

        assertThat(resultado).isEmpty();
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
