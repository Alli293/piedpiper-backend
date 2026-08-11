package com.piedpiper.carbonhub.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NombresArchivoTest {

    @Test
    void devuelveElNombreTalCualCuandoNoTieneCaracteresPeligrosos() {
        assertThat(NombresArchivo.seguro("cert-vigente.pdf")).isEqualTo("cert-vigente.pdf");
    }

    @Test
    void nullCaeAlNombrePorDefecto() {
        assertThat(NombresArchivo.seguro(null)).isEqualTo(NombresArchivo.NOMBRE_POR_DEFECTO);
    }

    @Test
    void blancoCaeAlNombrePorDefecto() {
        assertThat(NombresArchivo.seguro("   ")).isEqualTo(NombresArchivo.NOMBRE_POR_DEFECTO);
    }

    @Test
    void saltosDeLineaSeReemplazanParaEvitarInyeccionDeCabeceras() {
        String resultado = NombresArchivo.seguro("malo\r\nSet-Cookie: robado=1.pdf");

        assertThat(resultado).doesNotContain("\r").doesNotContain("\n");
        assertThat(resultado).contains("malo__Set-Cookie");
    }

    @Test
    void comillasYBarrasSeReemplazan() {
        String resultado = NombresArchivo.seguro("\"../../etc/passwd\".pdf");

        assertThat(resultado).doesNotContain("\"").doesNotContain("/").doesNotContain("\\");
    }

    @Test
    void unNombreCompuestoSoloPorCaracteresPeligrososSeReemplazaSinQuedarVacio() {
        assertThat(NombresArchivo.seguro("///")).isEqualTo("___");
    }
}
