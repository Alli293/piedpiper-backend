package com.piedpiper.carbonhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring arranca sin errores (todos los beans resuelven).
 * Detecta problemas como interfaces sin implementación o configuraciones faltantes.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // Si el contexto carga sin excepciones, todos los beans están correctos
    }
}
