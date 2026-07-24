package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: PP-54-gestion-especialidades-auditor, Property 7: Description length validation
// Validates: Requirements 3.6
class DescriptionLengthPropertyTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Provide
    Arbitrary<String> tooLongDescriptions() {
        return Arbitraries.strings().ofMinLength(501).ofMaxLength(1000);
    }

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings().ofMinLength(0).ofMaxLength(500);
    }

    @Property
    void descriptionLongerThan500_shouldBeRejected(@ForAll("tooLongDescriptions") String description) {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of("AGROINDUSTRIA"),
                List.of("SAN_JOSE"),
                true,
                description
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> descriptionMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("descripcionProfesional"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(descriptionMessages)
                .contains("La descripción no puede superar los 500 caracteres.");
    }

    @Property
    void descriptionWithin500_shouldPass(@ForAll("validDescriptions") String description) {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of("AGROINDUSTRIA"),
                List.of("SAN_JOSE"),
                true,
                description
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> descriptionMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("descripcionProfesional"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(descriptionMessages).isEmpty();
    }

    @Property
    void nullDescription_shouldPass() {
        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of("AGROINDUSTRIA"),
                List.of("SAN_JOSE"),
                true,
                null
        );

        Set<ConstraintViolation<ActualizarPerfilAuditorRequestDTO>> violations = validator.validate(dto);

        Set<String> descriptionMessages = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("descripcionProfesional"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(descriptionMessages).isEmpty();
    }
}
