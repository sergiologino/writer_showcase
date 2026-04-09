package io.altacod.publisher.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMediaEntity, Long> {

    @Query("""
            select pm from PostMediaEntity pm
            join fetch pm.mediaAsset
            where pm.post.id = :postId
            order by pm.sortOrder asc
            """)
    List<PostMediaEntity> findByPostIdOrderBySortOrderAsc(@Param("postId") Long postId);

    @Modifying
    @Query("delete from PostMediaEntity pm where pm.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Query("""
            select count(pm) from PostMediaEntity pm
            join pm.post p
            join p.workspace w
            where pm.mediaAsset.id = :mediaId
            and w.slug = :workspaceSlug
            and p.visibility = io.altacod.publisher.post.PostVisibility.PUBLIC
            and p.status = io.altacod.publisher.post.PostStatus.PUBLISHED
            """)
    long countPublicPublishedByMediaAndWorkspaceSlug(
            @Param("mediaId") Long mediaId,
            @Param("workspaceSlug") String workspaceSlug
    );
}
