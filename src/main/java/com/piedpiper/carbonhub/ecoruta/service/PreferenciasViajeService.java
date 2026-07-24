package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.mappers.PreferenciasViajeMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.ecoruta.repository.PreferenciasViajeRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PreferenciasViajeService {

    private static final Logger log = LoggerFactory.getLogger(PreferenciasViajeService.class);

    private final PreferenciasViajeRepository preferenciasViajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PreferenciasViajeMapper mapper;

    public PreferenciasViajeService(PreferenciasViajeRepository preferenciasViajeRepository,
                                    UsuarioRepository usuarioRepository,
                                    PreferenciasViajeMapper mapper) {
        this.preferenciasViajeRepository = preferenciasViajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<PreferenciasViajeResponseDTO> obtener(UUID usuarioId) {
        return preferenciasViajeRepository.findByUsuario_Id(usuarioId)
                .map(this::construirRespuesta);
    }

    @Transactional
    public PreferenciasViajeResponseDTO guardar(UUID usuarioId, PreferenciasViajeRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No se pudo identificar al usuario autenticado."));

        Optional<TipoViaje> tipoViaje = Catalogos.desde(TipoViaje.class, request.getTipoViaje());
        Optional<Provincia> provincia = request.getProvinciaPreferida() != null
                ? Catalogos.desde(Provincia.class, request.getProvinciaPreferida())
                : Optional.empty();
        LinkedHashSet<InteresTuristico> intereses = new LinkedHashSet<>();

        List<String> errores = new ArrayList<>();
        if (tipoViaje.isEmpty()) {
            errores.add("Selecciona el tipo de viaje.");
        }
        if (request.getProvinciaPreferida() != null && provincia.isEmpty()) {
            errores.add("Selecciona una provincia o región válida.");
        }
        for (String valor : request.getIntereses()) {
            Optional<InteresTuristico> interes = Catalogos.desde(InteresTuristico.class, valor);
            if (interes.isEmpty()) {
                errores.add("Selecciona actividades de interés válidas.");
                break;
            }
            intereses.add(interes.get());
        }
        if (!errores.isEmpty()) {
            throw ApiException.valorNoSoportado(String.join(" ", errores));
        }

        PreferenciasViaje entidad = preferenciasViajeRepository.findByUsuario_Id(usuarioId)
                .orElseGet(() -> PreferenciasViaje.builder().usuario(usuario).build());
        boolean recienCreada = entidad.getId() == null;

        entidad.setCantidadDias(request.getCantidadDias());
        entidad.setFechaInicio(request.getFechaInicio());
        entidad.setTipoViaje(tipoViaje.get());
        entidad.setPresupuesto(request.getPresupuesto());
        entidad.setIntereses(new ArrayList<>(intereses));
        entidad.setProvinciaPreferida(provincia.orElse(null));
        entidad.setUbicacionActual(request.getUbicacionActual());
        entidad.setBuscarCercaDeMi(request.isBuscarCercaDeMi());
        entidad.setLimitacionesMovilidad(request.getLimitacionesMovilidad());
        entidad.setRequiereHospedaje(request.isRequiereHospedaje());

        try {
            preferenciasViajeRepository.saveAndFlush(entidad);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.preferenciasViajeConflicto();
        } catch (DataAccessException e) {
            log.error("Error inesperado al guardar las preferencias de viaje del usuario {}", usuarioId, e);
            throw ApiException.errorInterno(
                    "Ocurrió un error al guardar tu información. Por favor intenta nuevamente.");
        }

        PreferenciasViajeResponseDTO response = construirRespuesta(entidad);
        response.setRecienCreada(recienCreada);
        return response;
    }

    private PreferenciasViajeResponseDTO construirRespuesta(PreferenciasViaje entidad) {
        PreferenciasViajeResponseDTO response = mapper.toDto(entidad);
        response.setConversacionCompleta(true);
        return response;
    }
}
