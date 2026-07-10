package com.piedpiper.carbonhub.auth.models.dtos;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// TODO: al construir el Usuario en el service, usar Usuario.recortarNombre() para el mismo
// recorte a 150 caracteres que se aplica a los claims de Google, y EstadoUsuario.PENDIENTE_VERIFICACION
// como estado inicial (no PENDIENTE_VALIDACION, que es solo para auditor).
public class RegistroEmpresaCorreoRequestDTO {

    @NotBlank(message = "Ingresa el nombre de la empresa.")
    @Size(min = 2, max = 150, message = "Ingresa el nombre de la empresa.")
    private String nombreEmpresa;

    @NotBlank(message = "Ingresa la cédula jurídica de la empresa.")
    @Pattern(regexp = "^\\d-\\d{3}-\\d{6}$",
            message = "Formato de cédula jurídica inválido (ej. 3-101-123456).")
    private String cedulaJuridica;

    @NotNull(message = "Selecciona una opción válida")
    private SectorIndustrial sectorIndustrial;

    @NotBlank(message = "Selecciona una opción válida")
    private String pais;

    @NotNull(message = "Ingresa un número de empleados mayor que 0.")
    @Positive(message = "Ingresa un número de empleados mayor que 0.")
    private Integer cantidadEmpleados;

    @NotBlank(message = "Ingresa un correo electrónico válido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 254, message = "Ingresa un correo electrónico válido")
    private String correoCorporativo;

    @NotBlank(message = "Ingresa el nombre del administrador.")
    @Size(min = 2, max = 100, message = "Ingresa el nombre del administrador.")
    private String nombreAdmin;

    @NotBlank(message = "Ingresa los apellidos del administrador.")
    @Size(min = 2, max = 100, message = "Ingresa los apellidos del administrador.")
    private String apellidosAdmin;

    @NotBlank(message = "Ingresa un correo electrónico válido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 254, message = "Ingresa un correo electrónico válido")
    private String emailAdmin;

    @NotBlank(message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    @Pattern(regexp = RegistroUsuarioCorreoRequestDTO.PATRON_CONTRASENA,
            message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    private String contrasena;

    @NotBlank(message = "Debes confirmar tu contraseña.")
    private String confirmarContrasena;

    @AssertTrue(message = "Debes aceptar los Términos y Condiciones y la Política de Privacidad.")
    private boolean aceptaTerminos;

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isContrasenasCoinciden() {
        return contrasena != null && contrasena.equals(confirmarContrasena);
    }
}
