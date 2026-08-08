package com.piedpiper.carbonhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("contexto")
class ContextoAplicacionTest {

    @Test
    void elContextoCargaConTodosLosBeans() {
    }
}
