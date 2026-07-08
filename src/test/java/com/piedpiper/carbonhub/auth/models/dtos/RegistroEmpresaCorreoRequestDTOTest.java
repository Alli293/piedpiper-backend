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

    private RegistroEmpresaCorreoRequestDTO dtoValido() {
        return new RegistroEmpresaCorreoRequestDTO(
                "Acme S.A.", "MANUFACTURA", "CR", 50,
                "contacto@acme.com", "Ana Perez", "admin@acme.com",
                "clave123", "clave123", true);
    }

    @Test
    void dtoValido_noTieneViolaciones() {
        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones =
                validator.validate(dtoValido());

        assertThat(violaciones).isEmpty();
    }

    @Test
    void nombreEmpresaVacio_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setNombreEmpresa("");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa el nombre de la empresa.");
    }

    @Test
    void nombreEmpresaMuyCorto_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setNombreEmpresa("A");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa el nombre de la empresa.");
    }

    @Test
    void correoCorporativoConFormatoInvalido_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setCorreoCorporativo("no-es-un-correo");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa un correo electrónico válido");
    }

    @Test
    void emailAdminConFormatoInvalido_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setEmailAdmin("no-es-un-correo");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa un correo electrónico válido");
    }

    @Test
    void nombreAdminVacio_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setNombreAdmin("");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa el nombre del administrador.");
    }

    @Test
    void nombreAdminMuyCorto_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setNombreAdmin("A");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa el nombre del administrador.");
    }

    @Test
    void contrasenaQueNoCumplePatron_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setContrasena("sololetras");
        dto.setConfirmarContrasena("sololetras");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("La contraseña debe tener al menos 8 caracteres, con una letra y un número.");
    }

    @Test
    void confirmarContrasenaDistinta_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setConfirmarContrasena("otraClave123");

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Las contraseñas no coinciden.");
    }

    @Test
    void aceptaTerminosFalso_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setAceptaTerminos(false);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Debes aceptar los Términos y Condiciones y la Política de Privacidad.");
    }

    @Test
    void cantidadEmpleadosCero_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setCantidadEmpleados(0);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa un número de empleados mayor que 0.");
    }

    @Test
    void cantidadEmpleadosNegativa_generaViolacion() {
        RegistroEmpresaCorreoRequestDTO dto = dtoValido();
        dto.setCantidadEmpleados(-5);

        Set<ConstraintViolation<RegistroEmpresaCorreoRequestDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("Ingresa un número de empleados mayor que 0.");
    }
}
