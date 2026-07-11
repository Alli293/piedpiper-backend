package com.piedpiper.carbonhub.user.models.entities;

import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.enums.Unidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

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

    // Preferencias de interfaz (PP-31). Se almacenan como texto para que un
    // valor que deje de estar soportado pueda resolverse al default sin error.
    @Column(length = 20)
    @Builder.Default
    private String idioma = Idioma.POR_DEFECTO.name();

    @Column(length = 20)
    @Builder.Default
    private String moneda = Moneda.POR_DEFECTO.name();

    @Column(length = 20)
    @Builder.Default
    private String unidades = Unidades.POR_DEFECTO.name();

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    public static String recortarNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.length() > NOMBRE_MAX ? nombre.substring(0, NOMBRE_MAX) : nombre;
    }
}
