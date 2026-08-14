package com.piedpiper.carbonhub.meta.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.meta.models.entities.Meta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba contra una base real el comentario de revisión sobre
 * {@code MetaService.crear()}: {@code Meta.id} usa {@code GenerationType.UUID}
 * (asignado en memoria, no {@code IDENTITY}), así que {@code save()} no
 * dispara el INSERT de inmediato — Hibernate puede diferirlo hasta el
 * flush/commit de la transacción. Un test con Mockito no puede detectar
 * esto (el repositorio está mockeado), de ahí que haga falta este test de
 * integración contra una base real.
 */
@DataJpaTest
class MetaRepositoryFlushIntegrationTest {

    @Autowired
    private MetaRepository metaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Test
    void findByIdAndEmpresaIdEncuentraLaMetaDeSuPropiaEmpresa() {
        Empresa empresa = empresaRepository.save(empresaDePrueba());
        Meta meta = metaRepository.saveAndFlush(Meta.builder()
                .empresa(empresa)
                .nombreMeta("Meta de prueba")
                .valorObjetivoHuellaT(new BigDecimal("50.0000"))
                .fechaLimite(LocalDate.now().plusMonths(6))
                .fechaCreacion(Instant.now())
                .build());

        assertThat(metaRepository.findByIdAndEmpresaId(meta.getId(), empresa.getId())).isPresent();
    }

    @Test
    void findByIdAndEmpresaIdNoEncuentraLaMetaDeOtraEmpresa() {
        Empresa empresa = empresaRepository.save(empresaDePrueba());
        Meta meta = metaRepository.saveAndFlush(Meta.builder()
                .empresa(empresa)
                .nombreMeta("Meta de prueba")
                .valorObjetivoHuellaT(new BigDecimal("50.0000"))
                .fechaLimite(LocalDate.now().plusMonths(6))
                .fechaCreacion(Instant.now())
                .build());

        assertThat(metaRepository.findByIdAndEmpresaId(meta.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveNoFallaDeInmediatoParaUnaEmpresaInexistente() {
        Meta meta = metaConEmpresaInexistente();

        // Con GenerationType.UUID, Hibernate puede diferir el INSERT: save()
        // sola no garantiza que la violacion de FK aparezca aca.
        assertThatCode(() -> metaRepository.save(meta)).doesNotThrowAnyException();
    }

    @Test
    void saveAndFlushFallaDeInmediatoParaUnaEmpresaInexistente() {
        Meta meta = metaConEmpresaInexistente();

        // saveAndFlush SI fuerza el INSERT dentro de esta llamada: es lo que
        // permite que MetaService.crear() capture la violacion de FK en su
        // propio try/catch y devuelva el mensaje de error propio del ticket,
        // en vez de dejar que escale sin control al terminar la transaccion.
        assertThatThrownBy(() -> metaRepository.saveAndFlush(meta))
                .isInstanceOf(DataAccessException.class);
    }

    private Empresa empresaDePrueba() {
        String sufijo = UUID.randomUUID().toString();
        return Empresa.builder()
                .nombreEmpresa("Empresa de prueba " + sufijo)
                .cedulaJuridica(sufijo)
                .sectorIndustrial(SectorIndustrial.SERVICIOS)
                .pais("Costa Rica")
                .correoCorporativo(sufijo + "@empresa-prueba.test")
                .slug("empresa-prueba-" + sufijo)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private Meta metaConEmpresaInexistente() {
        // Empresa "transitoria": nunca se guardo, su id no existe en la base.
        // Simula exactamente el resultado de empresaRepository.getReferenceById(...)
        // con un id invalido -- MetaService confia en que ese id ya fue
        // validado por una capa anterior (docs/CONVENTIONS.md §4.7), asi que
        // esto solo puede pasar por un bug real en esa capa, no por un caso
        // de uso normal; aun asi, el manejo de errores debe sostenerse.
        Empresa empresaInexistente = Empresa.builder().id(UUID.randomUUID()).build();

        return Meta.builder()
                .empresa(empresaInexistente)
                .nombreMeta("Meta de prueba")
                .valorObjetivoHuellaT(new BigDecimal("50.0000"))
                .fechaLimite(LocalDate.now().plusMonths(6))
                .fechaCreacion(Instant.now())
                .build();
    }
}
