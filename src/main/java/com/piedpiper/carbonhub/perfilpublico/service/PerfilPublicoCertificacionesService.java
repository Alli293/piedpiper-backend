package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.service.ConsultaCertificacionService;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Perfil publico de una empresa: expone solo lo que un visitante sin sesion
 * puede ver. Hoy unicamente la lista de certificaciones activas; otros datos
 * del perfil (nombre, logo, etc.) se agregaran a este mismo dominio mas
 * adelante.
 *
 * <p>Habla con {@link ConsultaCertificacionService}, no con el repositorio ni
 * el mapper de certificaciones directamente: ese dominio es dueno de como se
 * consulta y enriquece una {@code Certificacion}, y este servicio solo
 * necesita resolver el slug a una empresa y pedirle la lista.
 */
@Service
public class PerfilPublicoCertificacionesService {

    private final SlugResolverService slugResolver;
    private final ConsultaCertificacionService consultaCertificacionService;

    public PerfilPublicoCertificacionesService(SlugResolverService slugResolver,
                                               ConsultaCertificacionService consultaCertificacionService) {
        this.slugResolver = slugResolver;
        this.consultaCertificacionService = consultaCertificacionService;
    }

    @Transactional(readOnly = true)
    public List<CertificacionPublicaResponseDTO> listarPorSlug(String slug) {
        Empresa empresa = slugResolver.resolver(slug);
        return consultaCertificacionService.listarActivasPublicasPorEmpresa(empresa.getId());
    }
}
