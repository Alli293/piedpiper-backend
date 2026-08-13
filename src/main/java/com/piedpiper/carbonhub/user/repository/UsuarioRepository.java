package com.piedpiper.carbonhub.user.repository;

import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByGoogleSub(String googleSub);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByGoogleSub(String googleSub);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByTokenVerificacionHash(String tokenVerificacionHash);

    Optional<Usuario> findByTokenResetHash(String tokenResetHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.tokenVerificacionHash = :tokenVerificacionHash")
    Optional<Usuario> findByTokenVerificacionHashForUpdate(
            @Param("tokenVerificacionHash") String tokenVerificacionHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.tokenResetHash = :tokenResetHash")
    Optional<Usuario> findByTokenResetHashForUpdate(@Param("tokenResetHash") String tokenResetHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") UUID id);
}
