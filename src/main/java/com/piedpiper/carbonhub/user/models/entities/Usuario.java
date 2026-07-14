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

    // Preferencias de interfaz. Se almacenan como texto (y no como @Enumerated)
    // para que un valor que deje de estar soportado no rompa el mapeo JPA:
    // el servicio lo resuelve contra el catálogo y aplica el valor por defecto.
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String idioma = Idioma.POR_DEFECTO.name();

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String moneda = Moneda.POR_DEFECTO.name();

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unidades = UnidadesMedida.POR_DEFECTO.name();

    public static String recortarNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.length() > NOMBRE_MAX ? nombre.substring(0, NOMBRE_MAX) : nombre;
    }
}
