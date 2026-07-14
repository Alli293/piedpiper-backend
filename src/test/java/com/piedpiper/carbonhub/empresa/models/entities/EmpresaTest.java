package com.piedpiper.carbonhub.empresa.models.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmpresaTest {

    @Test
    void generarSlug_conAcentos_quitaLosAcentos() {
        String slug = Empresa.generarSlug("Café Solar");

        assertThat(slug).isEqualTo("cafe-solar");
    }

    @Test
    void generarSlug_conEspaciosOSimbolosSeguidos_colapsaGuionesEnUnoSolo() {
        String slug = Empresa.generarSlug("Acme   Solar S.A.");

        assertThat(slug).isEqualTo("acme-solar-s-a");
    }

    @Test
    void generarSlug_conEspacioAlInicioOFinal_recortaGuionesEnLosExtremos() {
        String slug = Empresa.generarSlug("  Acme Solar  ");

        assertThat(slug).isEqualTo("acme-solar");
    }

    @Test
    void generarSlug_nulo_devuelveNulo() {
        String slug = Empresa.generarSlug(null);

        assertThat(slug).isNull();
    }

    @Test
    void generarSlug_vacio_devuelveVacio() {
        String slug = Empresa.generarSlug("");

        assertThat(slug).isEmpty();
    }
}
