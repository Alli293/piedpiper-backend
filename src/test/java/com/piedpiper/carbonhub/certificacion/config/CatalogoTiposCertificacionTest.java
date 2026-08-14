package com.piedpiper.carbonhub.certificacion.config;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoTiposCertificacionTest {

    private final CatalogoTiposCertificacion catalogo = new CatalogoTiposCertificacion();

    @Test
    void resuelveTodosLosTiposDelCatalogo() {
        assertThat(catalogo.listar()).hasSize(TipoCertificacion.values().length);
        for (TipoCertificacion tipo : TipoCertificacion.values()) {
            assertThat(catalogo.buscar(tipo)).as("falta la definicion de %s", tipo).isPresent();
        }
    }

    @Test
    void cadaTipoDefineSuVigenciaEnMeses() {
        assertThat(vigencia(TipoCertificacion.INVENTARIO_GEI)).isEqualTo(12);
        assertThat(vigencia(TipoCertificacion.REDUCCION_EMISIONES)).isEqualTo(12);
        assertThat(vigencia(TipoCertificacion.REDUCCION_PLUS)).isEqualTo(12);
        assertThat(vigencia(TipoCertificacion.CARBONO_NEUTRAL)).isEqualTo(12);
        assertThat(vigencia(TipoCertificacion.CARBONO_NEUTRAL_PLUS)).isEqualTo(12);
        assertThat(vigencia(TipoCertificacion.ADAPTACION_CLIMATICA)).isEqualTo(24);
        assertThat(vigencia(TipoCertificacion.HUELLA_PRODUCTO)).isEqualTo(36);
        assertThat(vigencia(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL)).isEqualTo(24);
    }

    @Test
    void mapeaCadaTipoAUnAchievementTypeDeOpenBadges() {
        assertThat(tipoLogro(TipoCertificacion.CARBONO_NEUTRAL))
                .isEqualTo(TipoLogroOpenBadges.CERTIFICATION);
        assertThat(tipoLogro(TipoCertificacion.CARBONO_NEUTRAL_PLUS))
                .isEqualTo(TipoLogroOpenBadges.CERTIFICATION);
        assertThat(tipoLogro(TipoCertificacion.INVENTARIO_GEI))
                .isEqualTo(TipoLogroOpenBadges.QUALITY_ASSURANCE_CREDENTIAL);
        assertThat(tipoLogro(TipoCertificacion.HUELLA_PRODUCTO))
                .isEqualTo(TipoLogroOpenBadges.QUALITY_ASSURANCE_CREDENTIAL);
        assertThat(tipoLogro(TipoCertificacion.REDUCCION_EMISIONES))
                .isEqualTo(TipoLogroOpenBadges.CERTIFICATE);
        assertThat(tipoLogro(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL))
                .isEqualTo(TipoLogroOpenBadges.CERTIFICATION);
    }

    @Test
    void todaDefinicionTieneNombreDescripcionYCriterio() {
        assertThat(catalogo.listar()).allSatisfy(definicion -> {
            assertThat(definicion.nombre()).isNotBlank();
            assertThat(definicion.descripcion()).isNotBlank();
            assertThat(definicion.criterio()).isNotBlank();
        });
    }

    @Test
    void buscarConTipoNuloDevuelveVacio() {
        assertThat(catalogo.buscar(null)).isEmpty();
    }

    @Test
    void resuelveElTipoDesdeSuCodigoYRechazaUnoDesconocido() {
        assertThat(TipoCertificacion.desde("carbono_neutral"))
                .contains(TipoCertificacion.CARBONO_NEUTRAL);
        assertThat(TipoCertificacion.desde("CARBONO_NEUTRAL"))
                .contains(TipoCertificacion.CARBONO_NEUTRAL);
        assertThat(TipoCertificacion.desde("excelencia_climatica_empresarial"))
                .contains(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL);
        assertThat(TipoCertificacion.desde("inexistente")).isEmpty();
        assertThat(TipoCertificacion.desde(null)).isEmpty();
    }

    private int vigencia(TipoCertificacion tipo) {
        return definicion(tipo).vigenciaMeses();
    }

    private TipoLogroOpenBadges tipoLogro(TipoCertificacion tipo) {
        return definicion(tipo).tipoLogro();
    }

    private DefinicionCertificacion definicion(TipoCertificacion tipo) {
        Optional<DefinicionCertificacion> definicion = catalogo.buscar(tipo);
        assertThat(definicion).isPresent();
        return definicion.orElseThrow();
    }
}
