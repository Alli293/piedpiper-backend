package com.piedpiper.carbonhub.invitacion.repository;

import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.models.enums.EstadoInvitacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitacionRepository extends JpaRepository<Invitacion, UUID> {

    Optional<Invitacion> findByTokenHash(String tokenHash);

    Optional<Invitacion> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Invitacion> findAllByEmpresaIdOrderByFechaEmisionDesc(UUID empresaId);

    boolean existsByEmpresaIdAndEmailIgnoreCaseAndEstadoAndFechaExpiracionAfter(
            UUID empresaId, String email, EstadoInvitacion estado, Instant fecha);
}
