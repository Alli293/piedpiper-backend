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

class RegistroUsuarioCorreoRequestDTOTest {

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

    private RegistroUsuarioCorreoRequestDTO dtoValido() {
        return new RegistroUsuarioCorreoRequestDTO(
                "Ana", "Perez", "ana.perez@example.com", "clave123", "clave123", true);
    }

    @Test
    void dtoValido_noTieneViolaciones() {
        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones =
                validator.validate(dtoValido());

        assertThat(violaciones).isEmpty();
    }

    @Test
    void nombreVacio_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setNombre("");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa tu nombre.");
    }

    @Test
    void nombreMuyCorto_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setNombre("A");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa tu nombre.");
    }

    @Test
    void apellidosVacio_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setApellidos("");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa tus apellidos.");
    }

    @Test
    void apellidosMuyCorto_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setApellidos("A");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa tus apellidos.");
    }

    @Test
    void emailConFormatoInvalido_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setEmail("no-es-un-correo");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa un correo electrónico válido");
    }

    @Test
    void contrasenaMuyCorta_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setContrasena("abc1");
        dto.setConfirmarContrasena("abc1");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("La contraseña debe tener al menos 8 caracteres, con una letra y un número.");
    }

    @Test
    void contrasenaSinNumero_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setContrasena("sololetras");
        dto.setConfirmarContrasena("sololetras");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("La contraseña debe tener al menos 8 caracteres, con una letra y un número.");
    }

    @Test
    void contrasenaSinLetra_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setContrasena("12345678");
        dto.setConfirmarContrasena("12345678");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("La contraseña debe tener al menos 8 caracteres, con una letra y un número.");
    }

    @Test
    void confirmarContrasenaDistinta_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setConfirmarContrasena("otraClave123");

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Las contraseñas no coinciden.");
    }

    @Test
    void aceptaTerminosFalso_generaViolacion() {
        RegistroUsuarioCorreoRequestDTO dto = dtoValido();
        dto.setAceptaTerminos(false);

        Set<ConstraintViolation<RegistroUsuarioCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Debes aceptar los Términos y Condiciones y la Política de Privacidad.");
    }
}
