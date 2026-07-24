package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import com.piedpiper.carbonhub.ecoruta.repository.InsigniaUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EcoRutaInsigniaRegistroServiceTest {

    @Mock
    private InsigniaUsuarioRepository insigniaUsuarioRepository;

    @Test
    void registrarPersisteYFuerzaFlush() {
        EcoRutaInsigniaRegistroService service =
                new EcoRutaInsigniaRegistroService(insigniaUsuarioRepository);
        InsigniaUsuario insigniaUsuario = InsigniaUsuario.builder()
                .idInsignia(2L)
                .build();

        service.registrar(insigniaUsuario);

        verify(insigniaUsuarioRepository).saveAndFlush(insigniaUsuario);
    }
}
