package com.piedpiper.carbonhub.user.repository;

import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByGoogleSub(String googleSub);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByTokenVerificacionHash(String tokenVerificacionHash);
}
