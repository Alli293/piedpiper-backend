package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: PP-54-gestion-especialidades-auditor, Property 4: Specialties cardinality validation
// Validates: Requirements 3.1
class SpecialtiesCardinalityPropertyTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static final List<String> VALID_ESPECIALIDADES = Arrays.stream(EspecialidadAuditor.values())
            .map(Enum::name)
            .toList();

    @Provide
    Arbitrary<List<String>> emptyEspecialidades() {
        return Arbitraries.just(List.of());
    }

    @Provide
    Arbitrary<List<String>> tooManyEspecialidades() {
        return Arbitraries.integers().between(9, 20)
                .flatMap(size -> Arbitraries.of(VALID_ESPECIALIDADES)
                        .list().ofSize(size));
    }

    @Provide
    Arbitrary<List<String>> validEspecialidades() {
        return Arbitraries.integers().between(1, 8)
                .flatMap(size -> Arbitraries.of(VALID_ESPECIALIDADES)
                        .list().ofSize(size));
    }

    @Property
    void emptyEspecialidades_shouldBeRejected(@ForAll("emptyEspecialidades") List<String> especialidades) {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                especialidades,
                List.of("SAN_JOSE"),
                true,
                "Descripción válida"
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> especialidadesMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("especialidades"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(especialidadesMessages)
                .contains("Seleccione al menos una especialidad.");
    }

    @Property
    void tooManyEspecialidades_shouldBeRejected(@ForAll("tooManyEspecialidades") List<String> especialidades) {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                especialidades,
                List.of("SAN_JOSE"),
                true,
                "Descripción válida"
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> especialidadesMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("especialidades"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(especialidadesMessages)
                .contains("Puede seleccionar un máximo de 8 especialidades.");
    }

    @Property
    void validCardinalityEspecialidades_shouldPass(@ForAll("validEspecialidades") List<String> especialidades) {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                especialidades,
                List.of("SAN_JOSE"),
                true,
                "Descripción válida"
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> especialidadesMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("especialidades"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(especialidadesMessages).isEmpty();
    }
}
