package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ejercita las consultas nuevas de PP-71 contra la base. Los tests de servicio mockean el
 * repositorio, asi que no verifican ni que el fetch join traiga las relaciones LAZY ni que el nombre
 * de la consulta derivada resuelva: un nombre mal escrito falla al levantar el contexto, no al
 * compilar.
 */
@DataJpaTest
class AlertaRepositoryIntegrationTest {

    private static final AtomicLong INDICE = new AtomicLong(1);

    @Autowired
    private AlertaRepository alertaRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void elFetchJoinTraeLaEmpresaYLaCertificacionSinSesionAbierta() {
        UUID alertaId = alertaGuardada(EstadoAlerta.PENDIENTE, 0).getId();
        entityManager.clear();

        Alerta alerta = alertaRepository.buscarConEmpresaYCertificacion(alertaId).orElseThrow();
        entityManager.detach(alerta);

        assertThat(alerta.getEmpresa().getNombreEmpresa()).isEqualTo("Acme S.A.");
        assertThat(alerta.getEmpresa().getSlug()).isNotBlank();
        assertThat(alerta.getCertificacion().getTipo()).isEqualTo(TipoCertificacion.CARBONO_NEUTRAL);
        assertThat(alerta.getCertificacion().getFechaVencimiento()).isNotNull();
    }

    @Test
    void buscarPendientesExcluyeLasQueAgotaronIntentosYLasQueYaSeEnviaron() {
        Alerta pendiente = alertaGuardada(EstadoAlerta.PENDIENTE, 1);
        alertaGuardada(EstadoAlerta.PENDIENTE, 3);
        alertaGuardada(EstadoAlerta.ENVIADA, 1);
        alertaGuardada(EstadoAlerta.FALLIDA, 3);
        entityManager.clear();

        List<Alerta> resultado = alertaRepository
                .findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(EstadoAlerta.PENDIENTE, 3);

        assertThat(resultado).extracting(Alerta::getId).containsExactly(pendiente.getId());
    }

    @Test
    void elContadorDeIntentosArrancaEnCeroYPersiste() {
        Alerta guardada = alertaGuardada(EstadoAlerta.PENDIENTE, 0);
        entityManager.clear();

        Alerta recuperada = alertaRepository.findById(guardada.getId()).orElseThrow();
        assertThat(recuperada.getIntentosEnvio()).isZero();

        recuperada.setIntentosEnvio(2);
        alertaRepository.saveAndFlush(recuperada);
        entityManager.clear();

        assertThat(alertaRepository.findById(guardada.getId()).orElseThrow().getIntentosEnvio()).isEqualTo(2);
    }

    @Test
    void reclamarSubeElIntentoUnaSolaVezAunqueSeIntenteDosVeces() {
        Alerta alerta = alertaGuardada(EstadoAlerta.PENDIENTE, 2);
        entityManager.clear();

        assertThat(alertaRepository.reclamarParaEnvio(alerta.getId(), EstadoAlerta.PENDIENTE, 3)).isEqualTo(1);
        assertThat(alertaRepository.reclamarParaEnvio(alerta.getId(), EstadoAlerta.PENDIENTE, 3))
                .as("con el contador ya en el tope, el segundo intento no debe tomar la fila")
                .isZero();

        assertThat(alertaRepository.findById(alerta.getId()).orElseThrow().getIntentosEnvio()).isEqualTo(3);
    }

    @Test
    void reclamarNoTomaUnaAlertaQueYaSeEnvio() {
        Alerta alerta = alertaGuardada(EstadoAlerta.ENVIADA, 1);
        entityManager.clear();

        assertThat(alertaRepository.reclamarParaEnvio(alerta.getId(), EstadoAlerta.PENDIENTE, 3)).isZero();
    }

    @Test
    void marcarEnviadaNoPisaUnaAlertaQueYaNoEstaPendiente() {
        Alerta alerta = alertaGuardada(EstadoAlerta.FALLIDA, 3);
        entityManager.clear();

        assertThat(alertaRepository.marcarEnviada(
                alerta.getId(), EstadoAlerta.ENVIADA, EstadoAlerta.PENDIENTE, Instant.now())).isZero();
        assertThat(alertaRepository.findById(alerta.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoAlerta.FALLIDA);
    }

    @Test
    void marcarFallidaSoloCierraCuandoElContadorLlegoAlTope() {
        Alerta conIntentosDeSobra = alertaGuardada(EstadoAlerta.PENDIENTE, 1);
        Alerta agotada = alertaGuardada(EstadoAlerta.PENDIENTE, 3);
        entityManager.clear();

        assertThat(alertaRepository.marcarFallidaSiAgotoIntentos(
                conIntentosDeSobra.getId(), EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, 3)).isZero();
        assertThat(alertaRepository.marcarFallidaSiAgotoIntentos(
                agotada.getId(), EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, 3)).isEqualTo(1);

        assertThat(alertaRepository.findById(conIntentosDeSobra.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoAlerta.PENDIENTE);
        assertThat(alertaRepository.findById(agotada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoAlerta.FALLIDA);
    }

    private Alerta alertaGuardada(EstadoAlerta estado, int intentos) {
        Empresa empresa = empresaGuardada();
        return alertaRepository.saveAndFlush(Alerta.builder()
                .empresa(empresa)
                .certificacion(certificacionGuardada(empresa))
                .tipoAlerta(TipoAlerta.DIAS_90)
                .estado(estado)
                .intentosEnvio(intentos)
                .fechaGeneracion(Instant.now())
                .build());
    }

    private Empresa empresaGuardada() {
        return empresaRepository.saveAndFlush(Empresa.builder()
                .nombreEmpresa("Acme S.A.")
                .cedulaJuridica("3-101-" + System.nanoTime())
                .correoCorporativo("contacto" + System.nanoTime() + "@acme.cr")
                .pais("Costa Rica")
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .slug("acme-" + System.nanoTime())
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build());
    }

    private Certificacion certificacionGuardada(Empresa empresa) {
        Usuario auditor = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("auditor" + System.nanoTime() + "@carbonhub.cr")
                .nombre("Ana")
                .apellidos("Mora")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build());

        return entityManager.persistAndFlush(Certificacion.builder()
                .idAuditoria(UUID.randomUUID())
                .empresa(empresa)
                .auditor(auditor)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .fechaEmision(Instant.now())
                .fechaVencimiento(LocalDate.now().plusDays(90))
                .estado(EstadoCertificacion.ACTIVA)
                .credencialJwt("jwt-de-prueba")
                .indiceEstado(INDICE.getAndIncrement())
                .codigoVerificacion("CH-2026-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase())
                .build());
    }
}
