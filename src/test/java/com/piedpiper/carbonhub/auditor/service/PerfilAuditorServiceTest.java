package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(perfilAuditorRepository.findByAuditorId(auditor.getId())).thenReturn(Optional.empty());
        when(perfilAuditorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PerfilAuditor perfil = service.asegurarPerfil(auditor);

        ArgumentCaptor<PerfilAuditor> captor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(captor.capture());
        assertThat(perfil).isSameAs(captor.getValue());
        assertThat(perfil.getAuditor()).isEqualTo(auditor);
        assertThat(perfil.isDisponible()).isTrue();
        assertThat(perfil.getAuditoriasCompletadas()).isZero();
        assertThat(perfil.getCalificacionPromedio()).isNull();
        assertThat(perfil.getEspecialidades()).isEmpty();
    }

    @Test
    void noDuplicaElPerfilSiYaExiste() {
        Usuario auditor = auditor();
        PerfilAuditor existente = PerfilAuditor.builder().auditor(auditor).build();
        when(perfilAuditorRepository.findByAuditorId(auditor.getId())).thenReturn(Optional.of(existente));

        PerfilAuditor perfil = service.asegurarPerfil(auditor);

        assertThat(perfil).isSameAs(existente);
        verify(perfilAuditorRepository, never()).save(any());
    }

    @Test
    void siOtraLlamadaConcurrenteYaCreoElPerfilDevuelveEseInsteadDeFallar() {
        Usuario auditor = auditor();
        PerfilAuditor creadoPorLaOtraLlamada = PerfilAuditor.builder().auditor(auditor).build();
        when(perfilAuditorRepository.findByAuditorId(auditor.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(creadoPorLaOtraLlamada));
        when(perfilAuditorRepository.save(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_perfiles_auditor_auditor\""));

        PerfilAuditor perfil = service.asegurarPerfil(auditor);

        assertThat(perfil).isSameAs(creadoPorLaOtraLlamada);
    }

    @Test
    void siElConflictoPersisteLanzaErrorInterno() {
        Usuario auditor = auditor();
        when(perfilAuditorRepository.findByAuditorId(auditor.getId())).thenReturn(Optional.empty());
        when(perfilAuditorRepository.save(any())).thenThrow(new DataIntegrityViolationException("conflicto"));

        assertThatThrownBy(() -> service.asegurarPerfil(auditor))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void ignoraUsuariosQueNoSonAuditores() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .build();

        PerfilAuditor perfil = service.asegurarPerfil(usuario);

        assertThat(perfil).isNull();
        verify(perfilAuditorRepository, never()).findByAuditorId(any());
        verify(perfilAuditorRepository, never()).save(any());
    }
}
