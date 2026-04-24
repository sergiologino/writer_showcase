package io.altacod.publisher.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select (count(u) > 0) from UserEntity u
            where u.avatarMedia is not null and u.avatarMedia.id = :mediaId
            and exists (select 1 from MembershipEntity m where m.user = u and m.workspace.id = :workspaceId)
            """)
    boolean isAvatarMediaForMemberOfWorkspace(@Param("mediaId") long mediaId, @Param("workspaceId") long workspaceId);

    @Modifying
    @Query("update UserEntity u set u.avatarMedia = null where u.avatarMedia.id = :mediaId")
    int clearAvatarWhereMediaId(@Param("mediaId") long mediaId);
}
