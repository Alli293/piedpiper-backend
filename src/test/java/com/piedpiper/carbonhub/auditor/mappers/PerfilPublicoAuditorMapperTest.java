package com.piedpiper.carbonhub.auditor.mappers;

import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilPublicoAuditorMapperTest {

    private final PerfilPublicoAuditorMapper mapper = new PerfilPublicoAuditorMapperImpl();

    @Test
    void aPerfilPublicoDtoMapeaTodosLosCamposCorrectamente() {
        // Arrange
        Usuario auditor = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("María")
                .apellidos("García López")
                .email("maria@example.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .build();

        PerfilAuditor perfil = PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .fotoPerfil("https://cdn.example.com/maria.png")
                .disponible(true)
                .especialidades(Set.of(
                        EspecialidadAuditor.MANUFACTURA,
                        EspecialidadAuditor.AGROINDUSTRIA,
                        EspecialidadAuditor.ENERGIA_RENOVABLE))
                .descripcionProfesional("Auditora con experiencia en manufactura")
                .build();

        MetricasAuditor metricas = new MetricasAuditor(
                new BigDecimal("4.3"), 12, 8, new BigDecimal("1.5"));

        List<CertificacionPublicaDTO> certificaciones = List.of(
                new CertificacionPublicaDTO("Carbono Neutral", "CarbonHub",
                        LocalDate.of(2027, 3, 1), false));

        List<DistribucionSectorDTO> distribucion = List.of(
                new DistribucionSectorDTO("MANUFACTURA", new BigDecimal("75.0")));

        List<ResenaVerificadaDTO> resenas = List.of(
                new ResenaVerificadaDTO(new BigDecimal("5.0"), "Excelente", LocalDate.of(2024, 10, 5)));

        // Act
        PerfilPublicoAuditorResponseDTO result = mapper.aPerfilPublicoDto(
                perfil, metricas, certificaciones, distribucion, resenas);

        // Assert — nombre concatenado
        assertThat(result.getNombre()).isEqualTo("María García López");

        // Assert — especialidades sorted alphabetically by enum name
        assertThat(result.getEspecialidades())
                .containsExactly("AGROINDUSTRIA", "ENERGIA_RENOVABLE", "MANUFACTURA");

        // Assert — métricas mapeadas
        assertThat(result.getCalificacionPromedio()).isEqualByComparingTo(new BigDecimal("4.3"));
        assertThat(result.getTotalResenas()).isEqualTo(12);
        assertThat(result.getAuditoriasCompletadas()).isEqualTo(8);
        assertThat(result.getTiempoPromedioRespuestaDias()).isEqualByComparingTo(new BigDecimal("1.5"));

        // Assert — campos directos
        assertThat(result.getAuditorId()).isEqualTo(auditor.getId());
        assertThat(result.getFotoPerfil()).isEqualTo("https://cdn.example.com/maria.png");
        assertThat(result.getDescripcionProfesional()).isEqualTo("Auditora con experiencia en manufactura");
        assertThat(result.isDisponible()).isTrue();

        // Assert — colecciones pasadas tal cual
        assertThat(result.getCertificaciones()).hasSize(1);
        assertThat(result.getDistribucionSectores()).hasSize(1);
        assertThat(result.getResenas()).hasSize(1);
    }

    @Test
    void aPerfilPublicoDtoConMetricasNullNoSetteaMetricas() {
        // Arrange
        Usuario auditor = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .apellidos(null)
                .email("juan@example.com")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .build();

        PerfilAuditor perfil = PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .disponible(false)
                .especialidades(Collections.emptySet())
                .build();

        // Act
        PerfilPublicoAuditorResponseDTO result = mapper.aPerfilPublicoDto(
                perfil, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        // Assert — nombre sin apellidos
        assertThat(result.getNombre()).isEqualTo("Juan");

        // Assert — métricas quedan null
        assertThat(result.getCalificacionPromedio()).isNull();
        assertThat(result.getTotalResenas()).isNull();
        assertThat(result.getAuditoriasCompletadas()).isNull();
        assertThat(result.getTiempoPromedioRespuestaDias()).isNull();

        // Assert — especialidades vacías
        assertThat(result.getEspecialidades()).isEmpty();

        // Assert — no disponible
        assertThat(result.isDisponible()).isFalse();
    }
}
