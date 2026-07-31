package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
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

@ExtendWith(MockitoExtension.class)
class DashboardCertificacionesServiceTest {

    @Mock
    private EmisionEmpresaService emisionEmpresaService;
    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private DashboardCertificacionesService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Test
    void cuentaActivasConAlertaYVencidasPorSeparado() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);

        Certificacion activaSinAlerta1 = certificacion(60);
        Certificacion activaSinAlerta2 = certificacion(120);
        Certificacion activaSinAlerta3 = certificacion(200);
        Certificacion conAlerta = certificacion(30);
        Certificacion vencida1 = certificacion(-1);
        Certificacion vencida2 = certificacion(-10);

        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of(
                activaSinAlerta1, activaSinAlerta2, activaSinAlerta3, conAlerta, vencida1, vencida2));
        when(alertaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(
                alertaPara(conAlerta)));

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getActivas()).isEqualTo(3);
        assertThat(resumen.getProximasAVencer()).isEqualTo(1);
        assertThat(resumen.getVencidas()).isEqualTo(2);
    }

    @Test
    void empresaSinCertificacionesDevuelveTodosLosConteosEnCero() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of());
        when(alertaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

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
        when(alertaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        ResumenCertificacionesDashboardResponseDTO resumen = service.obtenerResumen(USUARIO_ID);

        assertThat(resumen.getVencidas()).isEqualTo(1);
        assertThat(resumen.getActivas()).isZero();
        assertThat(resumen.getProximasAVencer()).isZero();
    }

    @Test
    void soloConsultaLasCertificacionesYAlertasDeLaEmpresaAutenticada() {
        when(emisionEmpresaService.empresaId(USUARIO_ID)).thenReturn(EMPRESA_ID);
        when(certificacionRepository.findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID)).thenReturn(List.of());
        when(alertaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        service.obtenerResumen(USUARIO_ID);

        verify(certificacionRepository).findByEmpresaIdOrderByFechaEmisionDesc(EMPRESA_ID);
        verify(alertaRepository).findByEmpresaId(EMPRESA_ID);
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

    private static Alerta alertaPara(Certificacion certificacion) {
        return Alerta.builder()
                .id(UUID.randomUUID())
                .empresa(certificacion.getEmpresa())
                .certificacion(certificacion)
                .tipoAlerta(TipoAlerta.DIAS_30)
                .build();
    }
}
