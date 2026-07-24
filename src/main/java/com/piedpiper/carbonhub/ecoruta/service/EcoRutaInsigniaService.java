package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.config.CatalogoInsigniasEcoRuta;
import com.piedpiper.carbonhub.ecoruta.config.ReglaInsigniaEcoRuta;
import com.piedpiper.carbonhub.ecoruta.mappers.InsigniaUsuarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.InsigniaUsuarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import com.piedpiper.carbonhub.ecoruta.repository.InsigniaUsuarioRepository;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EcoRutaInsigniaService {

    private static final Logger log = LoggerFactory.getLogger(EcoRutaInsigniaService.class);

    private final UsuarioRepository usuarioRepository;
    private final InsigniaUsuarioRepository insigniaUsuarioRepository;
    private final CatalogoInsigniasEcoRuta catalogoInsigniasEcoRuta;
    private final InsigniaUsuarioMapper insigniaUsuarioMapper;

    public EcoRutaInsigniaService(UsuarioRepository usuarioRepository,
                                  InsigniaUsuarioRepository insigniaUsuarioRepository,
                                  CatalogoInsigniasEcoRuta catalogoInsigniasEcoRuta,
                                  InsigniaUsuarioMapper insigniaUsuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.insigniaUsuarioRepository = insigniaUsuarioRepository;
        this.catalogoInsigniasEcoRuta = catalogoInsigniasEcoRuta;
        this.insigniaUsuarioMapper = insigniaUsuarioMapper;
    }

    public void evaluarYOtorgar(EventoCertificacionRequestDTO evento) {
        try {
            evaluarYOtorgarInterno(evento);
        } catch (Exception e) {
            UUID usuarioId = evento == null ? null : evento.getUsuarioId();
            log.error("Error al evaluar insignias EcoRuta para el usuario {}", usuarioId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<InsigniaUsuarioResponseDTO> listarObtenidas(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(this::esUsuarioIndividualActivo)
                .map(usuario -> insigniaUsuarioRepository
                        .findByUsuarioIdOrderByFechaObtencionDesc(usuario.getId()).stream()
                        .map(this::aResponse)
                        .toList())
                .orElseGet(List::of);
    }

    private void evaluarYOtorgarInterno(EventoCertificacionRequestDTO evento) {
        if (evento == null || evento.getUsuarioId() == null) {
            log.warn("Evaluacion de insignias EcoRuta omitida por evento incompleto");
            return;
        }

        ReglaInsigniaEcoRuta regla = catalogoInsigniasEcoRuta
                .buscarPorEvento(evento.getEventoGenerado())
                .orElse(null);
        if (regla == null) {
            log.warn("Evento de desbloqueo EcoRuta desconocido: {}",
                    evento.getEventoGenerado());
            return;
        }

        Usuario usuario = usuarioRepository.findById(evento.getUsuarioId()).orElse(null);
        if (!esUsuarioIndividualActivo(usuario)) {
            log.info("Otorgamiento de insignia EcoRuta omitido para usuario no elegible {}",
                    evento.getUsuarioId());
            return;
        }

        if (insigniaUsuarioRepository.existsByUsuarioIdAndIdInsignia(
                usuario.getId(), regla.idInsignia())) {
            return;
        }

        insigniaUsuarioRepository.saveAndFlush(InsigniaUsuario.builder()
                .usuario(usuario)
                .idInsignia(regla.idInsignia())
                .eventoDesbloqueo(regla.eventoDesbloqueo())
                .fechaObtencion(fechaObtencion(evento.getFechaEvento()))
                .build());
    }

    private boolean esUsuarioIndividualActivo(Usuario usuario) {
        return usuario != null
                && usuario.getRol() == Rol.USUARIO_INDIVIDUAL
                && usuario.getEstado() == EstadoUsuario.ACTIVO;
    }

    private Instant fechaObtencion(Instant fechaEvento) {
        return fechaEvento == null ? Instant.now() : fechaEvento;
    }

    private InsigniaUsuarioResponseDTO aResponse(InsigniaUsuario insigniaUsuario) {
        InsigniaUsuarioResponseDTO response = insigniaUsuarioMapper.toDto(insigniaUsuario);
        catalogoInsigniasEcoRuta.buscarPorId(insigniaUsuario.getIdInsignia()).ifPresent(regla -> {
            response.setNombre(regla.nombre());
            response.setDescripcion(regla.descripcion());
        });
        return response;
    }
}
