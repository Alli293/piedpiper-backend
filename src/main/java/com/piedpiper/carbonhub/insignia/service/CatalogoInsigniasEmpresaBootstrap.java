package com.piedpiper.carbonhub.insignia.service;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;
import com.piedpiper.carbonhub.insignia.repository.CatalogoInsigniaRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
public class CatalogoInsigniasEmpresaBootstrap implements ApplicationRunner {

    public static final long EXCELENCIA_CLIMATICA_EMPRESARIAL = 3L;

    private final CatalogoInsigniaRepository catalogoInsigniaRepository;

    public CatalogoInsigniasEmpresaBootstrap(
            CatalogoInsigniaRepository catalogoInsigniaRepository) {
        this.catalogoInsigniaRepository = catalogoInsigniaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CatalogoInsignia> insignias = List.of(
                excelenciaClimatica("bronce", 1,
                        "Reconocimiento por mantener una certificacion ambiental activa validada.",
                        Set.of(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL)),
                excelenciaClimatica("plata", 2,
                        "Reconocimiento por sostener dos certificaciones ambientales activas y complementarias.",
                        Set.of(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL,
                                TipoCertificacion.CARBONO_NEUTRAL)),
                excelenciaClimatica("oro", 3,
                        "Reconocimiento por demostrar una gestion climatica integral con tres certificaciones activas.",
                        Set.of(TipoCertificacion.EXCELENCIA_CLIMATICA_EMPRESARIAL,
                                TipoCertificacion.CARBONO_NEUTRAL,
                                TipoCertificacion.REDUCCION_EMISIONES))
        );

        insignias.forEach(this::crearSiNoExiste);
    }

    private CatalogoInsignia excelenciaClimatica(String nivel,
                                                 int certificacionesMinimas,
                                                 String descripcion,
                                                 Set<TipoCertificacion> tiposRequeridos) {
        return CatalogoInsignia.builder()
                .idInsignia(EXCELENCIA_CLIMATICA_EMPRESARIAL)
                .nombre("Excelencia climatica empresarial")
                .descripcion(descripcion)
                .nivelInsignia(nivel)
                .cantidadMinimaCertificacionesActivas(certificacionesMinimas)
                .tiposCertificacionesRequeridas(tiposRequeridos)
                .activa(true)
                .build();
    }

    private void crearSiNoExiste(CatalogoInsignia insignia) {
        if (catalogoInsigniaRepository.findByIdInsigniaAndNivelInsignia(
                insignia.getIdInsignia(), insignia.getNivelInsignia()).isEmpty()) {
            catalogoInsigniaRepository.save(insignia);
        }
    }
}
