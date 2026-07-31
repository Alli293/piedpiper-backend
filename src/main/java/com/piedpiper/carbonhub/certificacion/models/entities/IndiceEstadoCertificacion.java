package com.piedpiper.carbonhub.certificacion.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla contador: cada fila insertada no tiene mas dato que su propio id
 * autoincremental. Su unico proposito es entregar numeros secuenciales para
 * ubicar cada certificacion dentro de la Bitstring Status List de revocacion
 * (W3C). No se modela como {@code @SequenceGenerator} porque JPA solo permite
 * generar valores sobre el {@code @Id} de una entidad, y el {@code @Id} de
 * {@code Certificacion} ya es su UUID; esta es la forma estandar de obtener un
 * contador aparte sin recurrir a SQL nativo (que ademas se comportaria distinto
 * entre Postgres en produccion y H2 en las pruebas).
 */
@Entity
@Table(name = "certificaciones_indice_estado")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndiceEstadoCertificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long indice;
}
