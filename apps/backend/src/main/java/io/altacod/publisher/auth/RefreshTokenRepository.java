package io.altacod.publisher.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revokedAt = :when where r.user.id = :userId and r.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("when") java.time.Instant when);
}
