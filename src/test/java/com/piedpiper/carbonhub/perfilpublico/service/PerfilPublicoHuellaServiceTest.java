package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;
import com.piedpiper.carbonhub.emision.repository.EmisionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EvolucionHuellaDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilPublicoHuellaServiceTest {

    private static final String SLUG = "cafe-del-valle";
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private EmisionRepository emisionRepository;

    private PerfilPublicoHuellaService service;

    @BeforeEach
    void prepararServicio() {
        service = new PerfilPublicoHuellaService(
                empresaRepository,
                solicitudAuditoriaRepository,
                emisionRepository);
    }

    @Test
    void variosPeriodosVerificadosArmanSerieEnToneladasVariacionYTendencia() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        SolicitudAuditoria periodo2024 = periodo(2024);
        SolicitudAuditoria periodo2025 = periodo(2025);
        SolicitudAuditoria periodo2026 = periodo(2026);
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(periodo2024, periodo2025, periodo2026));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2024.getPeriodoInicio(), periodo2024.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision("5000.000")));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2025.getPeriodoInicio(), periodo2025.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision("4000.000")));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2026.getPeriodoInicio(), periodo2026.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision("3500.000")));

        EvolucionHuellaDTO resultado = service.obtener(SLUG, "ultimos_3_anios");

        assertThat(resultado.getRangoPeriodo()).isEqualTo("ultimos_3_anios");
        assertThat(resultado.getTendencia()).isEqualTo("reduccion");
        assertThat(resultado.getSerie()).hasSize(3);
        assertThat(resultado.getSerie().get(0).getPeriodo()).isEqualTo("2024");
        assertThat(resultado.getSerie().get(0).getHuellaT()).isEqualByComparingTo("5.0000");
        assertThat(resultado.getSerie().get(0).getVariacionPorcentual()).isNull();
        assertThat(resultado.getSerie().get(1).getHuellaT()).isEqualByComparingTo("4.0000");
        assertThat(resultado.getSerie().get(1).getVariacionPorcentual()).isEqualByComparingTo("-20.0");
        assertThat(resultado.getSerie().get(2).getVariacionPorcentual()).isEqualByComparingTo("-12.5");
    }

    @Test
    void unSoloPeriodoSeMuestraSinVariacionYSinCambio() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        SolicitudAuditoria periodo2026 = periodo(2026);
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(periodo2026));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2026.getPeriodoInicio(), periodo2026.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision("5236.000")));

        EvolucionHuellaDTO resultado = service.obtener(SLUG, "ultimo_anio");

        assertThat(resultado.getSerie()).hasSize(1);
        assertThat(resultado.getSerie().get(0).getHuellaT()).isEqualByComparingTo("5.2360");
        assertThat(resultado.getSerie().get(0).getVariacionPorcentual()).isNull();
        assertThat(resultado.getTendencia()).isEqualTo("sin_cambio");
    }

    @Test
    void sinPeriodosVerificadosRetornaSerieVacia() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        EvolucionHuellaDTO resultado = service.obtener(SLUG, "historico");

        assertThat(resultado.getSerie()).isEmpty();
        assertThat(resultado.getTendencia()).isEqualTo("sin_cambio");
    }

    @Test
    void rangoInvalidoUsaUltimosTresAnios() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        EvolucionHuellaDTO resultado = service.obtener(SLUG, "valor-raro");

        assertThat(resultado.getRangoPeriodo()).isEqualTo("ultimos_3_anios");
    }

    @Test
    void emisionInvalidaSeOmiteSinInterrumpirPeriodosValidos() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        SolicitudAuditoria periodo2025 = periodo(2025);
        SolicitudAuditoria periodo2026 = periodo(2026);
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of(periodo2025, periodo2026));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2025.getPeriodoInicio(), periodo2025.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision(null), emision("-1.000")));
        when(emisionRepository.findAllByEmpresaIdAndPeriodo(
                EMPRESA_ID, periodo2026.getPeriodoInicio(), periodo2026.getPeriodoFin().plusDays(1)))
                .thenReturn(List.of(emision("2500.000")));

        EvolucionHuellaDTO resultado = service.obtener(SLUG, "historico");

        assertThat(resultado.getSerie()).hasSize(1);
        assertThat(resultado.getSerie().get(0).getPeriodo()).isEqualTo("2026");
        assertThat(resultado.getSerie().get(0).getHuellaT()).isEqualByComparingTo("2.5000");
    }

    @Test
    void slugInexistenteLanza404DelPerfilPublico() {
        when(empresaRepository.findBySlugAndEstado("fantasma", EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener("fantasma", "historico"))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void usaSoloEstadosVerificadosYCertificacionActiva() {
        Empresa empresa = Empresa.builder().id(EMPRESA_ID).build();
        when(empresaRepository.findBySlugAndEstado(SLUG, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(solicitudAuditoriaRepository.listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA))
                .thenReturn(List.of());

        service.obtener(SLUG, "historico");

        verify(solicitudAuditoriaRepository).listarPeriodosVerificados(
                EMPRESA_ID,
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoCertificacion.ACTIVA);
    }

    private SolicitudAuditoria periodo(int anio) {
        return SolicitudAuditoria.builder()
                .id(UUID.randomUUID())
                .periodoInicio(LocalDate.of(anio, 1, 1))
                .periodoFin(LocalDate.of(anio, 12, 31))
                .build();
    }

    private Emision emision(String carbonKg) {
        return EmisionElectricidad.builder()
                .id(UUID.randomUUID())
                .carbonKg(carbonKg == null ? null : new BigDecimal(carbonKg))
                .build();
    }
}
