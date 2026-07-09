package com.piedpiper.carbonhub.empresa.models.entities;

import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.time.Instant;
import java.util.regex.Pattern;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern GUIONES_REPETIDOS = Pattern.compile("-{2,}");
    private static final Pattern GUIONES_EXTREMOS = Pattern.compile("^-+|-+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreEmpresa;

    @Column(nullable = false, unique = true)
    private String cedulaJuridica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectorIndustrial sectorIndustrial;

    @Column(nullable = false)
    private String pais;

    @Column(nullable = true)
    private Integer cantidadEmpleados;

    @Column(nullable = false, unique = true)
    private String correoCorporativo;

    @Column(nullable = true)
    private String sitioWeb;

    @Column(nullable = true)
    private String logoUrl;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = true)
    private String descripcion;

    @Column(nullable = true)
    private String nivelEcologico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEmpresa estado;

    @Column(nullable = false)
    private Instant fechaRegistro;

    public static String generarSlug(String nombreEmpresa) {
        if (nombreEmpresa == null) {
            return null;
        }
        String sinAcentos = Normalizer.normalize(nombreEmpresa, Normalizer.Form.NFD);
        sinAcentos = DIACRITICOS.matcher(sinAcentos).replaceAll("");
        String normalizado = NO_ALFANUMERICO.matcher(sinAcentos.toLowerCase()).replaceAll("-");
        normalizado = GUIONES_REPETIDOS.matcher(normalizado).replaceAll("-");
        return GUIONES_EXTREMOS.matcher(normalizado).replaceAll("");
    }
}
