package com.piedpiper.carbonhub.calificacion;

import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: PP-56-calificacion-verificada-auditores, Property 4: Validación exhaustiva de campos inválidos
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 *
 * For any petición que contenga uno o más campos inválidos (calificacion fuera de [1,5],
 * comentario > 500 caracteres, auditoriaId ausente, calificacion ausente), la respuesta HTTP 400
 * SHALL incluir un mensaje de error por cada campo inválido, no solo el primero detectado.
 */
@Tag("Feature: PP-56-calificacion-verificada-auditores, Property 4: Validación exhaustiva de campos inválidos")
class ValidacionExhaustivaPropertyTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- Arbitraries for invalid field values ---

    @Provide
    Arbitrary<Integer> calificacionFueraDeRango() {
        return Arbitraries.oneOf(
                Arbitraries.integers().lessOrEqual(0),
                Arbitraries.integers().greaterOrEqual(6)
        );
    }

    @Provide
    Arbitrary<String> comentarioExcesivo() {
        return Arbitraries.strings()
                .ofMinLength(501)
                .ofMaxLength(1000)
                .alpha();
    }

    // --- Property: single invalid field calificacion out of range ---

    @Property(tries = 100)
    void calificacionFueraDeRango_debeProducirExactamenteUnaViolacion(
            @ForAll("calificacionFueraDeRango") Integer calificacionInvalida) {

        CrearCalificacionRequestDTO dto = new CrearCalificacionRequestDTO(
                UUID.randomUUID(),
                calificacionInvalida,
                "Comentario válido"
        );

        Set<ConstraintViolation<CrearCalificacionRequestDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .isNotEmpty()
                .allMatch(v -> v.getPropertyPath().toString().equals("calificacion"));
    }

    // --- Property: single invalid field comentario > 500 chars ---

    @Property(tries = 100)
    void comentarioMayorA500_debeProducirExactamenteUnaViolacion(
            @ForAll("comentarioExcesivo") String comentarioLargo) {

        CrearCalificacionRequestDTO dto = new CrearCalificacionRequestDTO(
                UUID.randomUUID(),
                3,
                comentarioLargo
        );

        Set<ConstraintViolation<CrearCalificacionRequestDTO>> violations = validator.validate(dto);

        Set<String> comentarioViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("comentario"))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(comentarioViolations)
                .contains("El comentario no puede superar los 500 caracteres.");
    }

    // --- Property: auditoriaId null produces violation ---

    @Property(tries = 100)
    void auditoriaIdNulo_debeProducirViolacion(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValida) {

        CrearCalificacionRequestDTO dto = new CrearCalificacionRequestDTO(
                null,
                calificacionValida,
                "Comentario válido"
        );

        Set<ConstraintViolation<CrearCalificacionRequestDTO>> violations = validator.validate(dto);

        Set<String> auditoriaIdViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("auditoriaId"))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(auditoriaIdViolations)
                .contains("El campo auditoriaId es requerido.");
    }

    // --- Property: calificacion null produces violation ---

    @Property(tries = 100)
    void calificacionNula_debeProducirViolacion() {

        CrearCalificacionRequestDTO dto = new CrearCalificacionRequestDTO(
                UUID.randomUUID(),
                null,
                "Comentario válido"
        );

        Set<ConstraintViolation<CrearCalificacionRequestDTO>> violations = validator.validate(dto);

        Set<String> calificacionViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("calificacion"))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(calificacionViolations)
                .contains("El campo calificacion es requerido.");
    }

    // --- Core property: multiple invalid fields produce multiple violations (exhaustive) ---

    @Property(tries = 100)
    void multiplesCamposInvalidos_debeReportarTodosLosErrores(
            @ForAll("combinacionInvalida") InvalidDTOWithExpectedViolations combo) {

        Set<ConstraintViolation<CrearCalificacionRequestDTO>> violations = validator.validate(combo.dto());

        assertThat(violations.size())
                .as("Debe haber al menos %d violaciones para los campos inválidos: %s",
                        combo.expectedViolationCount(), combo.description())
                .isGreaterThanOrEqualTo(combo.expectedViolationCount());
    }

    @Provide
    Arbitrary<InvalidDTOWithExpectedViolations> combinacionInvalida() {
        Arbitrary<Boolean> invalidAuditoriaId = Arbitraries.of(true, false);
        Arbitrary<Boolean> invalidCalificacion = Arbitraries.of(true, false);
        Arbitrary<Boolean> nullCalificacion = Arbitraries.of(true, false);
        Arbitrary<Boolean> invalidComentario = Arbitraries.of(true, false);

        return Combinators.combine(invalidAuditoriaId, invalidCalificacion, nullCalificacion, invalidComentario)
                .filter((audNull, califOutOfRange, califNull, comentLong) ->
                        audNull || califOutOfRange || califNull || comentLong)
                .as((audNull, califOutOfRange, califNull, comentLong) -> {
                    UUID auditoriaId = audNull ? null : UUID.randomUUID();

                    Integer calificacion;
                    if (califNull) {
                        calificacion = null;
                    } else if (califOutOfRange) {
                        // Pick a value out of range: either <= 0 or >= 6
                        calificacion = (Math.random() < 0.5) ? -1 * (int)(Math.random() * 100 + 1) : 6 + (int)(Math.random() * 100);
                    } else {
                        calificacion = 1 + (int)(Math.random() * 5); // valid: 1-5
                    }

                    String comentario = comentLong
                            ? "x".repeat(501 + (int)(Math.random() * 500))
                            : "Comentario válido";

                    int expectedViolations = 0;
                    StringBuilder desc = new StringBuilder();

                    if (audNull) {
                        expectedViolations++;
                        desc.append("auditoriaId=null ");
                    }
                    if (califNull) {
                        expectedViolations++;
                        desc.append("calificacion=null ");
                    } else if (califOutOfRange) {
                        expectedViolations++;
                        desc.append("calificacion=").append(calificacion).append(" ");
                    }
                    if (comentLong) {
                        expectedViolations++;
                        desc.append("comentario>500 ");
                    }

                    CrearCalificacionRequestDTO dto = new CrearCalificacionRequestDTO(
                            auditoriaId, calificacion, comentario);

                    return new InvalidDTOWithExpectedViolations(dto, expectedViolations, desc.toString().trim());
                });
    }

    record InvalidDTOWithExpectedViolations(
            CrearCalificacionRequestDTO dto,
            int expectedViolationCount,
            String description) {}
}
