package com.piedpiper.carbonhub.auditoria.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Resultado final que emite el auditor despues de cargar el reporte.
 *
 * <p>{@code resultado} llega como texto para devolver 422 con un mensaje de dominio cuando el
 * valor no sea reconocido, igual que {@link DecisionAuditorRequestDTO}.</p>
 *
 * <p>Los otros dos campos son condicionales entre si: {@code fechaVencimientoCert} es obligatoria
 * al aprobar y {@code observaciones} al devolver con observaciones. Bean Validation no puede
 * expresar "obligatorio segun el valor de otro campo", asi que cual de los dos exigir lo decide
 * {@code ResultadoAuditoriaService} una vez que sabe cual resultado llego. Anotarlos como
 * {@code @NotNull} aca haria imposible enviar cualquiera de los dos resultados.</p>
 *
 * <p><strong>Brecha aceptada:</strong> a diferencia de {@code resultado}, {@code fechaVencimientoCert}
 * si se declara como {@link LocalDate}. Una fecha con formato invalido (no ausente, sino ilegible)
 * falla en la deserializacion de Jackson y sale como 400 generico, no como el 422 de dominio que
 * devuelven {@code fechaVencimientoCertRequerida()} y sus hermanas. Se acepta porque el unico
 * cliente es un selector de fecha que siempre emite ISO; tratarla como texto para ganar ese 422
 * obligaria a parsearla a mano sin ningun caso real que lo justifique.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAuditoriaRequestDTO {

    @NotBlank(message = "Indica si la auditoria queda aprobada o con observaciones.")
    private String resultado;

    private String observaciones;

    private LocalDate fechaVencimientoCert;
}
