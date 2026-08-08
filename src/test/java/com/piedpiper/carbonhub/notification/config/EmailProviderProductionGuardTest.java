package com.piedpiper.carbonhub.notification.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailProviderProductionGuardTest {

    @Test
    void aceptaProveedorReal() {
        assertThatCode(() -> new EmailProviderProductionGuard("gmail").validar())
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaStubEnProduccion() {
        assertThatThrownBy(() -> new EmailProviderProductionGuard("stub").validar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no esta permitido");
    }
}
