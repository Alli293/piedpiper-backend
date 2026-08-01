package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.ResumenCertificacionesDashboardResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La clasificacion depende exclusivamente de {@code fechaVencimiento} contra
 * hoy, no de si el proceso nocturno de alertas (PP-70) ya genero una fila en
 * {@code alertas} para la certificacion — ver el javadoc de
 * {@link DashboardCertificacionesService} para el porque. Por eso estos
 * tests no mockean {@code AlertaRepository}: el servicio ya no depende de el.
 */
@ExtendWith(MockitoExtension.class)
class DashboardCertificacionesServiceTest {

    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private CertificacionRepository certificacionRepository;

    @InjectMocks
    private DashboardCertificacionesService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Test
    void cuentaActivasProximasYVencidasSegunDiasRestantes() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);

        Certificacion activa1 = certificacion(91);
        Certificacion activa2 = certificacion(120);
        Certificacion activa3 = certificacion(200);
        Certificacion proxima = certificacion(30);
        Certificacion vencida1 = certificacion(-1);
        Certificacion vencida2 = certificacion(-10);

        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of(
                activa1, activa2, activa3, proxima, vencida1, vencida2));

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getActivas()).isEqualTo(3);
        assertThat(resumen.getProximasAVencer()).isEqualTo(1);
        assertThat(resumen.getVencidas()).isEqualTo(2);
    }

    @Test
    void aExactamente90DiasCuentaComoProximaAVencer() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID))
                .thenReturn(List.of(certificacion(90)));

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getProximasAVencer()).isEqualTo(1);
        assertThat(resumen.getActivas()).isZero();
    }

    @Test
    void a91DiasCuentaComoActiva() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID))
                .thenReturn(List.of(certificacion(91)));

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getActivas()).isEqualTo(1);
        assertThat(resumen.getProximasAVencer()).isZero();
    }

    @Test
    void empresaSinCertificacionesDevuelveTodosLosConteosEnCero() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of());

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getActivas()).isZero();
        assertThat(resumen.getProximasAVencer()).isZero();
        assertThat(resumen.getVencidas()).isZero();
    }

    @Test
    void certificacionQueVenceHoyCuentaComoVencida() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        Certificacion venceHoy = certificacion(0);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID))
                .thenReturn(List.of(venceHoy));

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getVencidas()).isEqualTo(1);
        assertThat(resumen.getActivas()).isZero();
        assertThat(resumen.getProximasAVencer()).isZero();
    }

    @Test
    void soloConsultaLasCertificacionesDeLaEmpresaAutenticada() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of());

        service.obtenerResumen(USUARIO_ID);

        verify(certificacionRepository).findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID);
    }

    private static Certificacion certificacion(int diasParaVencer) {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(LocalDate.now(ZonasHorarias.COSTA_RICA).plusDays(diasParaVencer))
                .build();
    }
}
