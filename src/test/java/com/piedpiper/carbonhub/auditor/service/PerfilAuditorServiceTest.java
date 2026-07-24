package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilAuditorServiceTest {

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;

    @InjectMocks
    private PerfilAuditorService service;

    private Usuario auditor() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();
    }

    @Test
    void creaElPerfilCuandoElAuditorNoTieneUno() {
        Usuario auditor = auditor();
        when(perfilAuditorRepository.existsByAuditorId(auditor.getId())).thenReturn(false);

        service.asegurarPerfil(auditor);

        ArgumentCaptor<PerfilAuditor> captor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(captor.capture());
        PerfilAuditor perfil = captor.getValue();
        assertThat(perfil.getAuditor()).isEqualTo(auditor);
        assertThat(perfil.isDisponible()).isTrue();
        assertThat(perfil.getAuditoriasCompletadas()).isZero();
        assertThat(perfil.getCalificacionPromedio()).isNull();
        assertThat(perfil.getEspecialidades()).isEmpty();
    }

    @Test
    void noDuplicaElPerfilSiYaExiste() {
        Usuario auditor = auditor();
        when(perfilAuditorRepository.existsByAuditorId(auditor.getId())).thenReturn(true);

        service.asegurarPerfil(auditor);

        verify(perfilAuditorRepository, never()).save(any());
    }

    @Test
    void ignoraUsuariosQueNoSonAuditores() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .build();

        service.asegurarPerfil(usuario);

        verify(perfilAuditorRepository, never()).existsByAuditorId(any());
        verify(perfilAuditorRepository, never()).save(any());
    }
}
