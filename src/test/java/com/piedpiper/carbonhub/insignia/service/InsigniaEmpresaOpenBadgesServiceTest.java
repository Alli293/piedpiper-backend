package com.piedpiper.carbonhub.insignia.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.piedpiper.carbonhub.certificacion.service.FirmanteCredencialService;
import com.piedpiper.carbonhub.certificacion.service.GeneradorCredencialOpenBadges;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsigniaEmpresaOpenBadgesServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTRA_EMPRESA_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID INSIGNIA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private InsigniaEmpresaRepository insigniaEmpresaRepository;
    @Mock
    private CatalogoInsigniaRepository catalogoInsigniaRepository;
    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    @Mock
    private FirmanteCredencialService firmanteCredencialService;

    private InsigniaEmpresaOpenBadgesService service;

    @BeforeEach
    void setUp() {
        service = new InsigniaEmpresaOpenBadgesService(
                insigniaEmpresaRepository,
                catalogoInsigniaRepository,
                emisionEmpresaService,
                generadorCredencialOpenBadges,
                firmanteCredencialService,
                "https://carbonhub.test",
                "CarbonHub");
    }

    @Test
    void lanza403SiLaInsigniaPerteneceAOtraEmpresa() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(insigniaEmpresaRepository.findById(INSIGNIA_ID))
                .thenReturn(Optional.of(insignia(empresa(OTRA_EMPRESA_ID))));

        assertThatThrownBy(() -> service.generarParaEmpresaAutenticada(USUARIO_ID, INSIGNIA_ID))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generaJwtPublicoConLaMismaLogicaDeFirmaDeOpenBadges() {
        InsigniaEmpresa insignia = insignia(empresa(EMPRESA_ID));
        CatalogoInsignia catalogo = catalogo();

        when(insigniaEmpresaRepository.findById(INSIGNIA_ID)).thenReturn(Optional.of(insignia));
        when(catalogoInsigniaRepository.findByIdInsigniaAndNivelInsigniaAndActivaTrue(1L, "bronce"))
                .thenReturn(Optional.of(catalogo));
        when(generadorCredencialOpenBadges.urlEmisor())
                .thenReturn("https://carbonhub.test/api/certificaciones/emisor");
        when(generadorCredencialOpenBadges.construirEmisor()).thenReturn(Map.of(
                "id", "https://carbonhub.test/api/certificaciones/emisor",
                "type", "Profile",
                "name", "CarbonHub"));
        when(firmanteCredencialService.firmar(any(JWTClaimsSet.class)))
                .thenReturn("header.payload.signature");

        String jwt = service.generarJwtPublico(INSIGNIA_ID);

        assertThat(jwt).isEqualTo("header.payload.signature");
        ArgumentCaptor<JWTClaimsSet> captor = ArgumentCaptor.forClass(JWTClaimsSet.class);
        verify(firmanteCredencialService).firmar(captor.capture());
        assertThat(captor.getValue().getJWTID())
                .isEqualTo("https://carbonhub.test/api/insignias/" + INSIGNIA_ID + "/verificacion.jwt");
        Map<String, Object> vc = (Map<String, Object>) captor.getValue().getClaim("vc");
        assertThat(vc.get("type")).isEqualTo(List.of(
                "VerifiableCredential", "OpenBadgeCredential"));
    }

    private InsigniaEmpresa insignia(Empresa empresa) {
        return InsigniaEmpresa.builder()
                .id(INSIGNIA_ID)
                .empresa(empresa)
                .idInsignia(1L)
                .nivelInsignia("bronce")
                .fechaObtencion(Instant.parse("2026-01-15T00:00:00Z"))
                .build();
    }

    private Empresa empresa(UUID id) {
        return Empresa.builder()
                .id(id)
                .nombreEmpresa(id.equals(EMPRESA_ID) ? "Cafe del Valle S.A." : "Otra Empresa S.A.")
                .slug(id.equals(EMPRESA_ID) ? "cafe-del-valle" : "otra-empresa")
                .build();
    }

    private CatalogoInsignia catalogo() {
        return CatalogoInsignia.builder()
                .idInsignia(1L)
                .nivelInsignia("bronce")
                .nombre("Carbono Neutral")
                .descripcion("Reconocimiento ambiental activo.")
                .cantidadMinimaCertificacionesActivas(1)
                .build();
    }
}
