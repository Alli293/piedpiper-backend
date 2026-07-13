package com.piedpiper.carbonhub.auth.models.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroEmpresaCorreoRequestDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidator() {
        factory.close();
    }

    @Test
    void dtoCompletamenteValido_sinViolaciones() {
        RegistroEmpresaCorreoRequestDTO dto = new RegistroEmpresaCorreoRequestDTO(
                "Ana", "Perez", "admin@acme.com", "clave123", "clave123", true);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones).isEmpty();
    }

    @Test
    void variosCamposInvalidos_detectaTodasLasViolacionesEsperadas() {
        RegistroEmpresaCorreoRequestDTO dto = new RegistroEmpresaCorreoRequestDTO(
                "", "", "no-es-un-correo", "abc", "xyz", false);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "Ingresa el nombre del administrador.",
                        "Ingresa los apellidos del administrador.",
                        "Ingresa un correo electrónico válido",
                        "La contraseña debe tener al menos 8 caracteres, con una letra y un número.",
                        "Las contraseñas no coinciden.",
                        "Debes aceptar los Términos y Condiciones y la Política de Privacidad.");
    }

    @Test
    void contrasenaConMasDe72Caracteres_violaLimiteMaximo() {
        String contrasenaLarga = "a1".repeat(37);
        RegistroEmpresaCorreoRequestDTO dto = new RegistroEmpresaCorreoRequestDTO(
                "Ana", "Perez", "admin@acme.com", contrasenaLarga, contrasenaLarga, true);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("La contraseña no puede exceder 72 caracteres.");
    }
}
