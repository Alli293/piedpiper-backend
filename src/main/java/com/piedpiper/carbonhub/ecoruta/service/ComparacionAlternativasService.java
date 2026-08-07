package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.common.Catalogos;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ComparacionResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioActividadRepository;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ComparacionAlternativasService {

    private static final int ECO_SCORE_DEFAULT = 50;

    private final ItinerarioRepository itinerarioRepository;
    private final ItinerarioActividadRepository itinerarioActividadRepository;
    private final AlternativasIaClienteService alternativasIaClienteService;
    private final ItinerarioMapper mapper;

    public ComparacionAlternativasService(ItinerarioRepository itinerarioRepository,
                                          ItinerarioActividadRepository itinerarioActividadRepository,
                                          AlternativasIaClienteService alternativasIaClienteService,
                                          ItinerarioMapper mapper) {
        this.itinerarioRepository = itinerarioRepository;
        this.itinerarioActividadRepository = itinerarioActividadRepository;
        this.alternativasIaClienteService = alternativasIaClienteService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ComparacionResponseDTO obtenerAlternativas(UUID itinerarioId, UUID actividadId, UUID usuarioId) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No tienes permiso para acceder a este itinerario."));

        ItinerarioActividad actividadOriginal = itinerarioActividadRepository
                .findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No tienes permiso para acceder a este itinerario."));

        int ecoScoreOriginal = actividadOriginal.getPuntuacionAmbientalEstimada() != null
                ? actividadOriginal.getPuntuacionAmbientalEstimada()
                : ECO_SCORE_DEFAULT;

        List<String> nombresExcluidos = recopilarNombresActividades(itinerario);

        List<AlternativaIaDTO> alternativasIa = alternativasIaClienteService
                .buscarAlternativas(actividadOriginal, nombresExcluidos);

        if (alternativasIa == null || alternativasIa.isEmpty()) {
            return new ComparacionResponseDTO(
                    actividadOriginal.getNombre(),
                    ecoScoreOriginal,
                    actividadOriginal.getCategoriaTuristica() != null
                            ? actividadOriginal.getCategoriaTuristica().name() : null,
                    actividadOriginal.getProvincia() != null
                            ? actividadOriginal.getProvincia().name() : null,
                    List.of(),
                    "No existen alternativas disponibles para esta actividad."
            );
        }

        List<AlternativaDTO> alternativas = mapearAlternativas(alternativasIa, ecoScoreOriginal);

        alternativas.sort(Comparator.comparingInt(AlternativaDTO::getEcoScore).reversed());

        if (!alternativas.isEmpty()) {
            alternativas.get(0).setMejorDesempeno(true);
        }

        return new ComparacionResponseDTO(
                actividadOriginal.getNombre(),
                ecoScoreOriginal,
                actividadOriginal.getCategoriaTuristica() != null
                        ? actividadOriginal.getCategoriaTuristica().name() : null,
                actividadOriginal.getProvincia() != null
                        ? actividadOriginal.getProvincia().name() : null,
                alternativas,
                null
        );
    }

    private List<String> recopilarNombresActividades(Itinerario itinerario) {
        List<String> nombres = new ArrayList<>();
        for (ItinerarioDia dia : itinerario.getDias()) {
            for (ItinerarioActividad actividad : dia.getActividades()) {
                nombres.add(actividad.getNombre());
            }
        }
        return nombres;
    }

    private List<AlternativaDTO> mapearAlternativas(List<AlternativaIaDTO> alternativasIa, int ecoScoreOriginal) {
        List<AlternativaDTO> resultado = new ArrayList<>();
        for (AlternativaIaDTO iaDto : alternativasIa) {
            int ecoScore = iaDto.getPuntuacionAmbientalEstimada() != null
                    ? iaDto.getPuntuacionAmbientalEstimada()
                    : ECO_SCORE_DEFAULT;
            int diferenciaAmbiental = ecoScore - ecoScoreOriginal;

            AlternativaDTO dto = new AlternativaDTO();
            dto.setNombre(iaDto.getNombre());
            dto.setDescripcion(iaDto.getDescripcion());
            dto.setEcoScore(ecoScore);
            dto.setCostoAproximado(iaDto.getCostoAproximado());
            dto.setMoneda(iaDto.getMoneda());
            dto.setEstablecimientoRecomendado(iaDto.getEstablecimientoRecomendado());
            dto.setDiferenciaAmbiental(diferenciaAmbiental);
            dto.setMejorDesempeno(false);

            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional
    public ItinerarioResponseDTO sustituirActividad(UUID itinerarioId, UUID actividadId,
                                                     SustitucionRequestDTO request, UUID usuarioId) {
        Itinerario itinerario = itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No tienes permiso para acceder a este itinerario."));

        ItinerarioActividad actividad = itinerarioActividadRepository
                .findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No tienes permiso para acceder a este itinerario."));

        validarEquivalencia(actividad, request);

        actividad.setNombre(request.getNombre());
        actividad.setDescripcion(request.getDescripcion());
        actividad.setCostoAproximado(request.getCostoAproximado());
        actividad.setEstablecimientoRecomendado(request.getEstablecimientoRecomendado());
        actividad.setPuntuacionAmbientalEstimada(request.getEcoScore());

        if (request.getMoneda() != null) {
            Moneda moneda = Catalogos.desde(Moneda.class, request.getMoneda())
                    .orElseThrow(() -> ApiException.datosInvalidos(
                            "La moneda especificada no es válida."));
            actividad.setMoneda(moneda);
        }

        itinerarioActividadRepository.save(actividad);

        return mapper.toDto(itinerario);
    }

    private void validarEquivalencia(ItinerarioActividad actividad, SustitucionRequestDTO request) {
        boolean categoriaCoincide = actividad.getCategoriaTuristica() != null
                && actividad.getCategoriaTuristica().name().equalsIgnoreCase(request.getCategoriaTuristica());

        boolean provinciaCoincide = actividad.getProvincia() != null
                && actividad.getProvincia().name().equalsIgnoreCase(request.getProvincia());

        if (!categoriaCoincide || !provinciaCoincide) {
            throw ApiException.datosInvalidos(
                    "La alternativa seleccionada no es equivalente a la actividad original.");
        }
    }
}
