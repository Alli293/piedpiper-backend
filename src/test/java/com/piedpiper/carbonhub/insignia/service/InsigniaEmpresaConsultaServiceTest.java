package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.mappers.InsigniaEmpresaMapper;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.service.SlugResolverService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsigniaEmpresaConsultaServiceTest {

    private static final String SLUG = "cafe-del-valle";
    private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTRA_EMPRESA_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private InsigniaEmpresaRepository insigniaEmpresaRepository;
    @Mock
    private CatalogoInsigniaRepository catalogoInsigniaRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private InsigniaEmpresaMapper insigniaEmpresaMapper;
    @Mock
    private InsigniaEmpresaOpenBadgesService insigniaEmpresaOpenBadgesService;
    @Mock
    private SlugResolverService slugResolver;

    private InsigniaEmpresaConsultaService service;

    @BeforeEach
    void prepararServicio() {
        service = new InsigniaEmpresaConsultaService(
                insigniaEmpresaRepository,
                catalogoInsigniaRepository,
                empresaRepository,
                emisionEmpresaService,
                insigniaEmpresaMapper,
                insigniaEmpresaOpenBadgesService,
                slugResolver);
    }

    private Empresa empresa(UUID id, String nombreEmpresa) {
        return Empresa.builder()
                .id(id)
                .nombreEmpresa(nombreEmpresa)
                .slug(SLUG)
                .build();
    }

    private CatalogoInsignia catalogo(Long idInsignia, String nivel) {
        return CatalogoInsignia.builder()
                .id(1L)
                .idInsignia(idInsignia)
                .nombre("Carbono Neutral")
                .descripcion("Reconocimiento por certificaciones activas.")
                .nivelInsignia(nivel)
                .cantidadMinimaCertificacionesActivas(1)
                .tiposCertificacionesRequeridas(Set.of())
                .activa(true)
                .build();
    }

    private InsigniaEmpresa insigniaEmpresa(UUID empresaId, Long idInsignia, String nivel) {
        return InsigniaEmpresa.builder()
                .id(UUID.randomUUID())
                .empresa(empresa(empresaId, "Café del Valle S.A."))
                .idInsignia(idInsignia)
                .nivelInsignia(nivel)
                .fechaObtencion(Instant.parse("2026-01-15T00:00:00Z"))
                .build();
    }

    @Test
    void listarPorSlug_resuelveLaEmpresaPorSlugYConsultaSoloSusInsignias() {
        when(slugResolver.resolver(SLUG))
                .thenReturn(empresa(EMPRESA_ID, "Café del Valle S.A."));
        when(insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(EMPRESA_ID))
                .thenReturn(List.of());
        when(catalogoInsigniaRepository.findByActivaTrue()).thenReturn(List.of());

        service.listarPorSlug(SLUG);

        verify(insigniaEmpresaRepository).findByEmpresaIdOrderByFechaObtencionDesc(EMPRESA_ID);
        verify(insigniaEmpresaRepository, never())
                .findByEmpresaIdOrderByFechaObtencionDesc(OTRA_EMPRESA_ID);
    }

    @Test
    void listarPorSlug_proyectaLosDatosDelCatalogoEnElDto() {
        InsigniaEmpresa otorgada = insigniaEmpresa(EMPRESA_ID, 1L, "bronce");
        CatalogoInsignia catalogo = catalogo(1L, "bronce");

        when(slugResolver.resolver(SLUG))
                .thenReturn(empresa(EMPRESA_ID, "Café del Valle S.A."));
        when(insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(EMPRESA_ID))
                .thenReturn(List.of(otorgada));
        when(catalogoInsigniaRepository.findByActivaTrue()).thenReturn(List.of(catalogo));
        when(insigniaEmpresaMapper.toDto(otorgada)).thenReturn(new InsigniaEmpresaResponseDTO(
                1L, "bronce", null, null, otorgada.getFechaObtencion()));
        when(insigniaEmpresaOpenBadgesService.criterios(catalogo)).thenReturn("criterios");
        when(insigniaEmpresaOpenBadgesService.emisorNombre()).thenReturn("CarbonHub");
        when(insigniaEmpresaOpenBadgesService.urlVerificacionPublica(otorgada.getId()))
                .thenReturn("https://carbonhub.test/api/insignias/" + otorgada.getId() + "/verificacion");
        when(insigniaEmpresaOpenBadgesService.urlVerificacionJwt(otorgada.getId()))
                .thenReturn("https://carbonhub.test/api/insignias/" + otorgada.getId() + "/verificacion.jwt");
        when(insigniaEmpresaOpenBadgesService.urlLinkedIn(otorgada, catalogo)).thenReturn(null);

        List<InsigniaEmpresaResponseDTO> resultado = service.listarPorSlug(SLUG);

        assertThat(resultado).hasSize(1);
        InsigniaEmpresaResponseDTO dto = resultado.get(0);
        assertThat(dto.getNombre()).isEqualTo("Carbono Neutral");
        assertThat(dto.getDescripcion()).isEqualTo("Reconocimiento por certificaciones activas.");
        assertThat(dto.getEmisor()).isEqualTo("CarbonHub");
        assertThat(dto.getReceptor()).isEqualTo("Café del Valle S.A.");
        assertThat(dto.getUrlVerificacionPublica()).contains("/verificacion");
    }

    @Test
    void listarPorSlug_lanza404CuandoLaEmpresaNoExisteOEstaInactiva() {
        when(slugResolver.resolver(SLUG))
                .thenThrow(new PerfilNoEncontradoException(
                        "El perfil que buscas no existe o ya no está disponible."));

        assertThatThrownBy(() -> service.listarPorSlug(SLUG))
                .isInstanceOf(PerfilNoEncontradoException.class);

        verify(insigniaEmpresaRepository, never())
                .findByEmpresaIdOrderByFechaObtencionDesc(any());
    }

    private InsigniaEmpresa insigniaEmpresaConCodigo(String codigo, EstadoEmpresa estadoEmpresa) {
        return InsigniaEmpresa.builder()
                .id(UUID.randomUUID())
                .codigoVerificacion(codigo)
                .empresa(Empresa.builder().nombreEmpresa("EcoCorp").estado(estadoEmpresa).build())
                .idInsignia(1L)
                .nivelInsignia("oro")
                .fechaObtencion(Instant.parse("2026-01-15T00:00:00Z"))
                .build();
    }

    @Test
    void verificarPorCodigoDeUnaInsigniaVigenteDevuelveValidaVigente() {
        String codigo = "CH-2026-8F4A19KD";
        InsigniaEmpresa insigniaEmpresa = insigniaEmpresaConCodigo(codigo, EstadoEmpresa.ACTIVO);
        when(insigniaEmpresaRepository.findByCodigoVerificacion(codigo))
                .thenReturn(Optional.of(insigniaEmpresa));
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsigniaAndActivaTrue(1L, "oro"))
                .thenReturn(Optional.of(catalogo(1L, "oro")));
        when(insigniaEmpresaOpenBadgesService.emisorNombre()).thenReturn("CarbonHub");

        VerificacionCredencialDTO resultado = service.verificarPorCodigo(codigo);

        assertThat(resultado.getEstado()).isEqualTo("valida_vigente");
        assertThat(resultado.getCategoria()).isEqualTo("INSIGNIA");
        assertThat(resultado.getNivelInsignia()).isEqualTo("oro");
        assertThat(resultado.getNombreCertificacion()).isEqualTo("Carbono Neutral");
        assertThat(resultado.getEmpresa()).isEqualTo("EcoCorp");
        assertThat(resultado.getEntidadCertificadora()).isEqualTo("CarbonHub");
        assertThat(resultado.getFechaConsulta()).isNotNull();
    }

    @Test
    void verificarPorCodigoDeInsigniaInexistenteLanzaRecursoNoEncontrado() {
        String codigo = "CH-2026-8F4A19KD";
        when(insigniaEmpresaRepository.findByCodigoVerificacion(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificarPorCodigo(codigo))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void verificarPorCodigoDeInsigniaMalFormadoLanzaRecursoNoEncontradoSinConsultarElRepositorio() {
        assertThatThrownBy(() -> service.verificarPorCodigo("no-es-un-codigo-valido"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(insigniaEmpresaRepository, never()).findByCodigoVerificacion(any());
    }

    @Test
    void verificarPorCodigoDeInsigniaDeEmpresaInactivaLanzaRecursoNoEncontrado() {
        String codigo = "CH-2026-8F4A19KD";
        when(insigniaEmpresaRepository.findByCodigoVerificacion(codigo))
                .thenReturn(Optional.of(insigniaEmpresaConCodigo(codigo, EstadoEmpresa.INACTIVO)));

        assertThatThrownBy(() -> service.verificarPorCodigo(codigo))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
