package io.altacod.publisher.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileMediaRepository extends JpaRepository<UserProfileMediaEntity, Long> {

    @Query("""
            select (count(1) > 0) from UserProfileMediaEntity x
            where x.mediaAsset.id = :mediaId
            and exists (select 1 from MembershipEntity m
                        where m.user = x.user and m.workspace.id = :workspaceId)
            """)
    boolean isGalleryMediaInWorkspace(@Param("mediaId") long mediaId, @Param("workspaceId") long workspaceId);

    void deleteByMediaAsset_Id(long mediaAssetId);

    @Query("""
            select upm from UserProfileMediaEntity upm
            join fetch upm.mediaAsset
            where upm.user.id = :userId
            order by upm.sortOrder asc, upm.createdAt desc
            """)
    List<UserProfileMediaEntity> findByUserIdWithMediaOrderBySortOrder(@Param("userId") long userId);

    @Query("select coalesce(max(upm.sortOrder), -1) from UserProfileMediaEntity upm where upm.user.id = :userId")
    int findMaxSortOrderByUserId(@Param("userId") long userId);

    Optional<UserProfileMediaEntity> findByUser_IdAndMediaAsset_Id(long userId, long mediaAssetId);

    long countByUser_Id(long userId);
}
