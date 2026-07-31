package com.piedpiper.carbonhub.user.models.entities;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.enums.UnidadesMedida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    public static final int NOMBRE_MAX = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = NOMBRE_MAX)
    private String nombre;

    @Column(length = NOMBRE_MAX)
    private String apellidos;

    @Column(name = "nombre_visible", length = NOMBRE_MAX)
    private String nombreVisible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoUsuario estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_auth", nullable = false, length = 20)
    private MetodoAuth metodoAuth;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "configuracion_completa", nullable = false)
    @Builder.Default
    private boolean configuracionCompleta = false;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "token_verificacion_hash", unique = true)
    private String tokenVerificacionHash;

    @Column(name = "token_verificacion_expiracion")
    private Instant tokenVerificacionExpiracion;

    @Column(name = "reenvio_verificacion_contador", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int reenvioVerificacionContador = 0;

    @Column(name = "reenvio_verificacion_ventana_inicio")
    private Instant reenvioVerificacionVentanaInicio;

    @Column(name = "token_reset_hash", unique = true)
    private String tokenResetHash;

    @Column(name = "token_reset_expiracion")
    private Instant tokenResetExpiracion;

    @Column(name = "reset_contrasena_contador", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int resetContrasenaContador = 0;

    @Column(name = "reset_contrasena_ventana_inicio")
    private Instant resetContrasenaVentanaInicio;

    @Column(length = 20)
    @Builder.Default
    private String idioma = Idioma.POR_DEFECTO.name();

    @Column(length = 10)
    @Builder.Default
    private String moneda = Moneda.POR_DEFECTO.name();

    @Column(length = 20)
    @Builder.Default
    private String unidades = UnidadesMedida.POR_DEFECTO.name();

    /**
     * Nombre para mostrar: nombre y apellidos, con {@code nombreVisible} como respaldo cuando el
     * usuario no tiene esos campos (por ejemplo si entró por Google). Vive acá para que los correos
     * y las respuestas de la API no armen el nombre cada uno a su manera y muestren al mismo
     * auditor con dos formatos distintos.
     */
    /**
     * Nombre para mostrar, nunca nulo: cae a {@code nombreVisible} y, si ese tampoco esta, a la
     * parte local del correo. Los consumidores lo meten en DTOs y en el cuerpo de correos, donde un
     * nulo se vuelve un "null" impreso o una linea vacia, asi que la garantia vive aca y no en cada
     * llamador.
     */
    public String nombreCompleto() {
        String armado = Stream.of(nombre, apellidos)
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        if (!armado.isBlank()) {
            return armado;
        }
        if (nombreVisible != null && !nombreVisible.isBlank()) {
            return nombreVisible;
        }
        return email == null ? "" : email.split("@")[0];
    }

    public static String recortarNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.length() > NOMBRE_MAX ? nombre.substring(0, NOMBRE_MAX) : nombre;
    }

    @PrePersist
    @PreUpdate
    void normalizarEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
