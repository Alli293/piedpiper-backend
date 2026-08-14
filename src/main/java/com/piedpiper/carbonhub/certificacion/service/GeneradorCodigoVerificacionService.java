package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Year;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Genera el codigo publico de verificacion de una certificacion, con forma
 * {@code CH-<anio>-<8 caracteres>} (p. ej. {@code CH-2026-8F4A19KD}).
 *
 * <p>El sufijo usa el alfabeto Crockford base32 (sin {@code I}, {@code L},
 * {@code O} ni {@code U}, que se confunden facilmente al transcribirlos a
 * mano desde un certificado impreso). Nunca se deriva de {@code id} ni de
 * ningun dato secuencial: un codigo predecible permitiria enumerar
 * certificaciones ajenas contra el endpoint publico de verificacion.
 */
@Service
public class GeneradorCodigoVerificacionService {

    private static final Logger log = LoggerFactory.getLogger(GeneradorCodigoVerificacionService.class);

    private static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LONGITUD_SUFIJO = 8;
    private static final int INTENTOS_MAXIMOS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final Pattern FORMATO = Pattern.compile("^CH-\\d{4}-[0-9A-HJKMNP-TV-Z]{8}$");

    private final CertificacionRepository certificacionRepository;

    public GeneradorCodigoVerificacionService(CertificacionRepository certificacionRepository) {
        this.certificacionRepository = certificacionRepository;
    }

    public String generar() {
        return generar(certificacionRepository::existsByCodigoVerificacion);
    }

    /**
     * Igual que {@link #generar()} pero contra la unicidad de otro tipo de
     * credencial (p. ej. insignias, ver {@code InsigniaEmpresaConsultaService}):
     * el formato y el algoritmo de generacion son el mismo para toda
     * credencial verificable por codigo corto, solo cambia contra que
     * repositorio se comprueba la colision.
     */
    public String generar(Predicate<String> existeCodigo) {
        for (int intento = 0; intento < INTENTOS_MAXIMOS; intento++) {
            String candidato = "CH-" + Year.now() + "-" + sufijoAleatorio();
            if (!existeCodigo.test(candidato)) {
                return candidato;
            }
        }
        log.error("No fue posible generar un codigo de verificacion unico tras {} intentos.",
                INTENTOS_MAXIMOS);
        throw ApiException.errorInterno("No se pudo generar el codigo de verificacion. Intenta nuevamente.");
    }

    public static boolean formatoValido(String codigo) {
        return codigo != null && FORMATO.matcher(codigo).matches();
    }

    private String sufijoAleatorio() {
        StringBuilder sufijo = new StringBuilder(LONGITUD_SUFIJO);
        for (int i = 0; i < LONGITUD_SUFIJO; i++) {
            sufijo.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sufijo.toString();
    }
}
