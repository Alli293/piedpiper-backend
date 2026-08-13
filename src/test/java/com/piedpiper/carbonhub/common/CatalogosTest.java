package com.piedpiper.carbonhub.common;

import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogosTest {

    @Test
    void resolverConjuntoDevuelveElConjuntoResuelto() {
        Set<EspecialidadAuditor> resultado = Catalogos.resolverConjunto(
                EspecialidadAuditor.class, List.of("manufactura", "AGROINDUSTRIA"),
                () -> ApiException.datosInvalidos("duplicados"),
                ApiException::especialidadesInvalidas);

        assertThat(resultado).containsExactlyInAnyOrder(
                EspecialidadAuditor.MANUFACTURA, EspecialidadAuditor.AGROINDUSTRIA);
    }

    @Test
    void resolverConjuntoDetectaDuplicadosPorMayusculasYEspacios() {
        assertThatThrownBy(() -> Catalogos.resolverConjunto(
                EspecialidadAuditor.class, List.of("manufactura", "MANUFACTURA "),
                () -> ApiException.datosInvalidos("La lista contiene duplicados."),
                ApiException::especialidadesInvalidas))
                .isInstanceOf(ApiException.class)
                .hasMessage("La lista contiene duplicados.")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resolverConjuntoRechazaValoresFueraDelCatalogo() {
        assertThatThrownBy(() -> Catalogos.resolverConjunto(
                EspecialidadAuditor.class, List.of("MANUFACTURA", "BUCEO_RECREATIVO"),
                () -> ApiException.datosInvalidos("duplicados"),
                ApiException::especialidadesInvalidas))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getMessage()).contains("BUCEO_RECREATIVO"));
    }
}
