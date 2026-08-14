package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.AlertaVencimientoResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAlertasServiceTest {

    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private CatalogoTiposCertificacion catalogoTiposCertificacion;

    private DashboardAlertasService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.now(ZonasHorarias.COSTA_RICA);

    @BeforeEach
    void setUp() {
        service = new DashboardAlertasService(
                emisionEmpresaService, certificacionRepository,
                new VencimientoPresentacionService(catalogoTiposCertificacion));
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        stubNombre(TipoCertificacion.CARBONO_NEUTRAL, "Carbono Neutral");
        stubNombre(TipoCertificacion.INVENTARIO_GEI, "Inventario de GEI");
        stubNombre(TipoCertificacion.HUELLA_PRODUCTO, "Huella de Carbono de Producto");
    }

    @Test
    void ordenaLasAlertasPorDiasRestantesAscendenteConLasVencidasPrimero() {
        Certificacion a90 = certificacion(HOY.plusDays(90), TipoCertificacion.HUELLA_PRODUCTO);
        Certificacion a30 = certificacion(HOY.plusDays(30), TipoCertificacion.INVENTARIO_GEI);
        Certificacion a7 = certificacion(HOY.plusDays(7), TipoCertificacion.CARBONO_NEUTRAL);
        Certificacion vencida = certificacion(HOY.minusDays(5), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(vencida, a7, a30, a90));

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).extracting(AlertaVencimientoResponseDTO::getDiasRestantes)
                .containsExactly(-5L, 7L, 30L, 90L);
        assertThat(resultado.get(0).getUrgencia()).isEqualTo("vencida");
    }

    @Test
    void filtraSoloLasDeLaEmpresaAutenticada() {
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void excluyeCertificacionesFueraDelUmbralDe90Dias() {
        Certificacion lejana = certificacion(HOY.plusDays(120), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(lejana));

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void unaCertificacionAExactamente90DiasSeIncluye() {
        Certificacion a90 = certificacion(HOY.plusDays(90), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(a90));

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void unaCertificacionA91DiasSeExcluye() {
        Certificacion a91 = certificacion(HOY.plusDays(91), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(a91));

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void unaCertificacionRenovadaMasAlla90DiasYaNoApareceComoAlerta() {
        // Simula una certificacion que en algun momento cruzo un umbral (habria
        // generado una fila en `alertas`, PP-70) pero cuya fechaVencimiento ya
        // fue actualizada mas alla de 90 dias: el panel no debe mostrarla, sin
        // necesidad de tocar la tabla `alertas` para lograrlo.
        Certificacion renovada = certificacion(HOY.plusDays(200), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(renovada));

        List<AlertaVencimientoResponseDTO> resultado = service.obtenerAlertas(USUARIO_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void soloConsultaCertificacionesActivas() {
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        service.obtenerAlertas(USUARIO_ID);

        org.mockito.Mockito.verify(certificacionRepository)
                .findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(EMPRESA_ID, EstadoCertificacion.ACTIVA);
    }

    @Test
    void incluyeNombreFechaYDiasRestantesEnCadaAlerta() {
        Certificacion cert = certificacion(HOY.plusDays(7), TipoCertificacion.CARBONO_NEUTRAL);
        when(certificacionRepository.findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(
                EMPRESA_ID, EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(cert));

        AlertaVencimientoResponseDTO dto = service.obtenerAlertas(USUARIO_ID).get(0);

        assertThat(dto.getIdCertificacion()).isEqualTo(cert.getId());
        assertThat(dto.getNombre()).isEqualTo("Carbono Neutral");
        assertThat(dto.getFechaVencimiento()).isEqualTo(HOY.plusDays(7));
        assertThat(dto.getDiasRestantes()).isEqualTo(7);
        assertThat(dto.getUrgencia()).isEqualTo("7_dias");
    }

    private void stubNombre(TipoCertificacion tipo, String nombre) {
        lenient().when(catalogoTiposCertificacion.buscar(tipo)).thenReturn(Optional.of(
                new DefinicionCertificacion(tipo, nombre, "descripcion", 12,
                        TipoLogroOpenBadges.CERTIFICATE, "criterio")));
    }

    private static Certificacion certificacion(LocalDate fechaVencimiento, TipoCertificacion tipo) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .tipo(tipo)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(fechaVencimiento)
                .build();
    }
}
