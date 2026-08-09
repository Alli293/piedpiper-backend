package com.piedpiper.carbonhub.auditor.mappers;

import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.DistribucionSectorDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PerfilPublicoAuditorMapper {

    default PerfilPublicoAuditorResponseDTO aPerfilPublicoDto(
            PerfilAuditor perfil,
            MetricasAuditor metricas,
            List<CertificacionPublicaDTO> certificaciones,
            List<DistribucionSectorDTO> distribucionSectores,
            List<ResenaVerificadaDTO> resenas) {

        PerfilPublicoAuditorResponseDTO dto = new PerfilPublicoAuditorResponseDTO();

        dto.setAuditorId(perfil.getAuditor().getId());
        dto.setNombre(nombreCompleto(perfil));
        dto.setFotoPerfil(perfil.getFotoPerfil());
        dto.setDescripcionProfesional(perfil.getDescripcionProfesional());
        dto.setEspecialidades(especialidadesAList(perfil.getEspecialidades()));
        dto.setDisponible(perfil.isDisponible());

        if (metricas != null) {
            dto.setCalificacionPromedio(metricas.getCalificacionPromedio());
            dto.setTotalResenas(metricas.getTotalResenas());
            dto.setAuditoriasCompletadas(metricas.getAuditoriasCompletadas());
            dto.setTiempoPromedioRespuestaDias(metricas.getTiempoPromedioRespuestaDias());
        }

        dto.setCertificaciones(certificaciones);
        dto.setDistribucionSectores(distribucionSectores);
        dto.setResenas(resenas);

        return dto;
    }

    @Named("nombreCompleto")
    default String nombreCompleto(PerfilAuditor perfil) {
        return perfil.getAuditor().nombreCompleto();
    }

    @Named("especialidadesAList")
    default List<String> especialidadesAList(Set<EspecialidadAuditor> especialidades) {
        if (especialidades == null || especialidades.isEmpty()) {
            return Collections.emptyList();
        }
        return especialidades.stream()
                .sorted()
                .map(Enum::name)
                .toList();
    }
}
